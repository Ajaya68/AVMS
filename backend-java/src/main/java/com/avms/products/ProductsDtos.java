package com.avms.products;

import com.avms.common.Validators;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public class ProductsDtos {

  private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

  public record UnitRequest(String unitCode, String unitName, Boolean isBase) {
    public void validate(boolean create) {
      if (create && (unitCode == null || unitCode.isBlank())) {
        throw new IllegalArgumentException("unit_code is required.");
      }
      if (create && (unitName == null || unitName.isBlank())) {
        throw new IllegalArgumentException("unit_name is required.");
      }
      Validators.text(unitCode, "unit_code", 20);
      Validators.text(unitName, "unit_name", 50);
    }
  }

  public record UnitResponse(Long id, String unitCode, String unitName, Boolean isBase) {}

  public record CategoryRequest(Long venture, String categoryName, String description, String status) {
    public void validate(boolean create) {
      if (create && (categoryName == null || categoryName.isBlank())) {
        throw new IllegalArgumentException("category_name is required.");
      }
      Validators.text(categoryName, "category_name", 100);
      Validators.text(description, "description", 500);
      Validators.allowedValue(status, STATUSES, "status");
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
      Validators.text(productName, "product_name", 150);
      Validators.text(description, "description", 1000);
      Validators.nonNegative(purchasePrice, "purchase_price");
      Validators.nonNegative(sellingPrice, "selling_price");
      Validators.nonNegative(taxRate, "tax_rate");
      Validators.nonNegative(reorderLevel, "reorder_level");
      Validators.allowedValue(status, STATUSES, "status");
    }
  }

  public record ProductResponse(Long id, Long venture, String ventureName, Long category, String categoryName,
      Long unit, String unitCode, String sku, String productName, String description, BigDecimal purchasePrice,
      BigDecimal sellingPrice, BigDecimal taxRate, BigDecimal reorderLevel, String status, Instant createdAt,
      Instant updatedAt) {}
}
