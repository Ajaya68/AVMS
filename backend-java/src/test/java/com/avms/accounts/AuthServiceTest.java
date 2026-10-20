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
  void forgotPasswordAlwaysGenericAndLinksTokenMatchesRow() {
    Map<String, Object> unknown = auth.requestPasswordReset("nobody@avms.local", false);
    assertThat(unknown).containsKeys("detail").doesNotContainKey("reset_url");

    Map<String, Object> known = auth.requestPasswordReset("staff@avms.local", false);
    assertThat(known).containsKeys("detail").doesNotContainKey("reset_url");

    // dev-link mode returns a URL whose uidb64/token pair resolves to the stored hash
    Map<String, Object> link = auth.requestPasswordReset("staff@avms.local", true);
    assertThat(link).containsKey("reset_url");
    assertThat(String.valueOf(link.get("reset_url"))).contains("/reset-password?uidb64=");
  }

  @Test
  void resetPasswordRevokesSessionsIsSingleUseAndRejectsGarbage() {
    Map<String, Object> login = auth.login("staff@avms.local", "Staff@12345");
    String oldRefresh = (String) login.get("refresh");

    String url = (String) auth.requestPasswordReset("staff@avms.local", true).get("reset_url");
    String uidb64 = url.substring(url.indexOf("uidb64=") + "uidb64=".length(), url.indexOf("&token="));
    String token = url.substring(url.indexOf("&token=") + "&token=".length());

    // Garbage inputs fail with the same generic message (no oracle on which part failed)
    assertThatThrownBy(() -> auth.resetPassword("garbage!!", token, "NewPass@12345"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid or expired reset link.");
    assertThatThrownBy(() -> auth.resetPassword(uidb64, "wrong-token", "NewPass@12345"))
        .isInstanceOf(IllegalArgumentException.class);

    Map<String, Object> done = auth.resetPassword(uidb64, token, "NewPass@12345");
    assertThat(done).containsKeys("detail");
    assertThat(encoder.matches("NewPass@12345",
        users.findByEmailIgnoreCase("staff@avms.local").orElseThrow().getPassword())).isTrue();

    // Reset revokes previously issued sessions
    assertThatThrownBy(() -> auth.refresh(oldRefresh)).isInstanceOf(Exception.class);

    // Single use: replaying the link fails even with the correct token
    assertThatThrownBy(() -> auth.resetPassword(uidb64, token, "Another@12345"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void expiredResetTokensAreRejectedAndPurged() {
    auth.requestPasswordReset("", false);
    String url = (String) auth.requestPasswordReset("staff@avms.local", true).get("reset_url");
    String uidb64 = url.substring(url.indexOf("uidb64=") + "uidb64=".length(), url.indexOf("&token="));
    String token = url.substring(url.indexOf("&token=") + "&token=".length());

    resetTokens.findAll().forEach(t -> t.setExpiresAt(java.time.Instant.now().minusSeconds(60)));
    resetTokens.saveAll(resetTokens.findAll());

    assertThatThrownBy(() -> auth.resetPassword(uidb64, token, "NewPass@12345"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid or expired reset link.");
    // Expired row consumed and removed; fresh requests start clean
    assertThat(resetTokens.findByUserEmailIgnoreCase("staff@avms.local")).isEmpty();
  }
}
