package com.avms.purchases;

import com.avms.accounts.AccountsUser;
import com.avms.accounts.AccountsUserRepository;
import com.avms.audit.AuditService;
import com.avms.common.BusinessRuleException;
import com.avms.common.Money;
import com.avms.common.ResourceNotFoundException;
import com.avms.common.SequenceService;
import com.avms.common.VentureContextHolder;
import com.avms.inventory.InventoryDtos;
import com.avms.inventory.InventoryService;
import com.avms.inventory.Warehouse;
import com.avms.inventory.WarehouseRepository;
import com.avms.notifications.NotificationService;
import com.avms.products.Product;
import com.avms.products.ProductRepository;
import com.avms.suppliers.Supplier;
import com.avms.suppliers.SupplierRepository;
import com.avms.ventures.Venture;
import com.avms.ventures.VentureRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django purchases views/services: PINV-#### bills, stock postings, audited returns. */
@Service
public class PurchaseService {

  private static final Set<String> BILL_STATUSES =
      Set.of("PENDING", "PARTIAL", "COMPLETED", "RETURNED", "CANCELLED");

  private final PurchaseRepository purchases;
  private final PurchaseItemRepository items;
  private final PurchaseReturnRepository returns;
  private final PurchaseReturnItemRepository returnItems;
  private final SupplierRepository suppliers;
  private final ProductRepository products;
  private final WarehouseRepository warehouses;
  private final VentureRepository ventures;
  private final AccountsUserRepository users;
  private final SequenceService sequences;
  private final AuditService audit;
  private final NotificationService notifications;
  private final InventoryService inventory;

  public PurchaseService(PurchaseRepository purchases, PurchaseItemRepository items,
      PurchaseReturnRepository returns, PurchaseReturnItemRepository returnItems,
      SupplierRepository suppliers, ProductRepository products, WarehouseRepository warehouses,
      VentureRepository ventures, AccountsUserRepository users, SequenceService sequences,
      AuditService audit, NotificationService notifications, InventoryService inventory) {
    this.purchases = purchases;
    this.items = items;
    this.returns = returns;
    this.returnItems = returnItems;
    this.suppliers = suppliers;
    this.products = products;
    this.warehouses = warehouses;
    this.ventures = ventures;
    this.users = users;
    this.sequences = sequences;
    this.audit = audit;
    this.notifications = notifications;
    this.inventory = inventory;
  }

  @Transactional(readOnly = true)
  public Page<PurchaseDtos.PurchaseResponse> list(String search, String status, Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
    return purchases.search(ventureId, pattern, st, pageable)
        .map(p -> toResponse(p, items.findByPurchaseId(p.getId())));
  }

  @Transactional
  public PurchaseDtos.PurchaseResponse create(PurchaseDtos.PurchaseRequest req) {
    req.validate(true);
    Venture venture = resolveVenture(req.venture());
    Supplier supplier = suppliers.findById(req.supplier())
        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found."));
    if (!supplier.getVenture().getId().equals(venture.getId())) {
      throw new IllegalArgumentException("Supplier belongs to a different venture.");
    }
    Warehouse warehouse = null;
    if (req.warehouse() != null) {
      warehouse = warehouses.findById(req.warehouse())
          .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));
      if (!warehouse.getVenture().getId().equals(venture.getId())) {
        throw new IllegalArgumentException("Warehouse belongs to a different venture.");
      }
    }

    Purchase purchase = new Purchase();
    purchase.setVenture(venture);
    purchase.setSupplier(supplier);
    purchase.setWarehouse(warehouse);
    purchase.setInvoiceNumber(sequences.generateCode("purchases", venture.getId(), "PINV"));
    purchase.setPurchaseDate(req.purchaseDate());
    purchase.setDiscount(Money.amount(req.discount()));
    purchase.setTax(Money.amount(req.tax()));
    purchase.setNotes(req.notes());
    purchase.setStatus(req.status() == null ? "COMPLETED" : validatedStatus(req.status()));
    purchase.setCreatedBy(currentUser());
    purchases.save(purchase);

    BigDecimal subtotal = BigDecimal.ZERO;
    List<PurchaseItem> lines = new ArrayList<>();
    for (PurchaseDtos.PurchaseItemRequest line : req.items()) {
      Product product = productInVenture(line.product(), venture.getId());
      BigDecimal qty = requirePositive(line.quantity(), "quantity");
      BigDecimal price = line.unitPrice() == null
          ? product.getPurchasePrice() : line.unitPrice();
      PurchaseItem item = new PurchaseItem();
      item.setPurchase(purchase);
      item.setProduct(product);
      item.setQuantity(qty);
      item.setUnitPrice(Money.amount(price));
      item.setDiscount(Money.amount(line.discount()));
      item.setTax(Money.amount(line.tax()));
      item.setTotal(Money.lineTotal(qty, price, line.discount(), line.tax()));
      lines.add(item);
      subtotal = subtotal.add(qty.multiply(Money.amount(price)));
    }
    items.saveAll(lines);

    purchase.setSubtotal(Money.amount(subtotal));
    purchase.setTotalAmount(Money.amount(subtotal.subtract(purchase.getDiscount()).add(purchase.getTax())));
    purchase.setPaidAmount(Money.amount(BigDecimal.ZERO));
    purchase.setReturnedAmount(Money.amount(BigDecimal.ZERO));
    purchase.setDueAmount(Money.due(purchase.getTotalAmount(), BigDecimal.ZERO, BigDecimal.ZERO));
    purchases.save(purchase);

    if (warehouse != null) {
      for (PurchaseItem item : lines) {
        inventory.record(new InventoryDtos.MovementRequest(null, warehouse.getId(), null,
            item.getProduct().getId(), "PURCHASE", item.getQuantity(), purchase.getPurchaseDate(),
            "Purchase " + purchase.getInvoiceNumber(), "PURCHASE", purchase.getId()));
      }
    }

    audit.log("CREATE", "purchases", "Purchase", String.valueOf(purchase.getId()),
        "Purchase " + purchase.getInvoiceNumber() + " created.");
    notifications.notifyByPermission("PURCHASE_CREATED",
        "Purchase " + purchase.getInvoiceNumber() + " created from " + supplier.getName() + ".",
        "purchases.manage", "/purchases/" + purchase.getId());
    return toResponse(purchase, lines);
  }

  @Transactional(readOnly = true)
  public PurchaseDtos.PurchaseResponse get(Long id) {
    Purchase purchase = findScoped(id);
    return toResponse(purchase, items.findByPurchaseId(id));
  }

  @Transactional(readOnly = true)
  public List<PurchaseDtos.PurchaseItemResponse> lineItems(Long id) {
    Purchase purchase = findScoped(id);
    return items.findByPurchaseId(purchase.getId()).stream().map(this::toItemResponse).toList();
  }

  @Transactional
  public PurchaseDtos.PurchaseResponse update(Long id, PurchaseDtos.PurchaseRequest req) {
    Purchase purchase = findScoped(id);
    if (req.purchaseDate() != null) {
      purchase.setPurchaseDate(req.purchaseDate());
    }
    if (req.discount() != null) {
      purchase.setDiscount(Money.amount(req.discount()));
    }
    if (req.tax() != null) {
      purchase.setTax(Money.amount(req.tax()));
    }
    if (req.notes() != null) {
      purchase.setNotes(req.notes());
    }
    if (req.status() != null) {
      purchase.setStatus(validatedStatus(req.status()));
    }
    purchase.setTotalAmount(
        Money.amount(purchase.getSubtotal().subtract(purchase.getDiscount()).add(purchase.getTax())));
    purchase.setDueAmount(
        Money.due(purchase.getTotalAmount(), purchase.getPaidAmount(), purchase.getReturnedAmount()));
    purchases.save(purchase);
    audit.log("UPDATE", "purchases", "Purchase", String.valueOf(id),
        "Purchase " + purchase.getInvoiceNumber() + " updated.");
    return toResponse(purchase, items.findByPurchaseId(id));
  }

  @Transactional
  public void delete(Long id) {
    Purchase purchase = findScoped(id);
    if (purchase.getWarehouse() != null) {
      throw new BusinessRuleException(
          "Purchase has posted stock movements; reverse it with a purchase return instead.");
    }
    items.deleteAll(items.findByPurchaseId(id));
    purchases.delete(purchase);
    audit.log("DELETE", "purchases", "Purchase", String.valueOf(id),
        "Purchase " + purchase.getInvoiceNumber() + " deleted.");
  }

  @Transactional(readOnly = true)
  public Page<PurchaseDtos.PurchaseReturnResponse> listReturns(String search, Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    return returns.search(ventureId, pattern, pageable)
        .map(r -> toReturnResponse(r, returnItems.findByPurchaseReturnId(r.getId())));
  }

  @Transactional(readOnly = true)
  public PurchaseDtos.PurchaseReturnResponse getReturn(Long id) {
    PurchaseReturn ret = findReturnScoped(id);
    return toReturnResponse(ret, returnItems.findByPurchaseReturnId(id));
  }

  @Transactional
  public PurchaseDtos.PurchaseReturnResponse createReturn(PurchaseDtos.PurchaseReturnRequest req) {
    req.validate();
    Purchase purchase = findScoped(req.purchase());
    if (req.venture() != null && !req.venture().equals(purchase.getVenture().getId())) {
      throw new IllegalArgumentException("Purchase belongs to a different venture.");
    }
    PurchaseReturn ret = new PurchaseReturn();
    ret.setVenture(purchase.getVenture());
    ret.setPurchase(purchase);
    ret.setReturnNumber(sequences.generateCode("purchase_returns", purchase.getVenture().getId(), "RET"));
    ret.setReturnDate(req.returnDate());
    ret.setStatus("COMPLETED");
    ret.setNotes(req.notes());
    ret.setCreatedBy(currentUser());
    returns.save(ret);

    BigDecimal total = BigDecimal.ZERO;
    List<PurchaseReturnItem> lines = new ArrayList<>();
    for (PurchaseDtos.PurchaseReturnItemRequest line : req.items()) {
      Product product = productInVenture(line.product(), purchase.getVenture().getId());
      BigDecimal bought = items.purchasedQuantity(purchase.getId(), product.getId());
      if (bought.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException(
            "Product " + product.getSku() + " was not part of purchase " + purchase.getInvoiceNumber() + ".");
      }
      BigDecimal qty = requirePositive(line.quantity(), "quantity");
      BigDecimal already = returnItems.returnedQuantity(purchase.getId(), product.getId());
      if (already.add(qty).compareTo(bought) > 0) {
        throw new IllegalArgumentException("Return quantity " + qty + " for " + product.getSku()
            + " exceeds purchased quantity " + bought + ".");
      }
      BigDecimal price = line.unitPrice() == null ? BigDecimal.ZERO : line.unitPrice();
      PurchaseReturnItem item = new PurchaseReturnItem();
      item.setPurchaseReturn(ret);
      item.setProduct(product);
      item.setQuantity(qty);
      item.setUnitPrice(Money.amount(price));
      item.setTotal(Money.amount(qty.multiply(Money.amount(price))));
      lines.add(item);
      total = total.add(item.getTotal());
    }
    returnItems.saveAll(lines);
    ret.setTotalAmount(Money.amount(total));
    returns.save(ret);

    purchase.setReturnedAmount(Money.amount(purchase.getReturnedAmount().add(ret.getTotalAmount())));
    purchase.setDueAmount(
        Money.due(purchase.getTotalAmount(), purchase.getPaidAmount(), purchase.getReturnedAmount()));
    if (purchase.getReturnedAmount().compareTo(purchase.getTotalAmount()) >= 0) {
      purchase.setStatus("RETURNED");
    } else {
      purchase.setStatus("PARTIAL");
    }
    purchases.save(purchase);

    if (purchase.getWarehouse() != null) {
      for (PurchaseReturnItem item : lines) {
        inventory.record(new InventoryDtos.MovementRequest(null, purchase.getWarehouse().getId(), null,
            item.getProduct().getId(), "PURCHASE_RETURN", item.getQuantity(), ret.getReturnDate(),
            "Purchase return " + ret.getReturnNumber(), "PURCHASE_RETURN", ret.getId()));
      }
    }

    audit.log("CREATE", "purchase_returns", "PurchaseReturn", String.valueOf(ret.getId()),
        "Purchase return " + ret.getReturnNumber() + " created.");
    return toReturnResponse(ret, lines);
  }

  private Purchase findScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    Purchase purchase = ventureId == null
        ? purchases.findById(id).orElse(null)
        : purchases.findByIdAndVentureId(id, ventureId).orElse(null);
    if (purchase == null) {
      throw new ResourceNotFoundException("Purchase not found.");
    }
    return purchase;
  }

  private PurchaseReturn findReturnScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    PurchaseReturn ret = ventureId == null
        ? returns.findById(id).orElse(null)
        : returns.findByIdAndVentureId(id, ventureId).orElse(null);
    if (ret == null) {
      throw new ResourceNotFoundException("Purchase return not found.");
    }
    return ret;
  }

  private Venture resolveVenture(Long requested) {
    Long ventureId = requested != null ? requested : VentureContextHolder.get();
    if (ventureId == null) {
      throw new IllegalArgumentException("venture is required.");
    }
    return ventures.findById(ventureId)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found."));
  }

  private Product productInVenture(Long productId, Long ventureId) {
    Product product = products.findById(productId)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    if (!product.getVenture().getId().equals(ventureId)) {
      throw new IllegalArgumentException("Product " + product.getSku() + " belongs to a different venture.");
    }
    return product;
  }

  private BigDecimal requirePositive(BigDecimal value, String field) {
    if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(field + " must be greater than zero.");
    }
    return value;
  }

  private String validatedStatus(String status) {
    String st = status.trim().toUpperCase();
    if (!BILL_STATUSES.contains(st)) {
      throw new IllegalArgumentException("Unknown status: " + status);
    }
    return st;
  }

  private AccountsUser currentUser() {
    String email = com.avms.security.SecurityEmails.currentUserEmail();
    if (email == null) {
      return null;
    }
    return users.findByEmailIgnoreCase(email).orElse(null);
  }

  PurchaseDtos.PurchaseItemResponse toItemResponse(PurchaseItem item) {
    return new PurchaseDtos.PurchaseItemResponse(item.getId(), item.getProduct().getId(),
        item.getProduct().getSku(), item.getProduct().getProductName(),
        item.getProduct().getUnit().getUnitCode(), item.getQuantity(), item.getUnitPrice(),
        item.getDiscount(), item.getTax(), item.getTotal());
  }

  PurchaseDtos.PurchaseResponse toResponse(Purchase purchase, List<PurchaseItem> lines) {
    return new PurchaseDtos.PurchaseResponse(purchase.getId(), purchase.getVenture().getId(),
        purchase.getVenture().getVentureCode(), purchase.getSupplier().getId(),
        purchase.getSupplier().getName(), purchase.getSupplier().getSupplierCode(),
        purchase.getWarehouse() == null ? null : purchase.getWarehouse().getId(),
        purchase.getWarehouse() == null ? null : purchase.getWarehouse().getWarehouseCode(),
        purchase.getInvoiceNumber(), purchase.getPurchaseDate(), purchase.getStatus(),
        purchase.getSubtotal(), purchase.getDiscount(), purchase.getTax(), purchase.getTotalAmount(),
        purchase.getPaidAmount(), purchase.getReturnedAmount(), purchase.getDueAmount(),
        purchase.getNotes(), lines.stream().map(this::toItemResponse).toList(),
        purchase.getCreatedAt(), purchase.getUpdatedAt());
  }

  PurchaseDtos.PurchaseReturnResponse toReturnResponse(PurchaseReturn ret, List<PurchaseReturnItem> lines) {
    return new PurchaseDtos.PurchaseReturnResponse(ret.getId(), ret.getVenture().getId(),
        ret.getPurchase().getId(), ret.getPurchase().getInvoiceNumber(), ret.getReturnNumber(),
        ret.getReturnDate(), ret.getStatus(), ret.getTotalAmount(), ret.getNotes(),
        lines.stream().map(i -> new PurchaseDtos.PurchaseReturnItemResponse(i.getId(),
            i.getProduct().getId(), i.getProduct().getSku(), i.getProduct().getProductName(),
            i.getQuantity(), i.getUnitPrice(), i.getTotal())).toList(),
        ret.getCreatedAt());
  }
}
