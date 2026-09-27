package com.avms.purchases;

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
@RequestMapping("/api/purchases")
public class PurchaseController {

  private final PurchaseService service;
  private final PermEvaluator perm;

  public PurchaseController(PurchaseService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<PurchaseDtos.PurchaseResponse>>> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status, Pageable pageable) {
    perm.require("purchases.view");
    var page = service.list(search, status, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<PurchaseDtos.PurchaseResponse>> create(
      @RequestBody PurchaseDtos.PurchaseRequest req) {
    perm.require("purchases.manage");
    return ResponseEntity.status(201).body(ApiResponse.ok(service.create(req), "Purchase created."));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PurchaseDtos.PurchaseResponse>> get(@PathVariable Long id) {
    perm.require("purchases.view");
    return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
  }

  @GetMapping("/{id}/items")
  public ResponseEntity<ApiResponse<List<PurchaseDtos.PurchaseItemResponse>>> items(@PathVariable Long id) {
    perm.require("purchases.view");
    return ResponseEntity.ok(ApiResponse.ok(service.lineItems(id)));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<PurchaseDtos.PurchaseResponse>> update(
      @PathVariable Long id, @RequestBody PurchaseDtos.PurchaseRequest req) {
    perm.require("purchases.manage");
    return ResponseEntity.ok(ApiResponse.ok(service.update(id, req), "Purchase updated."));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Map<String, String>>> delete(@PathVariable Long id) {
    perm.require("purchases.manage");
    service.delete(id);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "Purchase deleted.")));
  }
}
