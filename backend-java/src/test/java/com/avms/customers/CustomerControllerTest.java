package com.avms.customers;

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
import com.avms.ventures.Venture;
import com.avms.ventures.VentureRepository;
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

/** API-parity tests for /api/customers (codes, search, scoping, audited CRUD). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerControllerTest {

  @Autowired MockMvc mvc;
  @Autowired AccountsUserRepository users;
  @Autowired VentureRepository ventures;
  @Autowired PasswordEncoder encoder;
  @Autowired SeedRolesService seeds;
  @Autowired JwtService jwt;

  String adminToken;
  long ventureId;

  @BeforeEach
  void setUp() {
    seeds.seed();
    AccountsUser admin = users.findByEmailIgnoreCase("admin@avms.local").orElseGet(() -> {
      AccountsUser u = new AccountsUser("admin@avms.local", encoder.encode("Admin@12345"));
      u.setIsSuperuser(true);
      u.setIsStaff(true);
      return users.save(u);
    });
    adminToken = "Bearer " + jwt.generateAccess(admin.getEmail(), admin.permissionCodes(), true);
    Venture venture = new Venture();
    venture.setVentureCode("V-T1");
    venture.setVentureName("Test Venture");
    venture.setStatus("ACTIVE");
    ventureId = ventures.save(venture).getId();
  }

  @Test
  void fullCrudCycle() throws Exception {
    String body = "{\"venture\":" + ventureId + ",\"name\":\"Green Stores\",\"city\":\"Pune\"}";
    String created = mvc.perform(post("/api/customers/").header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.customer_code").value("C-0001"))
        .andExpect(jsonPath("$.data.venture_name").value("V-T1"))
        .andReturn().getResponse().getContentAsString();
    long id = ((Number) com.jayway.jsonpath.JsonPath.read(created, "$.data.id")).longValue();

    mvc.perform(get("/api/customers/").header("Authorization", adminToken)
            .header("X-Venture-Id", String.valueOf(ventureId)).param("search", "green"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.count").value(1));

    mvc.perform(get("/api/customers/" + id + "/").header("Authorization", adminToken)
            .header("X-Venture-Id", String.valueOf(ventureId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("Green Stores"));

    mvc.perform(get("/api/customers/" + id + "/").header("Authorization", adminToken)
            .header("X-Venture-Id", "999999"))
        .andExpect(status().isNotFound());

    mvc.perform(patch("/api/customers/" + id + "/").header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"9876543210\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.phone").value("9876543210"));

    mvc.perform(delete("/api/customers/" + id + "/").header("Authorization", adminToken))
        .andExpect(status().isOk());

    mvc.perform(get("/api/customers/" + id + "/").header("Authorization", adminToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void createRequiresName() throws Exception {
    mvc.perform(post("/api/customers/").header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"venture\":" + ventureId + "}"))
        .andExpect(status().is4xxClientError());
  }
}
