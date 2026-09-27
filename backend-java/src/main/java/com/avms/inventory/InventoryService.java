package com.avms.inventory;

import com.avms.accounts.AccountsUser;
import com.avms.accounts.AccountsUserRepository;
import com.avms.audit.AuditService;
import com.avms.common.BusinessRuleException;
import com.avms.common.ResourceNotFoundException;
import com.avms.common.VentureContextHolder;
import com.avms.notifications.NotificationService;
import com.avms.products.Product;
import com.avms.products.ProductRepository;
import com.avms.ventures.Venture;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Port of Django inventory services/views: stock rows, movement recording with
 * transactional quantity deltas, transfer pairs, low-stock alerts.
 */
@Service
public class InventoryService {

  private static final Set<String> INCOMING =
      Set.of("PURCHASE", "SALES_RETURN", "ADJUSTMENT_IN", "TRANSFER_IN");
  private static final Set<String> OUTGOING =
      Set.of("SALE", "PURCHASE_RETURN", "ADJUSTMENT_OUT", "TRANSFER_OUT");
  private static final Set<String> ALL_TYPES;

  static {
    java.util.HashSet<String> all = new java.util.HashSet<>(INCOMING);
    all.addAll(OUTGOING);
    ALL_TYPES = Set.copyOf(all);
  }

  private final StockRepository stocks;
  private final StockMovementRepository movements;
  private final WarehouseRepository warehouses;
  private final ProductRepository products;
  private final AccountsUserRepository users;
  private final AuditService audit;
  private final NotificationService notifications;

  public InventoryService(StockRepository stocks, StockMovementRepository movements,
      WarehouseRepository warehouses, ProductRepository products, AccountsUserRepository users,
      AuditService audit, NotificationService notifications) {
    this.stocks = stocks;
    this.movements = movements;
    this.warehouses = warehouses;
    this.products = products;
    this.users = users;
    this.audit = audit;
    this.notifications = notifications;
  }

  @Transactional(readOnly = true)
  public Page<InventoryDtos.StockResponse> listStocks(Long warehouseId, Long productId, boolean lowOnly,
      Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    return stocks.search(ventureId, warehouseId, productId, lowOnly, pageable).map(this::toStockResponse);
  }

  @Transactional(readOnly = true)
  public Page<InventoryDtos.MovementResponse> listMovements(Long warehouseId, Long productId,
      String movementType, Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String type = movementType != null && !movementType.isBlank() ? movementType.trim().toUpperCase() : null;
    return movements.search(ventureId, warehouseId, productId, type, pageable).map(this::toMovementResponse);
  }

  @Transactional(readOnly = true)
  public InventoryDtos.MovementResponse getMovement(Long id) {
    Long ventureId = VentureContextHolder.get();
    StockMovement movement = ventureId == null
        ? movements.findById(id).orElse(null)
        : movements.findByIdAndVentureId(id, ventureId).orElse(null);
    if (movement == null) {
      throw new ResourceNotFoundException("Stock movement not found.");
    }
    return toMovementResponse(movement);
  }

  @Transactional
  public InventoryDtos.MovementResponse record(InventoryDtos.MovementRequest req) {
    req.validate();
    String type = req.movementType().trim().toUpperCase();
    if (!ALL_TYPES.contains(type)) {
      throw new IllegalArgumentException("Unknown movement_type: " + req.movementType());
    }
    Warehouse warehouse = findWarehouse(req.warehouse());
    Product product = findProduct(req.product());
    if (!product.getVenture().getId().equals(warehouse.getVenture().getId())) {
      throw new IllegalArgumentException("Product belongs to a different venture than the warehouse.");
    }
    Warehouse destination = null;
    if ("TRANSFER_OUT".equals(type)) {
      if (req.destinationWarehouse() == null) {
        throw new IllegalArgumentException("destination_warehouse is required for transfers.");
      }
      destination = findWarehouse(req.destinationWarehouse());
      if (destination.getId().equals(warehouse.getId())) {
        throw new IllegalArgumentException("destination_warehouse must differ from the source warehouse.");
      }
      if (!destination.getVenture().getId().equals(warehouse.getVenture().getId())) {
        throw new IllegalArgumentException("destination_warehouse belongs to a different venture.");
      }
    }

    AccountsUser actor = currentUser();
    StockMovement movement = applyDelta(warehouse, product, type, req, actor);

    if (destination != null) {
      StockMovement inbound = applyDelta(destination, product, "TRANSFER_IN", req, actor);
      movement.setPairedMovement(inbound);
      inbound.setPairedMovement(movement);
      movements.save(inbound);
      movements.save(movement);
    }

    audit.log("CREATE", "stock_movements", "StockMovement", String.valueOf(movement.getId()),
        type + " movement of " + req.quantity() + " " + product.getSku() + " recorded.");
    return toMovementResponse(movement);
  }

  private StockMovement applyDelta(Warehouse warehouse, Product product, String type,
      InventoryDtos.MovementRequest req, AccountsUser actor) {
    Stock stock = stocks.findByWarehouseIdAndProductId(warehouse.getId(), product.getId())
        .orElseGet(() -> {
          Stock created = new Stock();
          created.setVenture(warehouse.getVenture());
          created.setWarehouse(warehouse);
          created.setProduct(product);
          created.setQuantity(BigDecimal.ZERO);
          created.setReservedQuantity(BigDecimal.ZERO);
          created.setReorderLevel(product.getReorderLevel() == null
              ? BigDecimal.ZERO : product.getReorderLevel());
          return created;
        });
    // Keep the reorder level in sync with the product master on every move.
    stock.setReorderLevel(product.getReorderLevel() == null ? BigDecimal.ZERO : product.getReorderLevel());

    BigDecimal amount = req.quantity();
    if (OUTGOING.contains(type)) {
      BigDecimal onHand = stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity();
      if (onHand.compareTo(amount) < 0) {
        throw new BusinessRuleException("Insufficient stock: " + product.getSku()
            + " has " + onHand + " on hand in " + warehouse.getWarehouseCode() + ".");
      }
      stock.setQuantity(onHand.subtract(amount));
    } else {
      BigDecimal onHand = stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity();
      stock.setQuantity(onHand.add(amount));
    }
    stocks.save(stock);

    StockMovement movement = new StockMovement();
    movement.setVenture(warehouse.getVenture());
    movement.setWarehouse(warehouse);
    if ("TRANSFER_OUT".equals(type)) {
      movement.setDestinationWarehouse(
          warehouses.findById(req.destinationWarehouse()).orElse(null));
    }
    movement.setProduct(product);
    movement.setMovementType(type);
    movement.setQuantity(amount);
    movement.setMovementDate(req.movementDate());
    movement.setNotes(req.notes());
    movement.setReferenceType(req.referenceType());
    movement.setReferenceId(req.referenceId());
    movement.setCreatedBy(actor);
    movements.save(movement);

    if (stock.low()) {
      notifications.notifyByPermission("LOW_STOCK",
          product.getSku() + " " + product.getProductName() + " is low in "
              + warehouse.getWarehouseCode() + " (available " + stock.available() + ").",
          "inventory.manage", "/inventory");
    }
    return movement;
  }

  private Warehouse findWarehouse(Long id) {
    Long ventureId = VentureContextHolder.get();
    Warehouse warehouse = ventureId == null
        ? warehouses.findById(id).orElse(null)
        : warehouses.findByIdAndVentureId(id, ventureId).orElse(null);
    if (warehouse == null) {
      throw new ResourceNotFoundException("Warehouse not found.");
    }
    return warehouse;
  }

  private Product findProduct(Long id) {
    Long ventureId = VentureContextHolder.get();
    Product product = ventureId == null
        ? products.findById(id).orElse(null)
        : products.findByIdAndVentureId(id, ventureId).orElse(null);
    if (product == null) {
      throw new ResourceNotFoundException("Product not found.");
    }
    return product;
  }

  private AccountsUser currentUser() {
    String email = com.avms.security.SecurityEmails.currentUserEmail();
    if (email == null) {
      return null;
    }
    return users.findByEmailIgnoreCase(email).orElse(null);
  }

  InventoryDtos.StockResponse toStockResponse(Stock stock) {
    return new InventoryDtos.StockResponse(stock.getId(), stock.getVenture().getId(),
        stock.getProduct().getId(), stock.getProduct().getSku(), stock.getProduct().getProductName(),
        stock.getWarehouse().getId(), stock.getWarehouse().getWarehouseCode(),
        stock.getWarehouse().getWarehouseName(), stock.getQuantity(), stock.getReservedQuantity(),
        stock.available(), stock.getReorderLevel(), stock.low());
  }

  InventoryDtos.MovementResponse toMovementResponse(StockMovement movement) {
    Warehouse destination = movement.getDestinationWarehouse();
    return new InventoryDtos.MovementResponse(movement.getId(), movement.getVenture().getId(),
        movement.getWarehouse().getId(), movement.getWarehouse().getWarehouseCode(),
        destination == null ? null : destination.getId(),
        destination == null ? null : destination.getWarehouseCode(),
        movement.getProduct().getId(), movement.getProduct().getSku(),
        movement.getProduct().getProductName(), movement.getMovementType(), movement.getQuantity(),
        movement.getMovementDate(), movement.getNotes(), movement.getReferenceType(),
        movement.getReferenceId(),
        movement.getPairedMovement() == null ? null : movement.getPairedMovement().getId(),
        movement.getCreatedBy() == null ? null : movement.getCreatedBy().getEmail(),
        movement.getCreatedAt());
  }
}
