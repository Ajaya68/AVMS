package com.avms.audit;

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
@RequestMapping("/api/audit-logs")
public class AuditController {

  private final AuditLogRepository repository;
  private final PermEvaluator perm;

  public AuditController(AuditLogRepository repository, PermEvaluator perm) {
    this.repository = repository;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> list(
      @RequestParam(defaultValue = "") String module,
      @RequestParam(defaultValue = "") String action, Pageable pageable) {
    perm.require("audit.view");
    var page = repository.findByModuleContainingIgnoreCaseAndActionContainingIgnoreCase(module, action, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }
}
