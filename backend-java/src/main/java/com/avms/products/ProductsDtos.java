package com.avms.products;

import java.math.BigDecimal;
import java.time.Instant;

public class ProductsDtos {

  public record UnitRequest(String unitCode, String unitName, Boolean isBase) {
    public void validate(boolean create) {
      if (create && (unitCode == null || unitCode.isBlank())) {
        throw new IllegalArgumentException("unit_code is required.");
      }
      if (create && (unitName == null || unitName.isBlank())) {
        throw new IllegalArgumentException("unit_name is required.");
      }
    }
  }

  public record UnitResponse(Long id, String unitCode, String unitName, Boolean isBase) {}

  public record CategoryRequest(Long venture, String categoryName, String description, String status) {
    public void validate(boolean create) {
      if (create && (categoryName == null || categoryName.isBlank())) {
        throw new IllegalArgumentException("category_name is required.");
      }
    }
  }

  public record CategoryResponse(Long id, Long venture, String ventureName, String categoryName,
      String description, String status, Instant createdAt, Instant updatedAt) {}

  public record ProductRequest(Long venture, Long category, Long unit, String productName, String description,
      BigDecimal purchasePrice, BigDecimal sellingPrice, BigDecimal taxRate, BigDecimal reorderLevel,
      String status) {
    public void validate(boolean create) {
      if (create && (productName == null || productName.isBlank())) {
        throw new IllegalArgumentException("product_name is required.");
      }
      if (create && unit == null) {
        throw new IllegalArgumentException("unit is required.");
      }
    }
  }

  public record ProductResponse(Long id, Long venture, String ventureName, Long category, String categoryName,
      Long unit, String unitCode, String sku, String productName, String description, BigDecimal purchasePrice,
      BigDecimal sellingPrice, BigDecimal taxRate, BigDecimal reorderLevel, String status, Instant createdAt,
      Instant updatedAt) {}
}
