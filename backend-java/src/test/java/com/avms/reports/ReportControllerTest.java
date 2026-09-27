package com.avms.reports;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avms.accounts.AccountsUser;
import com.avms.accounts.AccountsUserRepository;
import com.avms.accounts.SeedRolesService;
import com.avms.customers.Customer;
import com.avms.customers.CustomerRepository;
import com.avms.inventory.Warehouse;
import com.avms.inventory.WarehouseRepository;
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

/** API-parity tests for /api/reports (summaries, series, valuation, financials). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportControllerTest {

  @Autowired MockMvc mvc;
  @Autowired AccountsUserRepository users;
  @Autowired VentureRepository ventures;
  @Autowired CustomerRepository customerRepo;
  @Autowired UnitRepository unitRepo;
  @Autowired ProductRepository productRepo;
  @Autowired WarehouseRepository warehouseRepo;
  @Autowired PasswordEncoder encoder;
  @Autowired SeedRolesService seeds;
  @Autowired JwtService jwt;

  String adminToken;
  String ventureHeader;
  long ventureId;
  long customerId;
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

    Customer customer = new Customer();
    customer.setVenture(venture);
    customer.setCustomerCode("C-0001");
    customer.setName("Green Stores");
    customer.setStatus("ACTIVE");
    customerId = customerRepo.save(customer).getId();
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
    product.setSellingPrice(new BigDecimal("150"));
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
  void reportsReflectBillsAndStock() throws Exception {
    mvc.perform(post("/api/stock-movements/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"warehouse\":" + warehouseId + ",\"product\":" + productId
                + ",\"movement_type\":\"PURCHASE\",\"quantity\":50,\"movement_date\":\"2026-09-27\"}"))
        .andExpect(status().isCreated());

    mvc.perform(post("/api/sales/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"venture\":" + ventureId + ",\"customer\":" + customerId
                + ",\"warehouse\":" + warehouseId + ",\"sale_date\":\"2026-09-27\","
                + "\"items\":[{\"product\":" + productId
                + ",\"quantity\":10,\"unit_price\":150}]}"))
        .andExpect(status().isCreated());

    mvc.perform(get("/api/reports/sales/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.summary.count").value(1))
        .andExpect(jsonPath("$.data.summary.net_amount", closeTo(1500.0, 0.01)))
        .andExpect(jsonPath("$.data.top_products[0].product_code").value("P-0001"))
        .andExpect(jsonPath("$.data.series.length()").value(30));

    mvc.perform(get("/api/reports/inventory/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.summary.stock_value", closeTo(4000.0, 0.01)))
        .andExpect(jsonPath("$.data.items[0].valuation", closeTo(4000.0, 0.01)));

    mvc.perform(get("/api/reports/financial/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.summary.revenue", closeTo(1500.0, 0.01)))
        .andExpect(jsonPath("$.data.summary.cogs", closeTo(1000.0, 0.01)))
        .andExpect(jsonPath("$.data.outstanding.receivables", closeTo(1500.0, 0.01)));

    mvc.perform(get("/api/reports/purchases/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.summary.count").value(0));
  }
}
