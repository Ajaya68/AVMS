package com.avms.suppliers;

import com.avms.common.Validators;
import java.time.Instant;
import java.util.Set;

public class SupplierDtos {

  private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

  public record SupplierRequest(Long venture, String name, String contactPerson, String phone, String email,
      String address, String city, String state, String pincode, String gstNumber, String paymentTerms,
      String status) {
    public void validate(boolean create) {
      if (create && (name == null || name.isBlank())) {
        throw new IllegalArgumentException("name is required.");
      }
      Validators.text(name, "name", 150);
      Validators.text(contactPerson, "contact_person", 150);
      Validators.text(phone, "phone", 30);
      Validators.text(address, "address", 255);
      Validators.text(city, "city", 100);
      Validators.text(state, "state", 100);
      Validators.text(pincode, "pincode", 20);
      Validators.text(gstNumber, "gst_number", 30);
      Validators.text(paymentTerms, "payment_terms", 100);
      Validators.optionalEmail(email, "email");
      Validators.allowedValue(status, STATUSES, "status");
    }
  }

  public record SupplierResponse(Long id, Long venture, String ventureName, String supplierCode, String name,
      String contactPerson, String phone, String email, String address, String city, String state,
      String pincode, String gstNumber, String paymentTerms, String status, Instant createdAt,
      Instant updatedAt) {}
}
