package com.avms.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avms.accounts.AccountsUser;
import com.avms.accounts.AccountsUserRepository;
import com.avms.accounts.SeedRolesService;
import com.avms.products.Category;
import com.avms.products.CategoryRepository;
import com.avms.products.Product;
import com.avms.products.ProductRepository;
import com.avms.products.Unit;
import com.avms.products.UnitRepository;
import com.avms.security.JwtService;
import com.avms.ventures.Venture;
import com.avms.ventures.VentureRepository;
import java.math.BigDecimal;
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

/** API-parity tests for warehouses/stock/movements (deltas, guards, transfers). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InventoryControllerTest {

  @Autowired MockMvc mvc;
  @Autowired AccountsUserRepository users;
  @Autowired VentureRepository ventures;
  @Autowired UnitRepository unitRepo;
  @Autowired CategoryRepository categoryRepo;
  @Autowired ProductRepository productRepo;
  @Autowired PasswordEncoder encoder;
  @Autowired SeedRolesService seeds;
  @Autowired JwtService jwt;

  String adminToken;
  String ventureHeader;
  long warehouseId;
  long productId;

  @BeforeEach
  void setUp() throws Exception {
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
    long ventureId = ventures.save(venture).getId();
    ventureHeader = String.valueOf(ventureId);

    Unit unit = new Unit();
    unit.setUnitCode("kg-t");
    unit.setUnitName("Kilogram-Test");
    unitRepo.save(unit);
    Category category = new Category();
    category.setVenture(venture);
    category.setCategoryName("Seeds");
    category.setStatus("ACTIVE");
    categoryRepo.save(category);
    Product product = new Product();
    product.setVenture(venture);
    product.setCategory(category);
    product.setUnit(unit);
    product.setSku("P-0001");
    product.setProductName("Oyster Spawn");
    product.setReorderLevel(new BigDecimal("5"));
    product.setStatus("ACTIVE");
    productId = productRepo.save(product).getId();

    String warehouse = mvc.perform(post("/api/warehouses/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"venture\":" + ventureId + ",\"warehouse_name\":\"Main Godown\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.warehouse_code").value("W-0001"))
        .andReturn().getResponse().getContentAsString();
    warehouseId = ((Number) com.jayway.jsonpath.JsonPath.read(warehouse, "$.data.id")).longValue();
  }

  @Test
  void movementAppliesDeltasAndGuards() throws Exception {
    mvc.perform(post("/api/stock-movements/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"warehouse\":" + warehouseId + ",\"product\":" + productId
                + ",\"movement_type\":\"PURCHASE\",\"quantity\":50,\"movement_date\":\"2026-09-27\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.movement_type").value("PURCHASE"));

    String stock = mvc.perform(get("/api/inventory/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    org.assertj.core.api.Assertions.assertThat(
            new java.math.BigDecimal(
                com.jayway.jsonpath.JsonPath.read(stock, "$.data.results[0].quantity").toString()))
        .isEqualByComparingTo(new java.math.BigDecimal("50"));
    org.assertj.core.api.Assertions.assertThat(
            new java.math.BigDecimal(
                com.jayway.jsonpath.JsonPath.read(stock, "$.data.results[0].available").toString()))
        .isEqualByComparingTo(new java.math.BigDecimal("50"));

    // Insufficient stock is rejected.
    mvc.perform(post("/api/stock-movements/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"warehouse\":" + warehouseId + ",\"product\":" + productId
                + ",\"movement_type\":\"SALE\",\"quantity\":999,\"movement_date\":\"2026-09-27\"}"))
        .andExpect(status().isUnprocessableEntity());

    // Sale-out then low-stock filter.
    mvc.perform(post("/api/stock-movements/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"warehouse\":" + warehouseId + ",\"product\":" + productId
                + ",\"movement_type\":\"SALE\",\"quantity\":48,\"movement_date\":\"2026-09-27\"}"))
        .andExpect(status().isCreated());

    mvc.perform(get("/api/inventory/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader).param("low", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.count").value(1));
  }

  @Test
  void transferMovesStockBetweenWarehouses() throws Exception {
    String second = mvc.perform(post("/api/warehouses/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"warehouse_name\":\"Second Godown\"}"))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    long secondId = ((Number) com.jayway.jsonpath.JsonPath.read(second, "$.data.id")).longValue();

    mvc.perform(post("/api/stock-movements/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"warehouse\":" + warehouseId + ",\"product\":" + productId
                + ",\"movement_type\":\"PURCHASE\",\"quantity\":20,\"movement_date\":\"2026-09-27\"}"))
        .andExpect(status().isCreated());

    String transfer = mvc.perform(post("/api/stock-movements/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"warehouse\":" + warehouseId + ",\"destination_warehouse\":" + secondId
                + ",\"product\":" + productId
                + ",\"movement_type\":\"TRANSFER_OUT\",\"quantity\":8,\"movement_date\":\"2026-09-27\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.paired_movement").exists())
        .andReturn().getResponse().getContentAsString();
    long pairedId = ((Number) com.jayway.jsonpath.JsonPath.read(transfer, "$.data.paired_movement"))
        .longValue();

    mvc.perform(get("/api/stock-movements/" + pairedId + "/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.movement_type").value("TRANSFER_IN"));

    String destStock = mvc.perform(get("/api/inventory/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader).param("warehouse", String.valueOf(secondId)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    org.assertj.core.api.Assertions.assertThat(
            new java.math.BigDecimal(
                com.jayway.jsonpath.JsonPath.read(destStock, "$.data.results[0].quantity").toString()))
        .isEqualByComparingTo(new java.math.BigDecimal("8"));
  }
}
