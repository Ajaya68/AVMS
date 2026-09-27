package com.avms.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class PaymentDtos {

  public record PaymentRequest(Long venture, String paymentType, String referenceType, Long referenceId,
      BigDecimal amount, LocalDate paymentDate, String paymentMethod, String transactionReference,
      String notes) {
    public void validate() {
      if (paymentType == null || paymentType.isBlank()) {
        throw new IllegalArgumentException("payment_type is required.");
      }
      if (referenceType == null || referenceType.isBlank()) {
        throw new IllegalArgumentException("reference_type is required.");
      }
      if (referenceId == null) {
        throw new IllegalArgumentException("reference_id is required.");
      }
      if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("amount must be greater than zero.");
      }
      if (paymentDate == null) {
        throw new IllegalArgumentException("payment_date is required.");
      }
    }
  }

  public record PaymentResponse(Long id, Long venture, String paymentType, String referenceType,
      Long referenceId, String referenceNumber, BigDecimal amount, LocalDate paymentDate,
      String paymentMethod, String transactionReference, String notes, Instant createdAt) {}
}
