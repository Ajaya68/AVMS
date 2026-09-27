package com.avms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey key;
  private final long accessMinutes;
  private final long refreshDays;

  public JwtService(
      @Value("${avms.jwt.secret:change-me-to-a-256-bit-secret-please-change-me-123456}") String secret,
      @Value("${avms.jwt.access-minutes:30}") long accessMinutes,
      @Value("${avms.jwt.refresh-days:7}") long refreshDays) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalStateException("avms.jwt.secret must be at least 256 bits (32 chars)");
    }
    this.key = Keys.hmacShaKeyFor(bytes);
    this.accessMinutes = accessMinutes;
    this.refreshDays = refreshDays;
  }

  public String generateAccess(String email, Set<String> permissions, boolean superuser) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(email)
        .claim("permissions", permissions)
        .claim("superuser", superuser)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(accessMinutes * 60)))
        .signWith(key)
        .compact();
  }

  public String generateRefresh(String email, String jti) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(email)
        .id(jti)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(refreshDays * 86400)))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public long getRefreshDays() {
    return refreshDays;
  }
}
