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
@RequestMapping("/api/categories")
public class CategoryController {

  private final CategoryService service;
  private final PermEvaluator perm;

  public CategoryController(CategoryService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<ProductsDtos.CategoryResponse>>> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status, Pageable pageable) {
    perm.require("categories.view");
    var page = service.list(search, status, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<ProductsDtos.CategoryResponse>> create(
      @RequestBody ProductsDtos.CategoryRequest req) {
    perm.require("categories.manage");
    return ResponseEntity.status(201).body(ApiResponse.ok(service.create(req), "Category created."));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ProductsDtos.CategoryResponse>> get(@PathVariable Long id) {
    perm.require("categories.view");
    return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<ProductsDtos.CategoryResponse>> update(
      @PathVariable Long id, @RequestBody ProductsDtos.CategoryRequest req) {
    perm.require("categories.manage");
    return ResponseEntity.ok(ApiResponse.ok(service.update(id, req), "Category updated."));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Map<String, String>>> delete(@PathVariable Long id) {
    perm.require("categories.manage");
    service.delete(id);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "Category deleted.")));
  }
}
