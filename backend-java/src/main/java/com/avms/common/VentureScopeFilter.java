package com.avms.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Resolves X-Venture-Id header into VentureContextHolder (cleared after request). */
@Component
public class VentureScopeFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    try {
      String header = request.getHeader("X-Venture-Id");
      if (header != null && !header.isBlank()) {
        try {
          VentureContextHolder.set(Long.parseLong(header.trim()));
        } catch (NumberFormatException ignored) {
          VentureContextHolder.set(null);
        }
      }
      chain.doFilter(request, response);
    } finally {
      VentureContextHolder.clear();
    }
  }
}
