package com.avms.customers;

import com.avms.common.Validators;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public class CustomerDtos {

  private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

  public record CustomerRequest(Long venture, String name, String phone, String email, String address,
      String city, String state, String pincode, String gstNumber, BigDecimal creditLimit, String status) {
    public void validate(boolean create) {
      if (create && (name == null || name.isBlank())) {
        throw new IllegalArgumentException("name is required.");
      }
      Validators.text(name, "name", 150);
      Validators.text(phone, "phone", 30);
      Validators.text(address, "address", 255);
      Validators.text(city, "city", 100);
      Validators.text(state, "state", 100);
      Validators.text(pincode, "pincode", 20);
      Validators.text(gstNumber, "gst_number", 30);
      Validators.optionalEmail(email, "email");
      Validators.nonNegative(creditLimit, "credit_limit");
      Validators.allowedValue(status, STATUSES, "status");
    }
  }

  public record CustomerResponse(Long id, Long venture, String ventureName, String customerCode, String name,
      String phone, String email, String address, String city, String state, String pincode, String gstNumber,
      BigDecimal creditLimit, String status, Instant createdAt, Instant updatedAt) {}
}
