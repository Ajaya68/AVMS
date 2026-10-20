package com.avms.common;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Shared input validation helpers for module DTOs: length caps (mirroring the
 * VARCHAR2 limits in the Flyway migrations), optional-email format, numeric
 * floors, and enum allowlists. Check-only: never mutates values, so partial
 * PATCH flows keep their null-tolerant behavior. Failures throw
 * IllegalArgumentException, which GlobalExceptionHandler maps to 400 with the
 * field message (static strings only — no internals echoed).
 */
public final class Validators {

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]+\\.[^@\\s]{1,32}$");
  private static final int MAX_EMAIL = 254;

  private Validators() {}

  /** Length cap for optional or required text. Null passes; blank passes. */
  public static void text(String value, String field, int maxLen) {
    if (value != null && value.length() > maxLen) {
      throw new IllegalArgumentException(field + " must not exceed " + maxLen + " characters.");
    }
  }

  /** RFC-style minimum email check when the caller chose to provide one. */
  public static void optionalEmail(String value, String field) {
    if (value == null || value.isBlank()) {
      return;
    }
    if (value.length() > MAX_EMAIL || !EMAIL.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " must be a valid email address.");
    }
  }

  /** Money/quantity floor: negatives are never valid on these fields. */
  public static void nonNegative(BigDecimal value, String field) {
    if (value != null && value.signum() < 0) {
      throw new IllegalArgumentException(field + " must not be negative.");
    }
  }

  /** Allowlist for enum-like columns. Blank passes (service defaults apply). */
  public static void allowedValue(String value, Set<String> allowed, String field) {
    if (value == null || value.isBlank()) {
      return;
    }
    if (!allowed.contains(value.trim().toUpperCase())) {
      throw new IllegalArgumentException(
          field + " must be one of: " + String.join(", ", allowed) + ".");
    }
  }
}
