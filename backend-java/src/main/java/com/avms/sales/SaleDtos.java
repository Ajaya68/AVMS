package com.avms.sales;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class SaleDtos {

  public record SaleItemRequest(Long product, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount,
      BigDecimal tax) {}

  public record SaleRequest(Long venture, Long customer, Long warehouse, LocalDate saleDate, BigDecimal discount,
      BigDecimal tax, String notes, String status, List<SaleItemRequest> items) {
    public void validate(boolean create) {
      if (create && customer == null) {
        throw new IllegalArgumentException("customer is required.");
      }
      if (create && saleDate == null) {
        throw new IllegalArgumentException("sale_date is required.");
      }
      if (create && (items == null || items.isEmpty())) {
        throw new IllegalArgumentException("items must not be empty.");
      }
    }
  }

  public record SaleItemResponse(Long id, Long product, String productCode, String productName, String unitCode,
      BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, BigDecimal tax, BigDecimal total) {}

  public record SaleResponse(Long id, Long venture, String ventureName, Long customer, String customerName,
      String customerCode, Long warehouse, String warehouseCode, String invoiceNumber, LocalDate saleDate,
      String status, BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal totalAmount,
      BigDecimal paidAmount, BigDecimal returnedAmount, BigDecimal dueAmount, String notes,
      List<SaleItemResponse> items, Instant createdAt, Instant updatedAt) {}

  public record SaleReturnItemRequest(Long product, BigDecimal quantity, BigDecimal unitPrice) {}

  public record SaleReturnRequest(Long venture, Long sale, LocalDate returnDate, String notes,
      List<SaleReturnItemRequest> items) {
    public void validate() {
      if (sale == null) {
        throw new IllegalArgumentException("sale is required.");
      }
      if (returnDate == null) {
        throw new IllegalArgumentException("return_date is required.");
      }
      if (items == null || items.isEmpty()) {
        throw new IllegalArgumentException("items must not be empty.");
      }
    }
  }

  public record SaleReturnItemResponse(Long id, Long product, String productCode, String productName,
      BigDecimal quantity, BigDecimal unitPrice, BigDecimal total) {}

  public record SaleReturnResponse(Long id, Long venture, Long sale, String invoiceNumber, String returnNumber,
      LocalDate returnDate, String status, BigDecimal totalAmount, String notes,
      List<SaleReturnItemResponse> items, Instant createdAt) {}
}
