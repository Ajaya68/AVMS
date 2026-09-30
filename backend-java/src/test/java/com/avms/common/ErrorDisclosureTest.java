package com.avms.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/**
 * Production error-disclosure regression tests: framework and routing failures
 * must return the fixed generic envelope with no stack traces, SQL, paths,
 * exception class names, or diagnostics.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ErrorDisclosureTest {

  @Autowired MockMvc mvc;
  @Autowired AccountsUserRepository users;
  @Autowired VentureRepository ventures;
  @Autowired PasswordEncoder encoder;
  @Autowired SeedRolesService seeds;
  @Autowired JwtService jwt;

  String adminToken;
  String ventureHeader;

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
    venture.setVentureCode("V-ERR");
    venture.setVentureName("Error Disclosure Venture");
    venture.setStatus("ACTIVE");
    ventureHeader = String.valueOf(ventures.save(venture).getId());
  }

  @Test
  void malformedJsonReturnsGeneric400() throws Exception {
    mvc.perform(post("/api/sales/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{bad json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Malformed request."))
        .andExpect(content().string(not(containsString("stack"))))
        .andExpect(content().string(not(containsString("Exception"))))
        .andExpect(content().string(not(containsString("com.avms"))));
  }

  @Test
  void pathTypeMismatchReturnsGeneric400() throws Exception {
    mvc.perform(get("/api/sales/not-a-number/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Invalid request parameter."))
        .andExpect(content().string(not(containsString("NumberFormat"))))
        .andExpect(content().string(not(containsString("MethodArgumentTypeMismatch"))));
  }

  @Test
  void unknownRouteReturnsGeneric404() throws Exception {
    mvc.perform(get("/api/no-such-thing/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Not found."))
        .andExpect(content().string(not(containsString("NoResourceFound"))));
  }

  @Test
  void wrongMethodReturnsGeneric405() throws Exception {
    mvc.perform(delete("/api/health/"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Method not allowed."))
        .andExpect(content().string(not(containsString("RequestMethod"))));
  }
}
