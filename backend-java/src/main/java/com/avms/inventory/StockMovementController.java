package com.avms.inventory;

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
@RequestMapping("/api/stock-movements")
public class StockMovementController {

  private final InventoryService service;
  private final PermEvaluator perm;

  public StockMovementController(InventoryService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<InventoryDtos.MovementResponse>>> list(
      @RequestParam(required = false) Long warehouse,
      @RequestParam(required = false) Long product,
      @RequestParam(name = "movement_type", required = false) String movementType,
      Pageable pageable) {
    perm.require("stock_movements.view");
    var page = service.listMovements(warehouse, product, movementType, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<InventoryDtos.MovementResponse>> record(
      @RequestBody InventoryDtos.MovementRequest req) {
    perm.require("stock_movements.manage");
    return ResponseEntity.status(201)
        .body(ApiResponse.ok(service.record(req), "Stock movement recorded."));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<InventoryDtos.MovementResponse>> get(@PathVariable Long id) {
    perm.require("stock_movements.view");
    return ResponseEntity.ok(ApiResponse.ok(service.getMovement(id)));
  }
}
