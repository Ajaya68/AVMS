package com.avms.inventory;

import com.avms.common.BaseEntity;
import com.avms.products.Product;
import com.avms.ventures.Venture;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/** Port of Django inventory.Inventory: on-hand / reserved stock per warehouse+product. */
@Entity
@Table(name = "INVENTORY_STOCK",
    uniqueConstraints = @UniqueConstraint(name = "uniq_stock_warehouse_product",
        columnNames = {"warehouse_id", "product_id"}))
public class Stock extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venture_id", nullable = false)
  private Venture venture;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "quantity", nullable = false, precision = 14, scale = 2)
  private BigDecimal quantity = BigDecimal.ZERO;

  @Column(name = "reserved_quantity", nullable = false, precision = 14, scale = 2)
  private BigDecimal reservedQuantity = BigDecimal.ZERO;

  @Column(name = "reorder_level", nullable = false, precision = 14, scale = 2)
  private BigDecimal reorderLevel = BigDecimal.ZERO;

  public Stock() {}

  public Long getId() { return id; }
  public Venture getVenture() { return venture; }
  public void setVenture(Venture venture) { this.venture = venture; }
  public Warehouse getWarehouse() { return warehouse; }
  public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
  public Product getProduct() { return product; }
  public void setProduct(Product product) { this.product = product; }
  public BigDecimal getQuantity() { return quantity; }
  public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
  public BigDecimal getReservedQuantity() { return reservedQuantity; }
  public void setReservedQuantity(BigDecimal reservedQuantity) { this.reservedQuantity = reservedQuantity; }
  public BigDecimal getReorderLevel() { return reorderLevel; }
  public void setReorderLevel(BigDecimal reorderLevel) { this.reorderLevel = reorderLevel; }

  public BigDecimal available() {
    BigDecimal qty = quantity == null ? BigDecimal.ZERO : quantity;
    BigDecimal reserved = reservedQuantity == null ? BigDecimal.ZERO : reservedQuantity;
    return qty.subtract(reserved);
  }

  public boolean low() {
    BigDecimal reorder = reorderLevel == null ? BigDecimal.ZERO : reorderLevel;
    return available().compareTo(reorder) <= 0;
  }
}
