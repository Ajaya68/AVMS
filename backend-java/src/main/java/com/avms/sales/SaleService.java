package com.avms.sales;

import com.avms.accounts.AccountsUser;
import com.avms.accounts.AccountsUserRepository;
import com.avms.audit.AuditService;
import com.avms.common.BusinessRuleException;
import com.avms.common.Money;
import com.avms.common.ResourceNotFoundException;
import com.avms.common.SequenceService;
import com.avms.common.VentureContextHolder;
import com.avms.customers.Customer;
import com.avms.customers.CustomerRepository;
import com.avms.inventory.InventoryDtos;
import com.avms.inventory.InventoryService;
import com.avms.inventory.Warehouse;
import com.avms.inventory.WarehouseRepository;
import com.avms.notifications.NotificationService;
import com.avms.products.Product;
import com.avms.products.ProductRepository;
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

/** Port of Django sales views/services: SINV-#### bills, stock postings, audited returns. */
@Service
public class SaleService {

  private static final Set<String> BILL_STATUSES =
      Set.of("PENDING", "PARTIAL", "COMPLETED", "RETURNED", "CANCELLED");

  private final SaleRepository sales;
  private final SaleItemRepository items;
  private final SaleReturnRepository returns;
  private final SaleReturnItemRepository returnItems;
  private final CustomerRepository customers;
  private final ProductRepository products;
  private final WarehouseRepository warehouses;
  private final VentureRepository ventures;
  private final AccountsUserRepository users;
  private final SequenceService sequences;
  private final AuditService audit;
  private final NotificationService notifications;
  private final InventoryService inventory;

  public SaleService(SaleRepository sales, SaleItemRepository items, SaleReturnRepository returns,
      SaleReturnItemRepository returnItems, CustomerRepository customers, ProductRepository products,
      WarehouseRepository warehouses, VentureRepository ventures, AccountsUserRepository users,
      SequenceService sequences, AuditService audit, NotificationService notifications,
      InventoryService inventory) {
    this.sales = sales;
    this.items = items;
    this.returns = returns;
    this.returnItems = returnItems;
    this.customers = customers;
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
  public Page<SaleDtos.SaleResponse> list(String search, String status, Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
    return sales.search(ventureId, pattern, st, pageable).map(s -> toResponse(s, items.findBySaleId(s.getId())));
  }

  @Transactional
  public SaleDtos.SaleResponse create(SaleDtos.SaleRequest req) {
    req.validate(true);
    Venture venture = resolveVenture(req.venture());
    Customer customer = customers.findById(req.customer())
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));
    if (!customer.getVenture().getId().equals(venture.getId())) {
      throw new IllegalArgumentException("Customer belongs to a different venture.");
    }
    Warehouse warehouse = null;
    if (req.warehouse() != null) {
      warehouse = warehouses.findById(req.warehouse())
          .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));
      if (!warehouse.getVenture().getId().equals(venture.getId())) {
        throw new IllegalArgumentException("Warehouse belongs to a different venture.");
      }
    }

    Sale sale = new Sale();
    sale.setVenture(venture);
    sale.setCustomer(customer);
    sale.setWarehouse(warehouse);
    sale.setInvoiceNumber(sequences.generateCode("sales", venture.getId(), "SINV"));
    sale.setSaleDate(req.saleDate());
    sale.setDiscount(Money.amount(req.discount()));
    sale.setTax(Money.amount(req.tax()));
    sale.setNotes(req.notes());
    sale.setStatus(req.status() == null ? "COMPLETED" : validatedStatus(req.status()));
    sale.setCreatedBy(currentUser());
    sales.save(sale);

    BigDecimal subtotal = BigDecimal.ZERO;
    List<SaleItem> lines = new ArrayList<>();
    for (SaleDtos.SaleItemRequest line : req.items()) {
      Product product = productInVenture(line.product(), venture.getId());
      BigDecimal qty = requirePositive(line.quantity(), "quantity");
      BigDecimal price = line.unitPrice() == null
          ? product.getSellingPrice() : line.unitPrice();
      SaleItem item = new SaleItem();
      item.setSale(sale);
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

    sale.setSubtotal(Money.amount(subtotal));
    sale.setTotalAmount(Money.amount(subtotal.subtract(sale.getDiscount()).add(sale.getTax())));
    sale.setPaidAmount(Money.amount(BigDecimal.ZERO));
    sale.setReturnedAmount(Money.amount(BigDecimal.ZERO));
    sale.setDueAmount(Money.due(sale.getTotalAmount(), BigDecimal.ZERO, BigDecimal.ZERO));
    sales.save(sale);

    if (warehouse != null) {
      for (SaleItem item : lines) {
        inventory.record(new InventoryDtos.MovementRequest(null, warehouse.getId(), null,
            item.getProduct().getId(), "SALE", item.getQuantity(), sale.getSaleDate(),
            "Sale " + sale.getInvoiceNumber(), "SALE", sale.getId()));
      }
    }

    audit.log("CREATE", "sales", "Sale", String.valueOf(sale.getId()),
        "Sale " + sale.getInvoiceNumber() + " created.");
    notifications.notifyByPermission("SALE_CREATED",
        "Sale " + sale.getInvoiceNumber() + " created for " + customer.getName() + ".",
        "sales.manage", "/sales/" + sale.getId());
    return toResponse(sale, lines);
  }

  @Transactional(readOnly = true)
  public SaleDtos.SaleResponse get(Long id) {
    Sale sale = findScoped(id);
    return toResponse(sale, items.findBySaleId(id));
  }

  @Transactional(readOnly = true)
  public List<SaleDtos.SaleItemResponse> lineItems(Long id) {
    Sale sale = findScoped(id);
    return items.findBySaleId(sale.getId()).stream().map(this::toItemResponse).toList();
  }

  @Transactional
  public SaleDtos.SaleResponse update(Long id, SaleDtos.SaleRequest req) {
    Sale sale = findScoped(id);
    if (req.saleDate() != null) {
      sale.setSaleDate(req.saleDate());
    }
    if (req.discount() != null) {
      sale.setDiscount(Money.amount(req.discount()));
    }
    if (req.tax() != null) {
      sale.setTax(Money.amount(req.tax()));
    }
    if (req.notes() != null) {
      sale.setNotes(req.notes());
    }
    if (req.status() != null) {
      sale.setStatus(validatedStatus(req.status()));
    }
    sale.setTotalAmount(Money.amount(sale.getSubtotal().subtract(sale.getDiscount()).add(sale.getTax())));
    sale.setDueAmount(Money.due(sale.getTotalAmount(), sale.getPaidAmount(), sale.getReturnedAmount()));
    sales.save(sale);
    audit.log("UPDATE", "sales", "Sale", String.valueOf(id),
        "Sale " + sale.getInvoiceNumber() + " updated.");
    return toResponse(sale, items.findBySaleId(id));
  }

  @Transactional
  public void delete(Long id) {
    Sale sale = findScoped(id);
    if (sale.getWarehouse() != null) {
      throw new BusinessRuleException(
          "Sale has posted stock movements; reverse it with a sales return instead.");
    }
    items.deleteAll(items.findBySaleId(id));
    sales.delete(sale);
    audit.log("DELETE", "sales", "Sale", String.valueOf(id),
        "Sale " + sale.getInvoiceNumber() + " deleted.");
  }

  @Transactional(readOnly = true)
  public Page<SaleDtos.SaleReturnResponse> listReturns(String search, Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    return returns.search(ventureId, pattern, pageable)
        .map(r -> toReturnResponse(r, returnItems.findBySaleReturnId(r.getId())));
  }

  @Transactional(readOnly = true)
  public SaleDtos.SaleReturnResponse getReturn(Long id) {
    SaleReturn ret = findReturnScoped(id);
    return toReturnResponse(ret, returnItems.findBySaleReturnId(id));
  }

  @Transactional
  public SaleDtos.SaleReturnResponse createReturn(SaleDtos.SaleReturnRequest req) {
    req.validate();
    Sale sale = findScoped(req.sale());
    if (req.venture() != null && !req.venture().equals(sale.getVenture().getId())) {
      throw new IllegalArgumentException("Sale belongs to a different venture.");
    }
    SaleReturn ret = new SaleReturn();
    ret.setVenture(sale.getVenture());
    ret.setSale(sale);
    ret.setReturnNumber(sequences.generateCode("sales_returns", sale.getVenture().getId(), "SRET"));
    ret.setReturnDate(req.returnDate());
    ret.setStatus("COMPLETED");
    ret.setNotes(req.notes());
    ret.setCreatedBy(currentUser());
    returns.save(ret);

    BigDecimal total = BigDecimal.ZERO;
    List<SaleReturnItem> lines = new ArrayList<>();
    for (SaleDtos.SaleReturnItemRequest line : req.items()) {
      Product product = productInVenture(line.product(), sale.getVenture().getId());
      BigDecimal sold = items.soldQuantity(sale.getId(), product.getId());
      if (sold.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException(
            "Product " + product.getSku() + " was not part of sale " + sale.getInvoiceNumber() + ".");
      }
      BigDecimal qty = requirePositive(line.quantity(), "quantity");
      BigDecimal already = returnItems.returnedQuantity(sale.getId(), product.getId());
      if (already.add(qty).compareTo(sold) > 0) {
        throw new IllegalArgumentException("Return quantity " + qty + " for " + product.getSku()
            + " exceeds sold quantity " + sold + ".");
      }
      BigDecimal price = line.unitPrice() == null ? BigDecimal.ZERO : line.unitPrice();
      SaleReturnItem item = new SaleReturnItem();
      item.setSaleReturn(ret);
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

    sale.setReturnedAmount(Money.amount(sale.getReturnedAmount().add(ret.getTotalAmount())));
    sale.setDueAmount(Money.due(sale.getTotalAmount(), sale.getPaidAmount(), sale.getReturnedAmount()));
    if (sale.getReturnedAmount().compareTo(sale.getTotalAmount()) >= 0) {
      sale.setStatus("RETURNED");
    } else {
      sale.setStatus("PARTIAL");
    }
    sales.save(sale);

    if (sale.getWarehouse() != null) {
      for (SaleReturnItem item : lines) {
        inventory.record(new InventoryDtos.MovementRequest(null, sale.getWarehouse().getId(), null,
            item.getProduct().getId(), "SALES_RETURN", item.getQuantity(), ret.getReturnDate(),
            "Sales return " + ret.getReturnNumber(), "SALE_RETURN", ret.getId()));
      }
    }

    audit.log("CREATE", "sales_returns", "SaleReturn", String.valueOf(ret.getId()),
        "Sales return " + ret.getReturnNumber() + " created.");
    return toReturnResponse(ret, lines);
  }

  private Sale findScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    Sale sale = ventureId == null
        ? sales.findById(id).orElse(null)
        : sales.findByIdAndVentureId(id, ventureId).orElse(null);
    if (sale == null) {
      throw new ResourceNotFoundException("Sale not found.");
    }
    return sale;
  }

  private SaleReturn findReturnScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    SaleReturn ret = ventureId == null
        ? returns.findById(id).orElse(null)
        : returns.findByIdAndVentureId(id, ventureId).orElse(null);
    if (ret == null) {
      throw new ResourceNotFoundException("Sales return not found.");
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

  SaleDtos.SaleItemResponse toItemResponse(SaleItem item) {
    return new SaleDtos.SaleItemResponse(item.getId(), item.getProduct().getId(),
        item.getProduct().getSku(), item.getProduct().getProductName(),
        item.getProduct().getUnit().getUnitCode(), item.getQuantity(), item.getUnitPrice(),
        item.getDiscount(), item.getTax(), item.getTotal());
  }

  SaleDtos.SaleResponse toResponse(Sale sale, List<SaleItem> lines) {
    return new SaleDtos.SaleResponse(sale.getId(), sale.getVenture().getId(),
        sale.getVenture().getVentureCode(), sale.getCustomer().getId(), sale.getCustomer().getName(),
        sale.getCustomer().getCustomerCode(),
        sale.getWarehouse() == null ? null : sale.getWarehouse().getId(),
        sale.getWarehouse() == null ? null : sale.getWarehouse().getWarehouseCode(),
        sale.getInvoiceNumber(), sale.getSaleDate(), sale.getStatus(), sale.getSubtotal(),
        sale.getDiscount(), sale.getTax(), sale.getTotalAmount(), sale.getPaidAmount(),
        sale.getReturnedAmount(), sale.getDueAmount(), sale.getNotes(),
        lines.stream().map(this::toItemResponse).toList(),
        sale.getCreatedAt(), sale.getUpdatedAt());
  }

  SaleDtos.SaleReturnResponse toReturnResponse(SaleReturn ret, List<SaleReturnItem> lines) {
    return new SaleDtos.SaleReturnResponse(ret.getId(), ret.getVenture().getId(), ret.getSale().getId(),
        ret.getSale().getInvoiceNumber(), ret.getReturnNumber(), ret.getReturnDate(), ret.getStatus(),
        ret.getTotalAmount(), ret.getNotes(),
        lines.stream().map(i -> new SaleDtos.SaleReturnItemResponse(i.getId(), i.getProduct().getId(),
            i.getProduct().getSku(), i.getProduct().getProductName(), i.getQuantity(),
            i.getUnitPrice(), i.getTotal())).toList(),
        ret.getCreatedAt());
  }
}
