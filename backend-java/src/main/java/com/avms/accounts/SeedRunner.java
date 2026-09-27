package com.avms.accounts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Runs the idempotent role/permission seed on every startup (mirrors `manage.py seed_roles`).
 * Optionally bootstraps a superuser when AVMS_ADMIN_EMAIL/PASSWORD are set
 * (mirrors `createsuperuser`; skipped when the user already exists or vars are unset).
 */
@Configuration
public class SeedRunner {

  @Bean
  public ApplicationRunner seedRoles(
      SeedRolesService seeds,
      AccountsUserRepository users,
      PasswordEncoder encoder,
      @Value("${AVMS_ADMIN_EMAIL:}") String adminEmail,
      @Value("${AVMS_ADMIN_PASSWORD:}") String adminPassword) {
    return args -> {
      seeds.seed();
      if (!adminEmail.isBlank() && !adminPassword.isBlank()
          && users.findByEmailIgnoreCase(adminEmail).isEmpty()) {
        AccountsUser admin = new AccountsUser(adminEmail, encoder.encode(adminPassword));
        admin.setFullName("Administrator");
        admin.setIsStaff(true);
        admin.setIsSuperuser(true);
        users.save(admin);
      }
    };
  }
}

