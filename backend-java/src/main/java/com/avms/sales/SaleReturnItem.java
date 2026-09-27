package com.avms.sales;

import com.avms.products.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Port of Django sales.SaleReturnItem: one returned line. */
@Entity
@Table(name = "SALES_SALERETURNITEM")
public class SaleReturnItem {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sale_return_id", nullable = false)
  private SaleReturn saleReturn;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "quantity", nullable = false, precision = 14, scale = 2)
  private BigDecimal quantity;

  @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "total", nullable = false, precision = 14, scale = 2)
  private BigDecimal total = BigDecimal.ZERO;

  public SaleReturnItem() {}

  public Long getId() { return id; }
  public SaleReturn getSaleReturn() { return saleReturn; }
  public void setSaleReturn(SaleReturn saleReturn) { this.saleReturn = saleReturn; }
  public Product getProduct() { return product; }
  public void setProduct(Product product) { this.product = product; }
  public BigDecimal getQuantity() { return quantity; }
  public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
  public BigDecimal getTotal() { return total; }
  public void setTotal(BigDecimal total) { this.total = total; }
}
