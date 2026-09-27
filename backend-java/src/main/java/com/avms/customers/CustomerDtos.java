package com.avms.customers;

import java.math.BigDecimal;
import java.time.Instant;

public class CustomerDtos {

  public record CustomerRequest(Long venture, String name, String phone, String email, String address,
      String city, String state, String pincode, String gstNumber, BigDecimal creditLimit, String status) {
    public void validate(boolean create) {
      if (create && (name == null || name.isBlank())) {
        throw new IllegalArgumentException("name is required.");
      }
    }
  }

  public record CustomerResponse(Long id, Long venture, String ventureName, String customerCode, String name,
      String phone, String email, String address, String city, String state, String pincode, String gstNumber,
      BigDecimal creditLimit, String status, Instant createdAt, Instant updatedAt) {}
}
