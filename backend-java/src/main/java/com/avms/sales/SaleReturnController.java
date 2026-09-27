package com.avms.sales;

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
@RequestMapping("/api/sales-returns")
public class SaleReturnController {

  private final SaleService service;
  private final PermEvaluator perm;

  public SaleReturnController(SaleService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<SaleDtos.SaleReturnResponse>>> list(
      @RequestParam(required = false) String search, Pageable pageable) {
    perm.require("sales_returns.view");
    var page = service.listReturns(search, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<SaleDtos.SaleReturnResponse>> create(
      @RequestBody SaleDtos.SaleReturnRequest req) {
    perm.require("sales_returns.manage");
    return ResponseEntity.status(201)
        .body(ApiResponse.ok(service.createReturn(req), "Sales return created."));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SaleDtos.SaleReturnResponse>> get(@PathVariable Long id) {
    perm.require("sales_returns.view");
    return ResponseEntity.ok(ApiResponse.ok(service.getReturn(id)));
  }
}
