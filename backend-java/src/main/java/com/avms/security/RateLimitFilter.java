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
 *
 * <p>Client IP comes from {@code request.getRemoteAddr()} — never from raw
 * {@code X-Forwarded-For}, which any client can set when the backend is
 * directly reachable and would let it rotate buckets to bypass throttling.
 * Trusted-proxy resolution happens once via
 * {@code server.forward-headers-strategy: framework} (prod profile) so remoteAddr
 * is the Caddy-resolved client IP on the VPS.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final boolean enabled;
  private final int loginMax;
  private final int refreshMax;
  private final int mutateMax;
  private final int defaultMax;
  private final long windowMs;

  private final ConcurrentHashMap<String, FixedWindow> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(
      @Value("${avms.rate-limit.enabled:true}") boolean enabled,
      @Value("${avms.rate-limit.login-max:20}") int loginMax,
      @Value("${avms.rate-limit.refresh-max:60}") int refreshMax,
      @Value("${avms.rate-limit.mutate-max:120}") int mutateMax,
      @Value("${avms.rate-limit.default-max:600}") int defaultMax,
      @Value("${avms.rate-limit.window-seconds:60}") long windowSeconds) {
    this.enabled = enabled;
    this.loginMax = loginMax;
    this.refreshMax = refreshMax;
    this.mutateMax = mutateMax;
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
      if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/forgot-password")
          || path.startsWith("/api/auth/reset-password")) {
        max = loginMax;
        bucket = "login";
      } else if (path.startsWith("/api/auth/refresh")) {
        max = refreshMax;
        bucket = "refresh";
      } else {
        String method = request.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method) && !"OPTIONS".equals(method)) {
          // Mutations (payments, stock movements, inventory, masters) get a
          // stricter per-IP bucket than reads so bulk write abuse is contained
          // while bulk read flows (reports/attendance) stay unaffected.
          max = mutateMax;
          bucket = "mutate";
        }
      }
      if (!allow(remoteKey(request) + "|" + bucket, max)) {
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

  private static String remoteKey(HttpServletRequest request) {
    String remote = request.getRemoteAddr();
    return remote != null ? remote : "unknown";
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
