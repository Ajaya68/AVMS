package com.avms.sales;

import com.avms.accounts.AccountsUser;
import com.avms.common.BaseEntity;
import com.avms.customers.Customer;
import com.avms.inventory.Warehouse;
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
import java.time.LocalDate;

/** Port of Django sales.Sale: bill header with computed totals and balances. */
@Entity
@Table(name = "SALES_SALE",
    uniqueConstraints = @UniqueConstraint(name = "uniq_sale_venture_invoice",
        columnNames = {"venture_id", "invoice_number"}))
public class Sale extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venture_id", nullable = false)
  private Venture venture;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse;

  @Column(name = "invoice_number", nullable = false, length = 20)
  private String invoiceNumber;

  @Column(name = "sale_date", nullable = false)
  private LocalDate saleDate;

  @Column(name = "status", nullable = false, length = 20)
  private String status = "COMPLETED";

  @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(name = "discount", nullable = false, precision = 14, scale = 2)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(name = "tax", nullable = false, precision = 14, scale = 2)
  private BigDecimal tax = BigDecimal.ZERO;

  @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal paidAmount = BigDecimal.ZERO;

  @Column(name = "returned_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal returnedAmount = BigDecimal.ZERO;

  @Column(name = "due_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal dueAmount = BigDecimal.ZERO;

  @Column(name = "notes", length = 255)
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private AccountsUser createdBy;

  public Sale() {}

  public Long getId() { return id; }
  public Venture getVenture() { return venture; }
  public void setVenture(Venture venture) { this.venture = venture; }
  public Customer getCustomer() { return customer; }
  public void setCustomer(Customer customer) { this.customer = customer; }
  public Warehouse getWarehouse() { return warehouse; }
  public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
  public String getInvoiceNumber() { return invoiceNumber; }
  public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
  public LocalDate getSaleDate() { return saleDate; }
  public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public BigDecimal getSubtotal() { return subtotal; }
  public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
  public BigDecimal getDiscount() { return discount; }
  public void setDiscount(BigDecimal discount) { this.discount = discount; }
  public BigDecimal getTax() { return tax; }
  public void setTax(BigDecimal tax) { this.tax = tax; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  public BigDecimal getPaidAmount() { return paidAmount; }
  public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
  public BigDecimal getReturnedAmount() { return returnedAmount; }
  public void setReturnedAmount(BigDecimal returnedAmount) { this.returnedAmount = returnedAmount; }
  public BigDecimal getDueAmount() { return dueAmount; }
  public void setDueAmount(BigDecimal dueAmount) { this.dueAmount = dueAmount; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public AccountsUser getCreatedBy() { return createdBy; }
  public void setCreatedBy(AccountsUser createdBy) { this.createdBy = createdBy; }
}
