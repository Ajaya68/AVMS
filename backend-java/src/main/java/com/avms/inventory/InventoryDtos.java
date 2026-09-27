package com.avms.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class InventoryDtos {

  public record WarehouseRequest(Long venture, String warehouseName, String address, String city, String state,
      String pincode, String manager, String status) {
    public void validate(boolean create) {
      if (create && (warehouseName == null || warehouseName.isBlank())) {
        throw new IllegalArgumentException("warehouse_name is required.");
      }
    }
  }

  public record WarehouseResponse(Long id, Long venture, String ventureName, String warehouseCode,
      String warehouseName, String address, String city, String state, String pincode, String manager,
      String status, Instant createdAt, Instant updatedAt) {}

  public record MovementRequest(Long venture, Long warehouse, Long destinationWarehouse, Long product,
      String movementType, BigDecimal quantity, LocalDate movementDate, String notes, String referenceType,
      Long referenceId) {
    public void validate() {
      if (warehouse == null) {
        throw new IllegalArgumentException("warehouse is required.");
      }
      if (product == null) {
        throw new IllegalArgumentException("product is required.");
      }
      if (movementType == null || movementType.isBlank()) {
        throw new IllegalArgumentException("movement_type is required.");
      }
      if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("quantity must be greater than zero.");
      }
      if (movementDate == null) {
        throw new IllegalArgumentException("movement_date is required.");
      }
    }
  }

  public record MovementResponse(Long id, Long venture, Long warehouse, String warehouseCode,
      Long destinationWarehouse, String destinationWarehouseCode, Long product, String productCode,
      String productName, String movementType, BigDecimal quantity, LocalDate movementDate, String notes,
      String referenceType, Long referenceId, Long pairedMovement, String createdByEmail,
      Instant createdAt) {}

  public record StockResponse(Long id, Long venture, Long product, String productCode, String productName,
      Long warehouse, String warehouseCode, String warehouseName, BigDecimal quantity,
      BigDecimal reservedQuantity, BigDecimal available, BigDecimal reorderLevel, boolean isLow) {}
}
