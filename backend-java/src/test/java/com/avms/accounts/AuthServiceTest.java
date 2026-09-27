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
}
