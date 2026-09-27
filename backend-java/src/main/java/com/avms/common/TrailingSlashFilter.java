package com.avms.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Strips trailing slashes (e.g. {@code /api/auth/login/} becomes
 * {@code /api/auth/login}) so the Django-style frontend URLs resolve against
 * Spring MVC's exact path matching. Registered ahead of Spring Security so the
 * permit rules also observe the normalized path. The root path {@code /} is
 * left untouched.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrailingSlashFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String uri = request.getRequestURI();
    if (uri != null && uri.length() > 1 && uri.endsWith("/")) {
      chain.doFilter(new TrailingSlashWrapper(request, uri.substring(0, uri.length() - 1)), response);
    } else {
      chain.doFilter(request, response);
    }
  }

  private static class TrailingSlashWrapper extends HttpServletRequestWrapper {
    private final String strippedUri;

    TrailingSlashWrapper(HttpServletRequest request, String strippedUri) {
      super(request);
      this.strippedUri = strippedUri;
    }

    @Override
    public String getRequestURI() {
      return strippedUri;
    }

    @Override
    public StringBuffer getRequestURL() {
      String originalUri = super.getRequestURI();
      StringBuffer url = super.getRequestURL();
      String urlString = url.toString();
      if (originalUri != null && urlString.endsWith(originalUri)) {
        return new StringBuffer(urlString.substring(0, urlString.length() - originalUri.length()) + strippedUri);
      }
      return url;
    }

    @Override
    public String getServletPath() {
      String path = super.getServletPath();
      if (path != null && path.length() > 1 && path.endsWith("/")) {
        return path.substring(0, path.length() - 1);
      }
      return path;
    }

    @Override
    public String getPathInfo() {
      String info = super.getPathInfo();
      if (info != null && info.length() > 1 && info.endsWith("/")) {
        return info.substring(0, info.length() - 1);
      }
      return info;
    }
  }
}
