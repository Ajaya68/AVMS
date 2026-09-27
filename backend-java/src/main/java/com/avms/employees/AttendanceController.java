package com.avms.employees;

import com.avms.common.ApiResponse;
import com.avms.common.PageResponse;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

  private final EmployeeService service;

  public AttendanceController(EmployeeService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<EmployeeDtos.AttendanceResponse>>> list(
      @RequestParam(required = false) Long venture,
      @RequestParam(required = false) Long employee,
      @RequestParam(required = false) String department,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      Pageable pageable) {
    var page = service.listAttendance(venture, employee, department, status, from, to, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<EmployeeDtos.AttendanceResponse>> mark(
      @RequestBody EmployeeDtos.AttendanceRequest req) {
    return ResponseEntity.status(201)
        .body(ApiResponse.ok(service.mark(req), "Attendance marked."));
  }

  @PostMapping("/bulk")
  public ResponseEntity<ApiResponse<EmployeeDtos.BulkResult>> bulk(
      @RequestBody EmployeeDtos.BulkRequest req) {
    return ResponseEntity.status(201)
        .body(ApiResponse.ok(service.bulk(req), "Bulk attendance recorded."));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<EmployeeDtos.AttendanceResponse>> update(
      @PathVariable Long id, @RequestBody EmployeeDtos.AttendanceRequest req) {
    return ResponseEntity.ok(ApiResponse.ok(service.updateAttendance(id, req), "Attendance updated."));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Map<String, String>>> delete(@PathVariable Long id) {
    service.deleteAttendance(id);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "Attendance deleted.")));
  }
}
