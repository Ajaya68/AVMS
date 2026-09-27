package com.avms.purchases;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avms.accounts.AccountsUser;
import com.avms.accounts.AccountsUserRepository;
import com.avms.accounts.SeedRolesService;
import com.avms.inventory.Warehouse;
import com.avms.inventory.WarehouseRepository;
import com.avms.products.Product;
import com.avms.products.ProductRepository;
import com.avms.products.Unit;
import com.avms.products.UnitRepository;
import com.avms.security.JwtService;
import com.avms.suppliers.Supplier;
import com.avms.suppliers.SupplierRepository;
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

/** API-parity tests for purchases + returns (totals, stock postings, balances). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PurchaseControllerTest {

  @Autowired MockMvc mvc;
  @Autowired AccountsUserRepository users;
  @Autowired VentureRepository ventures;
  @Autowired SupplierRepository supplierRepo;
  @Autowired UnitRepository unitRepo;
  @Autowired ProductRepository productRepo;
  @Autowired WarehouseRepository warehouseRepo;
  @Autowired PasswordEncoder encoder;
  @Autowired SeedRolesService seeds;
  @Autowired JwtService jwt;

  String adminToken;
  String ventureHeader;
  long ventureId;
  long supplierId;
  long productId;
  long warehouseId;

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
    venture = ventures.save(venture);
    ventureId = venture.getId();
    ventureHeader = String.valueOf(ventureId);

    Supplier supplier = new Supplier();
    supplier.setVenture(venture);
    supplier.setSupplierCode("S-0001");
    supplier.setName("Agro Supply");
    supplier.setStatus("ACTIVE");
    supplierId = supplierRepo.save(supplier).getId();

    Unit unit = new Unit();
    unit.setUnitCode("kg-t");
    unit.setUnitName("Kilogram-Test");
    unitRepo.save(unit);
    Product product = new Product();
    product.setVenture(venture);
    product.setUnit(unit);
    product.setSku("P-0001");
    product.setProductName("Oyster Spawn");
    product.setPurchasePrice(new BigDecimal("100"));
    product.setStatus("ACTIVE");
    productId = productRepo.save(product).getId();

    Warehouse warehouse = new Warehouse();
    warehouse.setVenture(venture);
    warehouse.setWarehouseCode("W-0001");
    warehouse.setWarehouseName("Main Godown");
    warehouse.setStatus("ACTIVE");
    warehouseId = warehouseRepo.save(warehouse).getId();
  }

  @Test
  void billLifecycleWithStockAndReturn() throws Exception {
    String purchase = mvc.perform(post("/api/purchases/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"venture\":" + ventureId + ",\"supplier\":" + supplierId
                + ",\"warehouse\":" + warehouseId + ",\"purchase_date\":\"2026-09-27\","
                + "\"items\":[{\"product\":" + productId
                + ",\"quantity\":20,\"unit_price\":100}]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.invoice_number").value("PINV-0001"))
        .andExpect(jsonPath("$.data.total_amount", closeTo(2000.0, 0.01)))
        .andReturn().getResponse().getContentAsString();
    long purchaseId = ((Number) com.jayway.jsonpath.JsonPath.read(purchase, "$.data.id")).longValue();

    mvc.perform(post("/api/purchase-returns/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"venture\":" + ventureId + ",\"purchase\":" + purchaseId
                + ",\"return_date\":\"2026-09-27\","
                + "\"items\":[{\"product\":" + productId
                + ",\"quantity\":5,\"unit_price\":100}]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.return_number").value("RET-0001"))
        .andExpect(jsonPath("$.data.total_amount", closeTo(500.0, 0.01)));

    mvc.perform(get("/api/purchases/" + purchaseId + "/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PARTIAL"))
        .andExpect(jsonPath("$.data.returned_amount", closeTo(500.0, 0.01)))
        .andExpect(jsonPath("$.data.due_amount", closeTo(1500.0, 0.01)));

    String stock = mvc.perform(get("/api/inventory/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    org.assertj.core.api.Assertions.assertThat(
            new java.math.BigDecimal(
                com.jayway.jsonpath.JsonPath.read(stock, "$.data.results[0].quantity").toString()))
        .isEqualByComparingTo(new java.math.BigDecimal("15"));
  }
}
