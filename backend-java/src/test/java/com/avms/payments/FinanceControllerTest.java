package com.avms.payments;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avms.accounts.AccountsUser;
import com.avms.accounts.AccountsUserRepository;
import com.avms.accounts.SeedRolesService;
import com.avms.customers.Customer;
import com.avms.customers.CustomerRepository;
import com.avms.expenses.ExpenseCategoryRepository;
import com.avms.products.Product;
import com.avms.products.ProductRepository;
import com.avms.products.Unit;
import com.avms.products.UnitRepository;
import com.avms.sales.Sale;
import com.avms.sales.SaleRepository;
import com.avms.security.JwtService;
import com.avms.ventures.Venture;
import com.avms.ventures.VentureRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/** API-parity tests for payments (caps, apply, reverse) + expenses (categories, CRUD). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FinanceControllerTest {

  @Autowired MockMvc mvc;
  @Autowired AccountsUserRepository users;
  @Autowired VentureRepository ventures;
  @Autowired CustomerRepository customerRepo;
  @Autowired UnitRepository unitRepo;
  @Autowired ProductRepository productRepo;
  @Autowired SaleRepository saleRepo;
  @Autowired ExpenseCategoryRepository categoryRepo;
  @Autowired PasswordEncoder encoder;
  @Autowired SeedRolesService seeds;
  @Autowired JwtService jwt;

  String adminToken;
  String ventureHeader;
  long ventureId;
  long saleId;

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
    customerRepo.save(customer);
    Unit unit = new Unit();
    unit.setUnitCode("kg-t");
    unit.setUnitName("Kilogram-Test");
    unitRepo.save(unit);
    Product product = new Product();
    product.setVenture(venture);
    product.setUnit(unit);
    product.setSku("P-0001");
    product.setProductName("Oyster Spawn");
    product.setStatus("ACTIVE");
    productRepo.save(product);

    Sale sale = new Sale();
    sale.setVenture(venture);
    sale.setCustomer(customer);
    sale.setInvoiceNumber("SINV-0001");
    sale.setSaleDate(LocalDate.of(2026, 9, 27));
    sale.setStatus("COMPLETED");
    sale.setSubtotal(new BigDecimal("1500"));
    sale.setTotalAmount(new BigDecimal("1500"));
    sale.setPaidAmount(BigDecimal.ZERO);
    sale.setReturnedAmount(BigDecimal.ZERO);
    sale.setDueAmount(new BigDecimal("1500"));
    saleId = saleRepo.save(sale).getId();
  }

  @Test
  void paymentCapsApplyAndReverse() throws Exception {
    mvc.perform(post("/api/payments/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"venture\":" + ventureId + ",\"payment_type\":\"RECEIVED\","
                + "\"reference_type\":\"SALE\",\"reference_id\":" + saleId
                + ",\"amount\":9999,\"payment_date\":\"2026-09-27\"}"))
        .andExpect(status().isBadRequest());

    String payment = mvc.perform(post("/api/payments/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"venture\":" + ventureId + ",\"payment_type\":\"RECEIVED\","
                + "\"reference_type\":\"SALE\",\"reference_id\":" + saleId
                + ",\"amount\":500,\"payment_date\":\"2026-09-27\",\"payment_method\":\"UPI\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.reference_number").value("SINV-0001"))
        .andExpect(jsonPath("$.data.amount", closeTo(500.0, 0.01)))
        .andReturn().getResponse().getContentAsString();
    long paymentId = ((Number) com.jayway.jsonpath.JsonPath.read(payment, "$.data.id")).longValue();

    mvc.perform(get("/api/payments/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader).param("payment_type", "RECEIVED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.count").value(1));

    mvc.perform(delete("/api/payments/" + paymentId + "/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk());

    Sale reloaded = saleRepo.findById(saleId).orElseThrow();
    org.assertj.core.api.Assertions.assertThat(reloaded.getPaidAmount())
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void expenseCategoriesAndCrud() throws Exception {
    String categories = mvc.perform(get("/api/expense-categories/").header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    java.util.List<?> list = com.jayway.jsonpath.JsonPath.read(categories, "$.data");
    org.assertj.core.api.Assertions.assertThat(list).isNotEmpty();

    String expense = mvc.perform(post("/api/expenses/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"venture\":" + ventureId + ",\"category\":3,\"amount\":5000,"
                + "\"expense_date\":\"2026-09-27\",\"payment_method\":\"CASH\","
                + "\"description\":\"Godown rent\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.category_name").value("Rent"))
        .andExpect(jsonPath("$.data.amount", closeTo(5000.0, 0.01)))
        .andReturn().getResponse().getContentAsString();
    long expenseId = ((Number) com.jayway.jsonpath.JsonPath.read(expense, "$.data.id")).longValue();

    mvc.perform(get("/api/expenses/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader).param("search", "godown"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.count").value(1));

    mvc.perform(delete("/api/expenses/" + expenseId + "/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk());
  }
}
