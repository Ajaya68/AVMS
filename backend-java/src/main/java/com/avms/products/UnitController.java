package com.avms.products;

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
@RequestMapping("/api/units")
public class UnitController {

  private final UnitService service;
  private final PermEvaluator perm;

  public UnitController(UnitService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<ProductsDtos.UnitResponse>>> list(
      @RequestParam(required = false) String search, Pageable pageable) {
    perm.require("units.view");
    var page = service.list(search, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<ProductsDtos.UnitResponse>> create(
      @RequestBody ProductsDtos.UnitRequest req) {
    perm.require("units.manage");
    return ResponseEntity.status(201).body(ApiResponse.ok(service.create(req), "Unit created."));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ProductsDtos.UnitResponse>> get(@PathVariable Long id) {
    perm.require("units.view");
    return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<ProductsDtos.UnitResponse>> update(
      @PathVariable Long id, @RequestBody ProductsDtos.UnitRequest req) {
    perm.require("units.manage");
    return ResponseEntity.ok(ApiResponse.ok(service.update(id, req), "Unit updated."));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Map<String, String>>> delete(@PathVariable Long id) {
    perm.require("units.manage");
    service.delete(id);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "Unit deleted.")));
  }
}
