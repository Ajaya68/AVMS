package com.avms.payments;

import com.avms.common.ApiResponse;
import com.avms.common.PageResponse;
import com.avms.security.PermEvaluator;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final PaymentService service;
  private final PermEvaluator perm;

  public PaymentController(PaymentService service, PermEvaluator perm) {
    this.service = service;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<PaymentDtos.PaymentResponse>>> list(
      @RequestParam(name = "payment_type", required = false) String paymentType,
      @RequestParam(name = "reference_type", required = false) String referenceType,
      @RequestParam(required = false) String search, Pageable pageable) {
    perm.require("payments.view");
    var page = service.list(paymentType, referenceType, search, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<PaymentDtos.PaymentResponse>> create(
      @RequestBody PaymentDtos.PaymentRequest req) {
    perm.require("payments.manage");
    return ResponseEntity.status(201).body(ApiResponse.ok(service.create(req), "Payment recorded."));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PaymentDtos.PaymentResponse>> get(@PathVariable Long id) {
    perm.require("payments.view");
    return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Map<String, String>>> delete(@PathVariable Long id) {
    perm.require("payments.manage");
    service.delete(id);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "Payment reversed.")));
  }
}
