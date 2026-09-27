package com.avms.common;

/**
 * Request-scoped holder for the X-Venture-Id header, port of Django
 * ventures.services.get_request_venture. Populated by VentureScopeFilter.
 */
public final class VentureContextHolder {
  private static final ThreadLocal<Long> VENTURE_ID = new ThreadLocal<>();

  private VentureContextHolder() {}

  public static void set(Long ventureId) {
    VENTURE_ID.set(ventureId);
  }

  public static Long get() {
    return VENTURE_ID.get();
  }

  public static void clear() {
    VENTURE_ID.remove();
  }
}
