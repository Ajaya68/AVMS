package com.avms.ventures;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avms.accounts.AccountsUser;
import com.avms.accounts.AccountsUserRepository;
import com.avms.accounts.SeedRolesService;
import com.avms.security.JwtService;
import java.util.Set;
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

/** API-parity tests for /api/ventures (envelope, perms, codes, 401/403/404). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VentureControllerTest {

  @Autowired MockMvc mvc;
  @Autowired AccountsUserRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired SeedRolesService seeds;
  @Autowired JwtService jwt;

  String adminToken;
  String staffToken;

  @BeforeEach
  void setUp() {
    seeds.seed();
    AccountsUser admin = users.findByEmailIgnoreCase("admin@avms.local").orElseGet(() -> {
      AccountsUser u = new AccountsUser("admin@avms.local", encoder.encode("Admin@12345"));
      u.setIsSuperuser(true);
      u.setIsStaff(true);
      return users.save(u);
    });
    AccountsUser staff = users.findByEmailIgnoreCase("plain@avms.local").orElseGet(() ->
        users.save(new AccountsUser("plain@avms.local", encoder.encode("Plain@12345"))));
    adminToken = "Bearer " + jwt.generateAccess(admin.getEmail(), admin.permissionCodes(), true);
    staffToken = "Bearer " + jwt.generateAccess(staff.getEmail(), Set.of(), false);
  }

  @Test
  void unauthenticatedIs401() throws Exception {
    mvc.perform(get("/api/ventures")).andExpect(status().isUnauthorized());
  }

  @Test
  void withoutPermissionIs403() throws Exception {
    mvc.perform(get("/api/ventures").header("Authorization", staffToken)).andExpect(status().isForbidden());
  }

  @Test
  void fullCrudCycle() throws Exception {
    String body = "{\"venture_name\":\"Mushroom Unit\",\"business_type\":\"MUSHROOM\",\"city\":\"Pune\"}";
    String created = mvc.perform(post("/api/ventures").header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.venture_code", containsString("V-")))
        .andReturn().getResponse().getContentAsString();
    long id = ((Number) com.jayway.jsonpath.JsonPath.read(created, "$.data.id")).longValue();

    mvc.perform(get("/api/ventures").header("Authorization", adminToken).param("search", "mushroom"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.count").value(1))
        .andExpect(jsonPath("$.data.results[0].venture_code").exists());

    mvc.perform(get("/api/ventures/" + id).header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.venture_name").value("Mushroom Unit"));

    mvc.perform(get("/api/ventures/999999").header("Authorization", adminToken))
        .andExpect(status().isNotFound());

    mvc.perform(patch("/api/ventures/" + id).header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("INACTIVE"));

    mvc.perform(delete("/api/ventures/" + id).header("Authorization", adminToken))
        .andExpect(status().isOk());
  }
}
