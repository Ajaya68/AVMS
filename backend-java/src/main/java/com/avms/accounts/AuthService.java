package com.avms.accounts;

import com.avms.audit.AuditService;
import com.avms.common.ResourceNotFoundException;
import com.avms.security.JwtService;
import com.avms.security.RefreshToken;
import com.avms.security.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Port of Django accounts.views: login/refresh/logout/me, password reset/change.
 * Passwords: BCrypt primary; legacy Django PBKDF2 ($pbkdf2-sha256$) verified then re-hashed.
 */
@Service
public class AuthService {

  private final AccountsUserRepository users;
  private final RoleRepository roles;
  private final RefreshTokenRepository refreshTokens;
  private final JwtService jwt;
  private final PasswordEncoder encoder;
  private final AuditService audit;

  public AuthService(AccountsUserRepository users, RoleRepository roles, RefreshTokenRepository refreshTokens,
      JwtService jwt, PasswordEncoder encoder, AuditService audit) {
    this.users = users;
    this.roles = roles;
    this.refreshTokens = refreshTokens;
    this.jwt = jwt;
    this.encoder = encoder;
    this.audit = audit;
  }

  @Transactional
  public Map<String, Object> login(String email, String password) {
    AccountsUser user = users.findByEmailIgnoreCase(email.trim())
        .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password."));
    if (!Boolean.TRUE.equals(user.getIsActive())) {
      throw new ResourceNotFoundException("Invalid email or password.");
    }
    if (!matches(password, user.getPassword())) {
      throw new ResourceNotFoundException("Invalid email or password.");
    }
    if (isLegacyHash(user.getPassword())) {
      user.setPassword(encoder.encode(password));
      users.save(user);
    }
    user.setLastLogin(Instant.now());
    users.save(user);
    Set<String> perms = user.permissionCodes();
    String access = jwt.generateAccess(user.getEmail(), perms, Boolean.TRUE.equals(user.getIsSuperuser()));
    String jti = UUID.randomUUID().toString().replace("-", "");
    String refresh = jwt.generateRefresh(user.getEmail(), jti);
    refreshTokens.save(new RefreshToken(jti, user.getEmail(), Instant.now().plusSeconds(jwt.getRefreshDays() * 86400)));
    audit.log("LOGIN", "accounts", "User " + user.getEmail() + " logged in.");
    return Map.of("access", access, "refresh", refresh, "user", toUserResponse(user));
  }

  @Transactional
  public Map<String, Object> refresh(String refreshToken) {
    var claims = jwt.parse(refreshToken);
    String jti = claims.getId();
    RefreshToken stored = refreshTokens.findByJti(jti)
        .orElseThrow(() -> new ResourceNotFoundException("Invalid refresh token."));
    if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
      // Possible token replay (stolen refresh reused after rotation/expiry):
      // revoke all sessions for this user so the attacker-held tokens die too.
      revokeAllForUser(stored.getUserEmail());
      throw new ResourceNotFoundException("Invalid refresh token.");
    }
    AccountsUser user = users.findByEmailIgnoreCase(stored.getUserEmail())
        .orElseThrow(() -> new ResourceNotFoundException("Invalid refresh token."));
    stored.setRevoked(true);
    refreshTokens.save(stored);
    Set<String> perms = user.permissionCodes();
    String access = jwt.generateAccess(user.getEmail(), perms, Boolean.TRUE.equals(user.getIsSuperuser()));
    String newJti = UUID.randomUUID().toString().replace("-", "");
    String refresh = jwt.generateRefresh(user.getEmail(), newJti);
    refreshTokens.save(new RefreshToken(newJti, user.getEmail(), Instant.now().plusSeconds(jwt.getRefreshDays() * 86400)));
    return Map.of("access", access, "refresh", refresh);
  }

  @Transactional
  public void logout(String refreshToken) {
    try {
      var claims = jwt.parse(refreshToken);
      refreshTokens.findByJti(claims.getId()).ifPresent(t -> {
        t.setRevoked(true);
        refreshTokens.save(t);
      });
    } catch (Exception ignored) {
    }
    audit.log("LOGOUT", "accounts", "User logged out.");
  }

  @Transactional
  public void changePassword(String email, String currentPassword, String newPassword) {
    AccountsUser user = users.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    if (!matches(currentPassword, user.getPassword())) {
      throw new IllegalArgumentException("Current password is incorrect.");
    }
    user.setPassword(encoder.encode(newPassword));
    users.save(user);
    audit.log("PASSWORD_CHANGED", "accounts", "User", String.valueOf(user.getId()), "Password changed.");
  }

  @Transactional(readOnly = true)
  public AuthDtos.UserResponse me(String email) {
    AccountsUser user = users.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    return toUserResponse(user);
  }

  public static AuthDtos.UserResponse toUserResponse(AccountsUser user) {
    return new AuthDtos.UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
        Boolean.TRUE.equals(user.getIsActive()), Boolean.TRUE.equals(user.getIsStaff()),
        Boolean.TRUE.equals(user.getIsSuperuser()), user.roleCodes(), user.permissionCodes());
  }

  private void revokeAllForUser(String email) {
    try {
      var tokens = refreshTokens.findByUserEmailIgnoreCase(email);
      tokens.forEach(t -> t.setRevoked(true));
      refreshTokens.saveAll(tokens);
    } catch (Exception ignored) {
    }
  }

  private boolean matches(String raw, String hash) {    if (hash != null && hash.startsWith("pbkdf2_sha256$")) {
      return verifyPbkdf2(raw, hash);
    }
    try {
      return encoder.matches(raw, hash);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isLegacyHash(String hash) {
    return hash != null && hash.startsWith("pbkdf2_sha256$");
  }

  /** Verifies Django-style pbkdf2_sha256$iterations$salt$base64hash. */
  static boolean verifyPbkdf2(String raw, String hash) {
    try {
      String[] parts = hash.split("\\$");
      int iterations = Integer.parseInt(parts[1]);
      byte[] salt = parts[2].getBytes(StandardCharsets.UTF_8);
      byte[] expected = Base64.getDecoder().decode(parts[3]);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] derived = pbkdf2(raw.getBytes(StandardCharsets.UTF_8), salt, iterations, expected.length, digest);
      return MessageDigest.isEqual(derived, expected);
    } catch (Exception e) {
      return false;
    }
  }

  private static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int dkLen, MessageDigest digest) throws Exception {
    int hLen = digest.getDigestLength();
    int blocks = (int) Math.ceil((double) dkLen / hLen);
    byte[] out = new byte[blocks * hLen];
    for (int i = 1; i <= blocks; i++) {
      byte[] intBytes = {(byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) i};
      byte[] u = hmac(password, concat(salt, intBytes), digest);
      byte[] t = u.clone();
      for (int j = 1; j < iterations; j++) {
        u = hmac(password, u, digest);
        for (int k = 0; k < t.length; k++) {
          t[k] ^= u[k];
        }
      }
      System.arraycopy(t, 0, out, (i - 1) * hLen, hLen);
    }
    byte[] dk = new byte[dkLen];
    System.arraycopy(out, 0, dk, 0, dkLen);
    return dk;
  }

  private static byte[] hmac(byte[] key, byte[] data, MessageDigest digest) throws Exception {
    int blockSize = 64;
    if (key.length > blockSize) {
      key = digest.digest(key);
    }
    byte[] padded = new byte[blockSize];
    System.arraycopy(key, 0, padded, 0, key.length);
    byte[] ipad = new byte[blockSize];
    byte[] opad = new byte[blockSize];
    for (int i = 0; i < blockSize; i++) {
      ipad[i] = (byte) (padded[i] ^ 0x36);
      opad[i] = (byte) (padded[i] ^ 0x5c);
    }
    digest.reset();
    digest.update(ipad);
    digest.update(data);
    byte[] inner = digest.digest();
    digest.reset();
    digest.update(opad);
    digest.update(inner);
    return digest.digest();
  }

  private static byte[] concat(byte[] a, byte[] b) {
    byte[] out = new byte[a.length + b.length];
    System.arraycopy(a, 0, out, 0, a.length);
    System.arraycopy(b, 0, out, a.length, b.length);
    return out;
  }
}
