package com.avms.employees;

import com.avms.common.ApiResponse;
import com.avms.common.PageResponse;
import com.avms.security.PermEvaluator;
import java.util.Map;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/employees")
public class EmployeeController {

  private final EmployeeService service;
  private final PermEvaluator perm;

  public EmployeeController(EmployeeService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<EmployeeDtos.EmployeeResponse>>> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long venture,
      @RequestParam(required = false) String department,
      @RequestParam(required = false) String designation, Pageable pageable) {
    perm.require("employees.view");
    var page = service.list(search, status, venture, department, designation, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> create(
      @RequestBody EmployeeDtos.EmployeeRequest req) {
    perm.require("employees.manage");
    return ResponseEntity.status(201).body(ApiResponse.ok(service.create(req), "Employee created."));
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<EmployeeDtos.MyProfileResponse>> me() {
    return ResponseEntity.ok(ApiResponse.ok(service.myProfile()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> get(@PathVariable Long id) {
    perm.require("employees.view");
    return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> update(
      @PathVariable Long id, @RequestBody EmployeeDtos.EmployeeRequest req) {
    perm.require("employees.manage");
    return ResponseEntity.ok(ApiResponse.ok(service.update(id, req), "Employee updated."));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Map<String, String>>> delete(@PathVariable Long id) {
    perm.require("employees.manage");
    service.delete(id);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "Employee deleted.")));
  }
}
