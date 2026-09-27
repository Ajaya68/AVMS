package com.avms.suppliers;

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

/** API-parity tests for /api/suppliers (codes, search, audited CRUD). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SupplierControllerTest {

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
    String body = "{\"venture\":" + ventureId + ",\"name\":\"Agro Supply\",\"contact_person\":\"Ravi\"}";
    String created = mvc.perform(post("/api/suppliers/").header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.supplier_code").value("S-0001"))
        .andExpect(jsonPath("$.data.contact_person").value("Ravi"))
        .andReturn().getResponse().getContentAsString();
    long id = ((Number) com.jayway.jsonpath.JsonPath.read(created, "$.data.id")).longValue();

    mvc.perform(get("/api/suppliers/").header("Authorization", adminToken)
            .header("X-Venture-Id", String.valueOf(ventureId)).param("search", "agro"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.count").value(1));

    mvc.perform(patch("/api/suppliers/" + id + "/").header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"payment_terms\":\"30 days\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.payment_terms").value("30 days"));

    mvc.perform(delete("/api/suppliers/" + id + "/").header("Authorization", adminToken))
        .andExpect(status().isOk());
  }
}
