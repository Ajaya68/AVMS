package com.avms.security;

import com.avms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "REFRESH_TOKEN")
public class RefreshToken extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "jti", nullable = false, unique = true, length = 64)
  private String jti;

  @Column(name = "user_email", nullable = false, length = 254)
  private String userEmail;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  public RefreshToken() {}

  public RefreshToken(String jti, String userEmail, Instant expiresAt) {
    this.jti = jti;
    this.userEmail = userEmail;
    this.expiresAt = expiresAt;
  }

  public Long getId() { return id; }
  public String getJti() { return jti; }
  public String getUserEmail() { return userEmail; }
  public Instant getExpiresAt() { return expiresAt; }
  public boolean isRevoked() { return revoked; }
  public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
