package com.avms.products;

import com.avms.common.BaseEntity;
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

/** Port of Django products.Product: venture-scoped, auto P-#### SKUs. */
@Entity
@Table(name = "PRODUCTS_PRODUCT",
    uniqueConstraints = @UniqueConstraint(name = "uniq_product_venture_sku",
        columnNames = {"venture_id", "sku"}))
public class Product extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venture_id", nullable = false)
  private Venture venture;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private Category category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unit_id", nullable = false)
  private Unit unit;

  @Column(name = "sku", nullable = false, length = 20)
  private String sku;

  @Column(name = "product_name", nullable = false, length = 150)
  private String productName;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal purchasePrice = BigDecimal.ZERO;

  @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal sellingPrice = BigDecimal.ZERO;

  @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal taxRate = BigDecimal.ZERO;

  @Column(name = "reorder_level", nullable = false, precision = 12, scale = 2)
  private BigDecimal reorderLevel = BigDecimal.ZERO;

  @Column(name = "status", nullable = false, length = 20)
  private String status = "ACTIVE";

  public Product() {}

  public Long getId() { return id; }
  public Venture getVenture() { return venture; }
  public void setVenture(Venture venture) { this.venture = venture; }
  public Category getCategory() { return category; }
  public void setCategory(Category category) { this.category = category; }
  public Unit getUnit() { return unit; }
  public void setUnit(Unit unit) { this.unit = unit; }
  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public String getProductName() { return productName; }
  public void setProductName(String productName) { this.productName = productName; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public BigDecimal getPurchasePrice() { return purchasePrice; }
  public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
  public BigDecimal getSellingPrice() { return sellingPrice; }
  public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
  public BigDecimal getTaxRate() { return taxRate; }
  public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
  public BigDecimal getReorderLevel() { return reorderLevel; }
  public void setReorderLevel(BigDecimal reorderLevel) { this.reorderLevel = reorderLevel; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
