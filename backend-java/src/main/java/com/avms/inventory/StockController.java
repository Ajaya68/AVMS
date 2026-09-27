package com.avms.inventory;

import com.avms.common.ApiResponse;
import com.avms.common.PageResponse;
import com.avms.security.PermEvaluator;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class StockController {

  private final InventoryService service;
  private final PermEvaluator perm;

  public StockController(InventoryService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<InventoryDtos.StockResponse>>> list(
      @RequestParam(required = false) Long warehouse,
      @RequestParam(required = false) Long product,
      @RequestParam(required = false) String low, Pageable pageable) {
    perm.require("inventory.view");
    boolean lowOnly = low != null && (low.equalsIgnoreCase("true") || low.equals("1"));
    var page = service.listStocks(warehouse, product, lowOnly, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }
}
