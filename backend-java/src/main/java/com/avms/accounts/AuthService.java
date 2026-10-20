package com.avms.accounts;

import com.avms.audit.AuditService;
import com.avms.common.ResourceNotFoundException;
import com.avms.security.JwtService;
import com.avms.security.PasswordResetToken;
import com.avms.security.PasswordResetTokenRepository;
import com.avms.security.RefreshToken;
import com.avms.security.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Port of Django accounts.views: login/refresh/logout/me, password reset/change.
 * Passwords: BCrypt primary; legacy Django PBKDF2 ($pbkdf2-sha256$) verified then re-hashed.
 */
@Service
public class AuthService {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(AuthService.class);

  /** Refresh-token lifetime cap for the whole reset-change-rotation chain. */
  static final long RESET_TOKEN_MINUTES = 30;
  static final String GENERIC_RESET_MESSAGE = "If an account exists for that email, reset instructions have been issued.";
  private static final String RESET_URL_PATH = "/reset-password";
  /** 2^232+ random value, hashed before storage; raw form never persisted. */
  private static final int RESET_TOKEN_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();
  /** OTP: 6 digits, 10-minute lifetime, single use. Console delivery is the
   *  deliberate local-recovery channel — no mail server exists. */
  private static final int OTP_DIGITS = 6;
  private static final int OTP_TTL_MINUTES = 10;
  /** Never matched against a real account; equalizes BCrypt work for unknown
   *  emails so response timing cannot reveal whether an email exists. */
  private static final String DUMMY_BCRYPT_HASH =
      new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());

  private final AccountsUserRepository users;
  private final RoleRepository roles;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordResetTokenRepository resetTokens;
  private final JwtService jwt;
  private final PasswordEncoder encoder;
  private final AuditService audit;
  private final boolean exposeResetLink;
  private final String firstFrontendOrigin;

  public AuthService(AccountsUserRepository users, RoleRepository roles, RefreshTokenRepository refreshTokens,
      PasswordResetTokenRepository resetTokens, JwtService jwt, PasswordEncoder encoder, AuditService audit,
      @Value("${avms.auth.expose-reset-link:false}") boolean exposeResetLink,
      @Value("${avms.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
    this.users = users;
    this.roles = roles;
    this.refreshTokens = refreshTokens;
    this.resetTokens = resetTokens;
    this.jwt = jwt;
    this.encoder = encoder;
    this.audit = audit;
    this.exposeResetLink = exposeResetLink;
    this.firstFrontendOrigin = java.util.Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty() && !s.contains("*"))
        .findFirst()
        .orElse("http://localhost:5173");
  }

  @Transactional
  public Map<String, Object> login(String email, String password) {
    // Uniform no-match message; unknown emails still pay one BCrypt round so
    // response timing cannot distinguish them from valid accounts.
    AccountsUser user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
    if (user == null) {
      encoder.matches(password, DUMMY_BCRYPT_HASH);
      throw new ResourceNotFoundException("Invalid email or password.");
    }
    if (!matches(password, user.getPassword())) {
      throw new ResourceNotFoundException("Invalid email or password.");
    }
    if (!Boolean.TRUE.equals(user.getIsActive())) {
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
    // A stolen refresh token must not survive a password change (matches the
    // reset flow). Access JWTs stay valid until their ≤30-min expiry, which is
    // the documented stateless tradeoff.
    refreshTokens.findByUserEmailIgnoreCase(user.getEmail())
        .forEach(t -> t.setRevoked(true));
    audit.log("PASSWORD_CHANGED", "accounts", "User", String.valueOf(user.getId()),
        "Password changed; issued sessions revoked.");
  }

  @Transactional(readOnly = true)
  public AuthDtos.UserResponse me(String email) {
    AccountsUser user = users.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    return toUserResponse(user);
  }

  /**
   * Issues a single-use 6-digit OTP (10-minute lifetime), SHA-256-hashed in
   * storage. Delivered on the server console (no mail server exists) and, in
   * dev only, echoed in the response. Responses are always generic to prevent
   * user enumeration.
   */
  @Transactional
  public Map<String, Object> requestPasswordReset(String email, boolean allowLink) {
    Map<String, Object> out = new HashMap<>();
    out.put("detail", GENERIC_RESET_MESSAGE);
    if (exposeResetLink) {
      allowLink = true;
    }
    String normalized = email == null ? "" : email.trim();
    AccountsUser user = users.findByEmailIgnoreCase(normalized).orElse(null);
    if (user != null && Boolean.TRUE.equals(user.getIsActive())) {
      // One outstanding credential per email: purge expired/used predecessors.
      resetTokens.deleteByUserEmailIgnoreCase(user.getEmail());
      String otp = newOtp();
      resetTokens.save(new PasswordResetToken(user.getEmail(), hashToken(otp),
          Instant.now().plusSeconds(OTP_TTL_MINUTES * 60)));
      // Delivery channel: server console always takes it (local recovery); dev
      // additionally echoes the code in the response for convenience.
      log.info("[forgot-password] Password reset OTP for {}: {} (valid {} min)",
          user.getEmail(), otp, OTP_TTL_MINUTES);
      if (allowLink) {
        out.put("otp", otp);
        String uidb64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(String.valueOf(user.getId()).getBytes(StandardCharsets.US_ASCII));
        out.put("reset_url", firstFrontendOrigin + RESET_URL_PATH + "?uidb64=" + uidb64);
      }
      audit.log("PASSWORD_RESET_REQUESTED", "accounts", "User", String.valueOf(user.getId()),
          "Password reset requested (OTP issued).");
    }
    return out;
  }

  private String newOtp() {
    StringBuilder sb = new StringBuilder(OTP_DIGITS);
    for (int i = 0; i < OTP_DIGITS; i++) {
      sb.append(RANDOM.nextInt(10));
    }
    return sb.toString();
  }

  private String hashToken(String rawToken) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  /** Consumes the single-use OTP: verifies the 6-digit code against the
   *  stored hash, swaps the password, revokes every session and audits.
   *  Failures return one identical message so account state is never leaked. */
  @Transactional
  public Map<String, Object> resetPassword(String email, String otp, String newPassword) {
    String normalized = email == null ? "" : email.trim();
    String normalizedOtp = otp == null ? "" : otp.trim();
    if (normalized.isEmpty() || normalizedOtp.length() != OTP_DIGITS
        || normalizedOtp.chars().anyMatch(c -> c < '0' || c > '9')) {
      throw new IllegalArgumentException("Invalid or expired reset code.");
    }
    PasswordResetToken stored = resetTokens.findByUserEmailIgnoreCase(normalized).stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset code."));
    boolean valid = MessageDigest.isEqual(
        hashToken(normalizedOtp).getBytes(StandardCharsets.US_ASCII),
        stored.getTokenHash().getBytes(StandardCharsets.US_ASCII));
    if (!valid || stored.isExpired()) {
      // Wrong code / expired code: one identical rejection. The row stays for
      // expired-at-request cleanup by the next issuance; deletion on valid use only.
      if (stored.isExpired()) {
        resetTokens.delete(stored);
      }
      throw new IllegalArgumentException("Invalid or expired reset code.");
    }
    AccountsUser user = users.findByEmailIgnoreCase(normalized)
        .filter(u -> u.getEmail().equalsIgnoreCase(stored.getUserEmail()))
        .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset code."));
    user.setPassword(encoder.encode(newPassword));
    users.save(user);
    resetTokens.delete(stored);
    // Any session using old credentials dies immediately.
    refreshTokens.findByUserEmailIgnoreCase(user.getEmail())
        .forEach(t -> t.setRevoked(true));
    audit.log("PASSWORD_RESET", "accounts", "User", String.valueOf(user.getId()),
        "Password reset completed via OTP.");
    return Map.of("detail", "Password has been reset. You can now sign in with the new password.");
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
