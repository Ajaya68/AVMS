package com.avms.sales;

import com.avms.common.ApiResponse;
import com.avms.common.PageResponse;
import com.avms.security.PermEvaluator;
import java.util.List;
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
@RequestMapping("/api/sales")
public class SaleController {

  private final SaleService service;
  private final PermEvaluator perm;

  public SaleController(SaleService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<SaleDtos.SaleResponse>>> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status, Pageable pageable) {
    perm.require("sales.view");
    var page = service.list(search, status, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<SaleDtos.SaleResponse>> create(
      @RequestBody SaleDtos.SaleRequest req) {
    perm.require("sales.manage");
    return ResponseEntity.status(201).body(ApiResponse.ok(service.create(req), "Sale created."));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SaleDtos.SaleResponse>> get(@PathVariable Long id) {
    perm.require("sales.view");
    return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
  }

  @GetMapping("/{id}/items")
  public ResponseEntity<ApiResponse<List<SaleDtos.SaleItemResponse>>> items(@PathVariable Long id) {
    perm.require("sales.view");
    return ResponseEntity.ok(ApiResponse.ok(service.lineItems(id)));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<SaleDtos.SaleResponse>> update(
      @PathVariable Long id, @RequestBody SaleDtos.SaleRequest req) {
    perm.require("sales.manage");
    return ResponseEntity.ok(ApiResponse.ok(service.update(id, req), "Sale updated."));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Map<String, String>>> delete(@PathVariable Long id) {
    perm.require("sales.manage");
    service.delete(id);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "Sale deleted.")));
  }
}
