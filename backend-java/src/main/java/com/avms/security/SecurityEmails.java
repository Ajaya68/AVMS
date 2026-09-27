package com.avms.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Resolves the signed-in user's email. CustomUserDetails is not a UserDetails,
 *  so Authentication.getName() would return Object.toString() instead of the email. */
public final class SecurityEmails {

  private SecurityEmails() {}

  public static String currentUserEmail() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return null;
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof CustomUserDetails cud) {
      return cud.getEmail();
    }
    return auth.getName();
  }
}
