package com.avms.security;

import com.avms.common.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Port of Django User.has_permission_code + HasPermission: superuser bypasses all. */
@Component("perm")
public class PermEvaluator {

  public boolean has(String code) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof CustomUserDetails cud) {
      return cud.hasPermission(code);
    }
    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERUSER"));
  }

  public void require(String code) {
    if (!has(code)) {
      throw new ForbiddenException("You do not have permission to perform this action.");
    }
  }

  public CustomUserDetails currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
      return cud;
    }
    return null;
  }
}
