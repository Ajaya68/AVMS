package com.avms.employees;

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

/** API-parity tests for employees + attendance (codes, self-service, bulk). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeControllerTest {

  @Autowired MockMvc mvc;
  @Autowired AccountsUserRepository users;
  @Autowired VentureRepository ventures;
  @Autowired PasswordEncoder encoder;
  @Autowired SeedRolesService seeds;
  @Autowired JwtService jwt;

  String adminToken;
  String ventureHeader;
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
    ventureHeader = String.valueOf(ventureId);
  }

  @Test
  void employeeAndAttendanceCycle() throws Exception {
    String employee = mvc.perform(post("/api/employees/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"venture\":" + ventureId + ",\"first_name\":\"Ravi\",\"last_name\":\"Kumar\","
                + "\"email\":\"admin@avms.local\",\"department\":\"Ops\",\"designation\":\"Manager\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.employee_code").value("EMP-0001"))
        .andExpect(jsonPath("$.data.full_name").value("Ravi Kumar"))
        .andReturn().getResponse().getContentAsString();
    long employeeId = ((Number) com.jayway.jsonpath.JsonPath.read(employee, "$.data.id")).longValue();

    mvc.perform(get("/api/employees/me/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.linked").value(true))
        .andExpect(jsonPath("$.data.employee.employee_code").value("EMP-0001"));

    String row = mvc.perform(post("/api/attendance/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"employee\":" + employeeId
                + ",\"date\":\"2026-09-27\",\"status\":\"PRESENT\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.status").value("PRESENT"))
        .andExpect(jsonPath("$.data.check_in").exists())
        .andReturn().getResponse().getContentAsString();
    long rowId = ((Number) com.jayway.jsonpath.JsonPath.read(row, "$.data.id")).longValue();

    mvc.perform(get("/api/attendance/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .param("from", "2026-09-01").param("to", "2026-09-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.count").value(1));

    mvc.perform(post("/api/attendance/bulk/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"date\":\"2026-09-26\",\"items\":[{\"employee\":" + employeeId
                + ",\"status\":\"LEAVE\",\"remarks\":\"sick\"}]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.created").value(1));

    mvc.perform(patch("/api/attendance/" + rowId + "/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"check_out\":\"now\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.check_out").exists());

    mvc.perform(delete("/api/attendance/" + rowId + "/").header("Authorization", adminToken)
            .header("X-Venture-Id", ventureHeader))
        .andExpect(status().isOk());
  }
}
