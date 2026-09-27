package com.avms.products;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** API-parity tests for units/categories/products (codes, uniqueness, guards). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductsControllerTest {

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
  void mastersCycle() throws Exception {
    String unit = mvc.perform(post("/api/units/").header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"unit_code\":\"bag\",\"unit_name\":\"Bag\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.unit_code").value("bag"))
        .andReturn().getResponse().getContentAsString();
    long unitId = ((Number) com.jayway.jsonpath.JsonPath.read(unit, "$.data.id")).longValue();

    mvc.perform(post("/api/units/").header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"unit_code\":\"bag\",\"unit_name\":\"Duplicate\"}"))
        .andExpect(status().is4xxClientError());

    String category = mvc.perform(post("/api/categories/").header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"venture\":" + ventureId + ",\"category_name\":\"Seeds\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.venture_name").value("V-T1"))
        .andReturn().getResponse().getContentAsString();
    long categoryId = ((Number) com.jayway.jsonpath.JsonPath.read(category, "$.data.id")).longValue();

    String product = mvc.perform(post("/api/products/").header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"venture\":" + ventureId + ",\"category\":" + categoryId
                + ",\"unit\":" + unitId
                + ",\"product_name\":\"Oyster Spawn\",\"purchase_price\":100,\"selling_price\":150}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.sku").value("P-0001"))
        .andExpect(jsonPath("$.data.category_name").value("Seeds"))
        .andExpect(jsonPath("$.data.unit_code").value("bag"))
        .andReturn().getResponse().getContentAsString();
    long productId = ((Number) com.jayway.jsonpath.JsonPath.read(product, "$.data.id")).longValue();

    mvc.perform(get("/api/products/").header("Authorization", adminToken)
            .header("X-Venture-Id", String.valueOf(ventureId)).param("search", "oyster"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.count").value(1));

    // Unit in use cannot be deleted.
    mvc.perform(delete("/api/units/" + unitId + "/").header("Authorization", adminToken))
        .andExpect(status().isUnprocessableEntity());

    mvc.perform(delete("/api/products/" + productId + "/").header("Authorization", adminToken))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/categories/" + categoryId + "/").header("Authorization", adminToken))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/units/" + unitId + "/").header("Authorization", adminToken))
        .andExpect(status().isOk());
  }
}
