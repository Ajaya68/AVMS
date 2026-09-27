package com.avms.expenses;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExpenseController {

  private final ExpenseService service;
  private final PermEvaluator perm;

  public ExpenseController(ExpenseService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping("/api/expense-categories")
  public ResponseEntity<ApiResponse<List<ExpenseDtos.CategoryResponse>>> categories() {
    perm.require("expenses.view");
    return ResponseEntity.ok(ApiResponse.ok(service.listCategories()));
  }

  @GetMapping("/api/expenses")
  public ResponseEntity<ApiResponse<PageResponse<ExpenseDtos.ExpenseResponse>>> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long category, Pageable pageable) {
    perm.require("expenses.view");
    var page = service.list(search, category, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping("/api/expenses")
  public ResponseEntity<ApiResponse<ExpenseDtos.ExpenseResponse>> create(
      @RequestBody ExpenseDtos.ExpenseRequest req) {
    perm.require("expenses.manage");
    return ResponseEntity.status(201).body(ApiResponse.ok(service.create(req), "Expense recorded."));
  }

  @GetMapping("/api/expenses/{id}")
  public ResponseEntity<ApiResponse<ExpenseDtos.ExpenseResponse>> get(@PathVariable Long id) {
    perm.require("expenses.view");
    return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
  }

  @PatchMapping("/api/expenses/{id}")
  public ResponseEntity<ApiResponse<ExpenseDtos.ExpenseResponse>> update(
      @PathVariable Long id, @RequestBody ExpenseDtos.ExpenseRequest req) {
    perm.require("expenses.manage");
    return ResponseEntity.ok(ApiResponse.ok(service.update(id, req), "Expense updated."));
  }

  @DeleteMapping("/api/expenses/{id}")
  public ResponseEntity<ApiResponse<Map<String, String>>> delete(@PathVariable Long id) {
    perm.require("expenses.manage");
    service.delete(id);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "Expense deleted.")));
  }
}
