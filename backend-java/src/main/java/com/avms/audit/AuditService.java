package com.avms.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Port of Django audit.services.log_audit: user + X-Forwarded-For-aware IP. */
@Service
public class AuditService {

  private static final Logger log = LoggerFactory.getLogger(AuditService.class);
  private final AuditLogRepository repository;

  public AuditService(AuditLogRepository repository) {
    this.repository = repository;
  }

  public void log(String action, String module, String objectType, String objectId, String description, String userEmail) {
    try {
      repository.save(new AuditLog(userEmail, action, module, objectType, objectId, clientIp(), description));
    } catch (Exception e) {
      log.warn("Audit write failed: {} {}", module, action, e);
    }
  }

  public void log(String action, String module, String objectType, String objectId, String description) {
    log(action, module, objectType, objectId, description, currentUserEmail());
  }

  public void log(String action, String module, String description) {
    log(action, module, null, null, description, currentUserEmail());
  }

  private String currentUserEmail() {
    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof com.avms.security.CustomUserDetails cud) {
      return cud.getEmail();
    }
    return auth != null ? auth.getName() : null;
  }

  private String clientIp() {
    try {
      var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs == null) {
        return null;
      }
      HttpServletRequest req = attrs.getRequest();
      String forwarded = req.getHeader("X-Forwarded-For");
      if (forwarded != null && !forwarded.isBlank()) {
        return forwarded.split(",")[0].trim();
      }
      return req.getRemoteAddr();
    } catch (Exception e) {
      return null;
    }
  }
}
