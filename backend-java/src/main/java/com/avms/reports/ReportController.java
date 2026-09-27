package com.avms.reports;

import com.avms.common.ApiResponse;
import com.avms.security.PermEvaluator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

  private final ReportService service;
  private final PermEvaluator perm;

  public ReportController(ReportService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping("/sales")
  public ResponseEntity<ApiResponse<ReportDtos.SalesReport>> sales(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    perm.require("reports.view");
    return ResponseEntity.ok(ApiResponse.ok(service.salesReport(from, to)));
  }

  @GetMapping("/purchases")
  public ResponseEntity<ApiResponse<ReportDtos.PurchasesReport>> purchases(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    perm.require("reports.view");
    return ResponseEntity.ok(ApiResponse.ok(service.purchasesReport(from, to)));
  }

  @GetMapping("/inventory")
  public ResponseEntity<ApiResponse<ReportDtos.InventoryReport>> inventory() {
    perm.require("reports.view");
    return ResponseEntity.ok(ApiResponse.ok(service.inventoryReport()));
  }

  @GetMapping("/financial")
  public ResponseEntity<ApiResponse<ReportDtos.FinancialReport>> financial(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    perm.require("reports.view");
    return ResponseEntity.ok(ApiResponse.ok(service.financialReport(from, to)));
  }
}
