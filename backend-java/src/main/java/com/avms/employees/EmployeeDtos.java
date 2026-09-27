package com.avms.employees;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class EmployeeDtos {

  public record EmployeeRequest(Long venture, String firstName, String lastName, String phone, String email,
      String department, String designation, LocalDate joiningDate, BigDecimal salary, String status,
      Long user) {
    public void validate(boolean create) {
      if (create && (firstName == null || firstName.isBlank())) {
        throw new IllegalArgumentException("first_name is required.");
      }
    }
  }

  public record EmployeeResponse(Long id, Long venture, String ventureName, String employeeCode,
      String firstName, String lastName, String fullName, String phone, String email, String department,
      String designation, LocalDate joiningDate, BigDecimal salary, String status, Long user,
      Instant createdAt, Instant updatedAt) {}

  public record AttendanceRequest(Long employee, LocalDate date, String status, String checkIn,
      String checkOut, String notes) {}

  public record AttendanceResponse(Long id, Long employee, String employeeName, LocalDate date, String status,
      LocalTime checkIn, LocalTime checkOut, String notes) {}

  public record BulkItem(Long employee, String status, String remarks) {}

  public record BulkRequest(LocalDate date, List<BulkItem> items) {
    public void validate() {
      if (date == null) {
        throw new IllegalArgumentException("date is required.");
      }
      if (items == null || items.isEmpty()) {
        throw new IllegalArgumentException("items must not be empty.");
      }
    }
  }

  public record BulkResult(int created, int updated, List<AttendanceResponse> results) {}

  public record MyProfileResponse(boolean linked, EmployeeResponse employee, AttendanceResponse today,
      Map<String, Long> monthSummary) {}
}
