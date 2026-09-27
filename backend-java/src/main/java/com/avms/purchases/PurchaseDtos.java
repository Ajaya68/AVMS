package com.avms.purchases;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class PurchaseDtos {

  public record PurchaseItemRequest(Long product, BigDecimal quantity, BigDecimal unitPrice,
      BigDecimal discount, BigDecimal tax) {}

  public record PurchaseRequest(Long venture, Long supplier, Long warehouse, LocalDate purchaseDate,
      BigDecimal discount, BigDecimal tax, String notes, String status, List<PurchaseItemRequest> items) {
    public void validate(boolean create) {
      if (create && supplier == null) {
        throw new IllegalArgumentException("supplier is required.");
      }
      if (create && purchaseDate == null) {
        throw new IllegalArgumentException("purchase_date is required.");
      }
      if (create && (items == null || items.isEmpty())) {
        throw new IllegalArgumentException("items must not be empty.");
      }
    }
  }

  public record PurchaseItemResponse(Long id, Long product, String productCode, String productName,
      String unitCode, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, BigDecimal tax,
      BigDecimal total) {}

  public record PurchaseResponse(Long id, Long venture, String ventureName, Long supplier, String supplierName,
      String supplierCode, Long warehouse, String warehouseCode, String invoiceNumber, LocalDate purchaseDate,
      String status, BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal totalAmount,
      BigDecimal paidAmount, BigDecimal returnedAmount, BigDecimal dueAmount, String notes,
      List<PurchaseItemResponse> items, Instant createdAt, Instant updatedAt) {}

  public record PurchaseReturnItemRequest(Long product, BigDecimal quantity, BigDecimal unitPrice) {}

  public record PurchaseReturnRequest(Long venture, Long purchase, LocalDate returnDate, String notes,
      List<PurchaseReturnItemRequest> items) {
    public void validate() {
      if (purchase == null) {
        throw new IllegalArgumentException("purchase is required.");
      }
      if (returnDate == null) {
        throw new IllegalArgumentException("return_date is required.");
      }
      if (items == null || items.isEmpty()) {
        throw new IllegalArgumentException("items must not be empty.");
      }
    }
  }

  public record PurchaseReturnItemResponse(Long id, Long product, String productCode, String productName,
      BigDecimal quantity, BigDecimal unitPrice, BigDecimal total) {}

  public record PurchaseReturnResponse(Long id, Long venture, Long purchase, String invoiceNumber,
      String returnNumber, LocalDate returnDate, String status, BigDecimal totalAmount, String notes,
      List<PurchaseReturnItemResponse> items, Instant createdAt) {}
}
