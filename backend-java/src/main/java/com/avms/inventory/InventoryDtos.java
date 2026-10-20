package com.avms.inventory;

import com.avms.common.Validators;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public class InventoryDtos {

  private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

  public record WarehouseRequest(Long venture, String warehouseName, String address, String city, String state,
      String pincode, String manager, String status) {
    public void validate(boolean create) {
      if (create && (warehouseName == null || warehouseName.isBlank())) {
        throw new IllegalArgumentException("warehouse_name is required.");
      }
      Validators.text(warehouseName, "warehouse_name", 150);
      Validators.text(address, "address", 255);
      Validators.text(city, "city", 100);
      Validators.text(state, "state", 100);
      Validators.text(pincode, "pincode", 20);
      Validators.text(manager, "manager", 150);
      Validators.allowedValue(status, STATUSES, "status");
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
      Validators.text(movementType, "movement_type", 20);
      Validators.text(notes, "notes", 255);
      Validators.text(referenceType, "reference_type", 80);
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
