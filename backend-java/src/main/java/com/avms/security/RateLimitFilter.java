package com.avms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Minimal in-app abuse control: per-IP fixed-window rate limits on the API.
 *
 * <p>Stricter buckets for credential endpoints ({@code /api/auth/login},
 * {@code /api/auth/refresh}); a generous default bucket for everything else so
 * bulk flows (attendance, reports) are unaffected. Health and (dev-only) docs
 * paths are excluded so load-balancer probes can never be throttled.
 *
 * <p>This is defense-in-depth, not a DDoS solution: edge rate limiting
 * (WAF/CDN/proxy) is still required for production. For multi-instance
 * deployments replace the in-memory map with Redis-backed buckets.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final boolean enabled;
  private final int loginMax;
  private final int refreshMax;
  private final int defaultMax;
  private final long windowMs;

  private final ConcurrentHashMap<String, FixedWindow> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(
      @Value("${avms.rate-limit.enabled:true}") boolean enabled,
      @Value("${avms.rate-limit.login-max:20}") int loginMax,
      @Value("${avms.rate-limit.refresh-max:60}") int refreshMax,
      @Value("${avms.rate-limit.default-max:600}") int defaultMax,
      @Value("${avms.rate-limit.window-seconds:60}") long windowSeconds) {
    this.enabled = enabled;
    this.loginMax = loginMax;
    this.refreshMax = refreshMax;
    this.defaultMax = defaultMax;
    this.windowMs = windowSeconds * 1000L;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.equals("/health") || path.equals("/health/")
        || path.equals("/api/health") || path.equals("/api/health/")
        || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")
        || path.startsWith("/api/schema") || path.startsWith("/api/docs");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    if (enabled && request.getRequestURI() != null && request.getRequestURI().startsWith("/api/")) {
      String path = request.getRequestURI();
      int max = defaultMax;
      String bucket = "api";
      if (path.startsWith("/api/auth/login")) {
        max = loginMax;
        bucket = "login";
      } else if (path.startsWith("/api/auth/refresh")) {
        max = refreshMax;
        bucket = "refresh";
      }
      if (!allow(clientIp(request) + "|" + bucket, max)) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        response.getWriter().write(
            "{\"success\":false,\"message\":\"Too many requests. Please slow down and retry later.\",\"errors\":{}}");
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private boolean allow(String key, int max) {
    if (windows.size() > 20000) {
      long now = System.currentTimeMillis();
      windows.entrySet().removeIf(e -> now - e.getValue().start >= windowMs);
    }
    long now = System.currentTimeMillis();
    FixedWindow w = windows.compute(key, (k, existing) -> {
      if (existing == null || now - existing.start >= windowMs) {
        return new FixedWindow(now, 1);
      }
      existing.count++;
      return existing;
    });
    return w.count <= max;
  }

  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    String real = request.getHeader("X-Real-IP");
    if (real != null && !real.isBlank()) {
      return real.trim();
    }
    return request.getRemoteAddr();
  }

  private static final class FixedWindow {
    final long start;
    int count;

    FixedWindow(long start, int count) {
      this.start = start;
      this.count = count;
    }
  }
}
