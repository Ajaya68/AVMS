package com.avms.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Port of Django core.views.HealthView: GET /health + /api/health with DB probe. */
@RestController
public class HealthController {

  private final javax.sql.DataSource dataSource;

  public HealthController(javax.sql.DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @GetMapping({"/health", "/health/", "/api/health", "/api/health/"})
  public ResponseEntity<ApiResponse<java.util.Map<String, String>>> health() {
    try (var conn = dataSource.getConnection()) {
      boolean ok = conn.isValid(2);
      if (!ok) {
        return ResponseEntity.status(503)
            .body(ApiResponse.ok(java.util.Map.of("status", "degraded", "database", "unavailable"), "Service running but database is unavailable"));
      }
    } catch (Exception e) {
      return ResponseEntity.status(503)
          .body(ApiResponse.ok(java.util.Map.of("status", "degraded", "database", "unavailable"), "Service running but database is unavailable"));
    }
    return ResponseEntity.ok(
        ApiResponse.ok(
            java.util.Map.of("status", "ok", "service", "avms-backend", "version", "0.1.0", "database", "ok"),
            null));
  }
}
