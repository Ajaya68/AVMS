package com.avms.config;

import com.avms.common.VentureScopeFilter;
import com.avms.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Value("${avms.cors.allowed-origins:http://localhost:5173}")
  private String allowedOrigins;

  private final JwtAuthenticationFilter jwtFilter;
  private final VentureScopeFilter ventureScopeFilter;

  public SecurityConfig(JwtAuthenticationFilter jwtFilter, VentureScopeFilter ventureScopeFilter) {
    this.jwtFilter = jwtFilter;
    this.ventureScopeFilter = ventureScopeFilter;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .headers(headers -> headers
            .frameOptions(frame -> frame.deny())
            .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'none'"))
            .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
            .permissionsPolicyHeader(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()")))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          res.getWriter().write("{\"success\":false,\"message\":\"Authentication required.\"}");
                        })
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          res.getWriter().write("{\"success\":false,\"message\":\"You do not have permission to perform this action.\"}");
                        }))
        .authorizeHttpRequests(
            auth ->
                auth                    .requestMatchers(
                        "/health/**",
                        "/api/health/**",
                        "/api/auth/login",
                        "/api/auth/login/",
                        "/api/auth/refresh",
                        "/api/auth/refresh/",
                        "/api/auth/logout",
                        "/api/auth/logout/",
                        "/api/auth/forgot-password",
                        "/api/auth/forgot-password/",
                        "/api/auth/reset-password",
                        "/api/auth/reset-password/",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api/schema/**",
                        "/api/docs/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(ventureScopeFilter, JwtAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    List<String> origins = java.util.Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
    if (origins.contains("*")) {
      throw new IllegalStateException("CORS allowed-origins must not be '*' when allowCredentials is true; set explicit origins");
    }
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("authorization", "content-type", "x-venture-id"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }
}
