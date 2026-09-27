package com.avms.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        Claims claims = jwtService.parse(token);
        String email = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> perms = claims.get("permissions", List.class);
        Boolean superuser = claims.get("superuser", Boolean.class);
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        if (perms != null) {
          perms.forEach(p -> authorities.add(new SimpleGrantedAuthority("PERM_" + p)));
        }
        if (Boolean.TRUE.equals(superuser)) {
          authorities.add(new SimpleGrantedAuthority("ROLE_SUPERUSER"));
        }
        CustomUserDetails principal = new CustomUserDetails(email, Boolean.TRUE.equals(superuser), authorities);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (Exception ignored) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
