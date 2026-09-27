package com.avms.expenses;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class ExpenseDtos {

  public record CategoryResponse(Long id, String categoryCode, String categoryName) {}

  public record ExpenseRequest(Long venture, Long category, BigDecimal amount, LocalDate expenseDate,
      String paymentMethod, String description) {
    public void validate(boolean create) {
      if (create && category == null) {
        throw new IllegalArgumentException("category is required.");
      }
      if (create && (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)) {
        throw new IllegalArgumentException("amount must be greater than zero.");
      }
      if (create && expenseDate == null) {
        throw new IllegalArgumentException("expense_date is required.");
      }
    }
  }

  public record ExpenseResponse(Long id, Long venture, Long category, String categoryName, BigDecimal amount,
      LocalDate expenseDate, String paymentMethod, String description, Instant createdAt) {}
}
