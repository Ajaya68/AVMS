package com.avms.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avms.security.PermEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Set;

  @SpringBootTest
  @ActiveProfiles("test")
  @Transactional
  class AuthServiceTest {

  @Autowired AuthService auth;
  @Autowired AccountsUserRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired SeedRolesService seeds;
  @Autowired com.avms.security.PasswordResetTokenRepository resetTokens;

  @BeforeEach
  void setUp() {
    seeds.seed();
    users.findByEmailIgnoreCase("staff@avms.local").orElseGet(() -> {
      AccountsUser u = new AccountsUser("staff@avms.local", encoder.encode("Staff@12345"));
      u.setFullName("Staff");
      return users.save(u);
    });
  }

  @Test
  void loginReturnsTokensAndUserThenRefreshRotates() {
    Map<String, Object> first = auth.login("staff@avms.local", "Staff@12345");
    assertThat(first).containsKeys("access", "refresh", "user");

    Map<String, Object> second = auth.refresh((String) first.get("refresh"));
    assertThat(second).containsKeys("access", "refresh");
    assertThat((String) second.get("refresh")).isNotEqualTo((String) first.get("refresh"));

    // Old refresh is now revoked
    assertThatThrownBy(() -> auth.refresh((String) first.get("refresh")))
        .isInstanceOf(Exception.class);
  }

  @Test
  void seedIsIdempotentAndAdminHasAllPermissions() {
    seeds.seed();
    seeds.seed();
    var admin = roles().findByCode("ADMIN").orElseThrow();
    assertThat(admin.permissionCodes()).contains("sales.manage", "users.manage", "audit.view");
    var employee = roles().findByCode("EMPLOYEE").orElseThrow();
    assertThat(employee.permissionCodes()).containsExactly("dashboard.view");
  }

  @Autowired
  RoleRepository rolesRepo;

  private RoleRepository roles() {
    return rolesRepo;
  }

  @Test
  void forgotPasswordAlwaysGenericAndNeverEchoesOtp() {
    Map<String, Object> unknown = auth.requestPasswordReset("nobody@avms.local");
    assertThat(unknown).containsKeys("detail").doesNotContainKey("otp");

    Map<String, Object> known = auth.requestPasswordReset("staff@avms.local");
    assertThat(known).containsKeys("detail").doesNotContainKey("otp");
  }

  /** The SMTP fallback logs the raw OTP on the console — tests capture it there. */
  private String otpFromLog(String email) {
    var logger = (ch.qos.logback.classic.Logger)
        org.slf4j.LoggerFactory.getLogger(com.avms.accounts.AuthService.class);
    var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      auth.requestPasswordReset(email);
      return appender.list.stream()
          .map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
          .filter(m -> m.contains("Password reset OTP for " + email))
          .map(m -> {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("([0-9]{6})").matcher(m);
            matcher.find();
            return matcher.group(1);
          })
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("OTP not logged"));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void resetPasswordRevokesSessionsIsSingleUseAndRejectsGarbage() {
    Map<String, Object> login = auth.login("staff@avms.local", "Staff@12345");
    String oldRefresh = (String) login.get("refresh");

    String otp = otpFromLog("staff@avms.local");

    // Garbage inputs fail with the same generic message (no oracle on which part failed)
    assertThatThrownBy(() -> auth.resetPassword("garbage!!", otp, "NewPass@12345"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid or expired reset code.");
    assertThatThrownBy(() -> auth.resetPassword("staff@avms.local", "000000", "NewPass@12345"))
        .isInstanceOf(IllegalArgumentException.class);

    Map<String, Object> done = auth.resetPassword("staff@avms.local", otp, "NewPass@12345");
    assertThat(done).containsKeys("detail");
    assertThat(encoder.matches("NewPass@12345",
        users.findByEmailIgnoreCase("staff@avms.local").orElseThrow().getPassword())).isTrue();

    // Reset revokes previously issued sessions
    assertThatThrownBy(() -> auth.refresh(oldRefresh)).isInstanceOf(Exception.class);

    // Single use: replaying the OTP fails even with the correct code
    assertThatThrownBy(() -> auth.resetPassword("staff@avms.local", otp, "Another@12345"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void expiredResetTokensAreRejectedAndPurged() {
    auth.requestPasswordReset("");
    String otp = otpFromLog("staff@avms.local");

    resetTokens.findAll().forEach(t -> t.setExpiresAt(java.time.Instant.now().minusSeconds(60)));
    resetTokens.saveAll(resetTokens.findAll());

    assertThatThrownBy(() -> auth.resetPassword("staff@avms.local", otp, "NewPass@12345"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid or expired reset code.");
    // Expired row consumed and removed; fresh requests start clean
    assertThat(resetTokens.findByUserEmailIgnoreCase("staff@avms.local")).isEmpty();
  }
}
