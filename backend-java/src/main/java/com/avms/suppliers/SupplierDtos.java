package com.avms.suppliers;

import java.time.Instant;

public class SupplierDtos {

  public record SupplierRequest(Long venture, String name, String contactPerson, String phone, String email,
      String address, String city, String state, String pincode, String gstNumber, String paymentTerms,
      String status) {
    public void validate(boolean create) {
      if (create && (name == null || name.isBlank())) {
        throw new IllegalArgumentException("name is required.");
      }
    }
  }

  public record SupplierResponse(Long id, Long venture, String ventureName, String supplierCode, String name,
      String contactPerson, String phone, String email, String address, String city, String state,
      String pincode, String gstNumber, String paymentTerms, String status, Instant createdAt,
      Instant updatedAt) {}
}
