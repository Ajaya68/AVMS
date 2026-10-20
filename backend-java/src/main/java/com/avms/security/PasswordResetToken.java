package com.avms.security;

import com.avms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import java.time.Instant;

/** Single-use password-reset token. Only the SHA-256 hash is stored; the
 *  raw token exists solely in the reset link shown to the user. Row is
 *  deleted on successful use, and expired/renamed tokens are purged when a
 *  new token is issued for the same email. */
@Entity
@Table(name = "PASSWORD_RESET_TOKEN")
public class PasswordResetToken extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_email", nullable = false, length = 254)
  private String userEmail;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  public PasswordResetToken() {}

  public PasswordResetToken(String userEmail, String tokenHash, Instant expiresAt) {
    this.userEmail = userEmail;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  public Long getId() { return id; }
  public String getUserEmail() { return userEmail; }
  public String getTokenHash() { return tokenHash; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public boolean isExpired() { return expiresAt.isBefore(Instant.now()); }
}
