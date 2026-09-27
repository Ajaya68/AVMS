package com.avms.expenses;

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

/** Port of Django expenses.Expense: venture-scoped operating expense. */
@Entity
@Table(name = "EXPENSES_EXPENSE")
public class Expense extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venture_id", nullable = false)
  private Venture venture;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private ExpenseCategory category;

  @Column(name = "amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "expense_date", nullable = false)
  private LocalDate expenseDate;

  @Column(name = "payment_method", nullable = false, length = 20)
  private String paymentMethod;

  @Column(name = "description", length = 255)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private AccountsUser createdBy;

  public Expense() {}

  public Long getId() { return id; }
  public Venture getVenture() { return venture; }
  public void setVenture(Venture venture) { this.venture = venture; }
  public ExpenseCategory getCategory() { return category; }
  public void setCategory(ExpenseCategory category) { this.category = category; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public LocalDate getExpenseDate() { return expenseDate; }
  public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
  public String getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public AccountsUser getCreatedBy() { return createdBy; }
  public void setCreatedBy(AccountsUser createdBy) { this.createdBy = createdBy; }
}
