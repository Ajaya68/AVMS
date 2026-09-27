package com.avms.expenses;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;

/** Port of Django expenses.ExpenseCategory: global static reference data. */
@Entity
@Table(name = "EXPENSES_CATEGORY")
public class ExpenseCategory {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "category_code", nullable = false, unique = true, length = 30)
  private String categoryCode;

  @Column(name = "category_name", nullable = false, length = 100)
  private String categoryName;

  public ExpenseCategory() {}

  public Long getId() { return id; }
  public String getCategoryCode() { return categoryCode; }
  public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
  public String getCategoryName() { return categoryName; }
  public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
