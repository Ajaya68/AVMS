package com.avms.ventures;

import jakarta.validation.constraints.NotBlank;

public class VentureDtos {

  public record VentureRequest(String ventureName, String description, String businessType, String phone,
      String email, String address, String city, String state, String pincode, String status) {
    public void validate(boolean create) {
      if (create && (ventureName == null || ventureName.isBlank())) {
        throw new IllegalArgumentException("ventureName is required.");
      }
    }
  }

  public record VentureResponse(Long id, String ventureCode, String ventureName, String description,
      String businessType, String phone, String email, String address, String city, String state,
      String pincode, String status) {}
}
