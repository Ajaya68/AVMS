package com.avms.ventures;

import com.avms.common.Validators;
import java.util.Set;

public class VentureDtos {

  private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

  public record VentureRequest(String ventureName, String description, String businessType, String phone,
      String email, String address, String city, String state, String pincode, String status) {
    public void validate(boolean create) {
      if (create && (ventureName == null || ventureName.isBlank())) {
        throw new IllegalArgumentException("ventureName is required.");
      }
      Validators.text(ventureName, "venture_name", 150);
      Validators.text(description, "description", 1000);
      Validators.text(businessType, "business_type", 30);
      Validators.text(phone, "phone", 30);
      Validators.text(address, "address", 255);
      Validators.text(city, "city", 100);
      Validators.text(state, "state", 100);
      Validators.text(pincode, "pincode", 20);
      Validators.optionalEmail(email, "email");
      Validators.allowedValue(status, STATUSES, "status");
    }
  }

  public record VentureResponse(Long id, String ventureCode, String ventureName, String description,
      String businessType, String phone, String email, String address, String city, String state,
      String pincode, String status) {}
}
