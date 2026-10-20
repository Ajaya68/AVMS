package com.avms.accounts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avms.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** API-parity tests for /api/auth (login/me/users guards, envelope, 401/403). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

  @Autowired MockMvc mvc;
  @Autowired AccountsUserRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired SeedRolesService seeds;
  @Autowired JwtService jwt;

  @BeforeEach
  void setUp() {
    seeds.seed();
    users.findByEmailIgnoreCase("admin@avms.local").orElseGet(() -> {
      AccountsUser u = new AccountsUser("admin@avms.local", encoder.encode("Admin@12345"));
      u.setIsSuperuser(true);
      u.setIsStaff(true);
      return users.save(u);
    });
  }

  @Test
  void loginMeAndGuards() throws Exception {
    String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin@avms.local\",\"password\":\"Admin@12345\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.access").exists())
        .andExpect(jsonPath("$.data.refresh").exists())
        .andReturn().getResponse().getContentAsString();
    String access = com.jayway.jsonpath.JsonPath.read(login, "$.data.access");
    String refresh = com.jayway.jsonpath.JsonPath.read(login, "$.data.refresh");
    long adminId = ((Number) com.jayway.jsonpath.JsonPath.read(login, "$.data.user.id")).longValue();

    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value("admin@avms.local"));

    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin@avms.local\",\"password\":\"wrong\"}"))
        .andExpect(status().is4xxClientError());

    // Refresh rotation
    String refreshed = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
            .content("{\"refresh\":\"" + refresh + "\"}"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    String refresh2 = com.jayway.jsonpath.JsonPath.read(refreshed, "$.data.refresh");

    // Old refresh revoked
    mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
            .content("{\"refresh\":\"" + refresh + "\"}"))
        .andExpect(status().is4xxClientError());

    // Superuser cannot be deleted; self-delete blocked
    mvc.perform(delete("/api/auth/users/" + adminId).header("Authorization", "Bearer " + access))
        .andExpect(status().isForbidden());

    // Logout with rotated token
    mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + access)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refresh\":\"" + refresh2 + "\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void forgotPasswordAlwaysGenericAndResetRejectsGarbage() throws Exception {
    // Unknown email: same 200 + generic body as a valid one (no enumeration)
    mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"ghost@nowhere.example\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.detail").exists())
        .andExpect(jsonPath("$.data.otp").doesNotExist());

    // Reset with garbage params: fixed generic 400, no internals echoed
    mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"who@x.y\",\"otp\":\"abc\",\"new_password\":\"whatever@123\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Invalid or expired reset code."));

    // Missing/short new password fails validation (generic envelope)
    mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"who@x.y\",\"otp\":\"123456\",\"new_password\":\"short\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }
}
