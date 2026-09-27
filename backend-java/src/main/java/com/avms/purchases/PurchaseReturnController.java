package com.avms.purchases;

import com.avms.common.ApiResponse;
import com.avms.common.PageResponse;
import com.avms.security.PermEvaluator;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-returns")
public class PurchaseReturnController {

  private final PurchaseService service;
  private final PermEvaluator perm;

  public PurchaseReturnController(PurchaseService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<PurchaseDtos.PurchaseReturnResponse>>> list(
      @RequestParam(required = false) String search, Pageable pageable) {
    perm.require("purchase_returns.view");
    var page = service.listReturns(search, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<PurchaseDtos.PurchaseReturnResponse>> create(
      @RequestBody PurchaseDtos.PurchaseReturnRequest req) {
    perm.require("purchase_returns.manage");
    return ResponseEntity.status(201)
        .body(ApiResponse.ok(service.createReturn(req), "Purchase return created."));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PurchaseDtos.PurchaseReturnResponse>> get(@PathVariable Long id) {
    perm.require("purchase_returns.view");
    return ResponseEntity.ok(ApiResponse.ok(service.getReturn(id)));
  }
}
