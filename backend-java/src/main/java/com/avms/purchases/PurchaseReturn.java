package com.avms.purchases;

import com.avms.accounts.AccountsUser;
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
import java.time.LocalDate;

/** Port of Django purchases.PurchaseReturn: stock-reducing reversal against a bill. */
@Entity
@Table(name = "PURCHASES_PURCHASERETURN",
    uniqueConstraints = @UniqueConstraint(name = "uniq_purchasereturn_venture_number",
        columnNames = {"venture_id", "return_number"}))
public class PurchaseReturn extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venture_id", nullable = false)
  private Venture venture;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_id", nullable = false)
  private Purchase purchase;

  @Column(name = "return_number", nullable = false, length = 20)
  private String returnNumber;

  @Column(name = "return_date", nullable = false)
  private LocalDate returnDate;

  @Column(name = "status", nullable = false, length = 20)
  private String status = "COMPLETED";

  @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(name = "notes", length = 255)
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private AccountsUser createdBy;

  public PurchaseReturn() {}

  public Long getId() { return id; }
  public Venture getVenture() { return venture; }
  public void setVenture(Venture venture) { this.venture = venture; }
  public Purchase getPurchase() { return purchase; }
  public void setPurchase(Purchase purchase) { this.purchase = purchase; }
  public String getReturnNumber() { return returnNumber; }
  public void setReturnNumber(String returnNumber) { this.returnNumber = returnNumber; }
  public LocalDate getReturnDate() { return returnDate; }
  public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public AccountsUser getCreatedBy() { return createdBy; }
  public void setCreatedBy(AccountsUser createdBy) { this.createdBy = createdBy; }
}
