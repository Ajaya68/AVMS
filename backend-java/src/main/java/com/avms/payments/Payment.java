package com.avms.payments;

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
import java.math.BigDecimal;
import java.time.LocalDate;

/** Port of Django payments.Payment: money in (RECEIVED/sale) or out (PAID/purchase). */
@Entity
@Table(name = "PAYMENTS_PAYMENT")
public class Payment extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venture_id", nullable = false)
  private Venture venture;

  @Column(name = "payment_type", nullable = false, length = 20)
  private String paymentType;

  @Column(name = "reference_type", nullable = false, length = 20)
  private String referenceType;

  @Column(name = "reference_id", nullable = false)
  private Long referenceId;

  @Column(name = "amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "payment_date", nullable = false)
  private LocalDate paymentDate;

  @Column(name = "payment_method", nullable = false, length = 20)
  private String paymentMethod;

  @Column(name = "transaction_reference", length = 100)
  private String transactionReference;

  @Column(name = "notes", length = 255)
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private AccountsUser createdBy;

  public Payment() {}

  public Long getId() { return id; }
  public Venture getVenture() { return venture; }
  public void setVenture(Venture venture) { this.venture = venture; }
  public String getPaymentType() { return paymentType; }
  public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
  public String getReferenceType() { return referenceType; }
  public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
  public Long getReferenceId() { return referenceId; }
  public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public LocalDate getPaymentDate() { return paymentDate; }
  public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
  public String getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
  public String getTransactionReference() { return transactionReference; }
  public void setTransactionReference(String transactionReference) {
    this.transactionReference = transactionReference;
  }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public AccountsUser getCreatedBy() { return createdBy; }
  public void setCreatedBy(AccountsUser createdBy) { this.createdBy = createdBy; }
}
