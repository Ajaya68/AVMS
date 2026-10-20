package com.avms.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Verifies RateLimitFilter buckets without booting the full context. */
class RateLimitFilterTest {

  @RestController
  static class PingController {
    @PostMapping("/api/auth/login")
    String login() {
      return "ok";
    }

    @PostMapping("/api/auth/refresh")
    String refresh() {
      return "ok";
    }

    @PostMapping("/api/auth/forgot-password")
    String forgot() {
      return "ok";
    }

    @PostMapping("/api/auth/reset-password")
    String reset() {
      return "ok";
    }

    @PostMapping("/api/payments")
    String payments() {
      return "ok";
    }

    @PostMapping("/api/mutate-a")
    String mutateA() {
      return "ok";
    }

    @PostMapping("/api/mutate-b")
    String mutateB() {
      return "ok";
    }

    @GetMapping("/api/products")
    String products() {
      return "ok";
    }
  }

  private MockMvc mvc(int loginMax, int refreshMax, int defaultMax) {
    return mvc(loginMax, refreshMax, 120, defaultMax);
  }

  private MockMvc mvc(int loginMax, int refreshMax, int mutateMax, int defaultMax) {
    return MockMvcBuilders.standaloneSetup(new PingController())
        .addFilters(new RateLimitFilter(true, loginMax, refreshMax, mutateMax, defaultMax, 60))
        .build();
  }

  @Test
  void loginBucketThrottlesAndReturnsEnvelope() throws Exception {
    MockMvc mvc = mvc(2, 100, 1000);
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"a@b.c\",\"password\":\"x\"}"))
        .andExpect(status().isOk());
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"a@b.c\",\"password\":\"x\"}"))
        .andExpect(status().isOk());
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"a@b.c\",\"password\":\"x\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "60"))
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void bucketsAreIndependentPerEndpoint() throws Exception {
    MockMvc mvc = mvc(1, 100, 1000);
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isTooManyRequests());
    // Refresh bucket untouched by login exhaustion.
    mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/products"))
        .andExpect(status().isOk());
  }

  @Test
  void forgotAndResetShareLoginBucket() throws Exception {
    MockMvc mvc = mvc(1, 100, 1000);
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk());
    // Credential-recovery endpoints draw from the strict login bucket so a
    // leaked login budget cannot be renewed by switching endpoint families.
    mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"x@y.z\"}"))
        .andExpect(status().isTooManyRequests());
    mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
            .content("{\"uidb64\":\"MQ\",\"token\":\"t\",\"new_password\":\"12345678\"}"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void mutationsShareStrictBucketIndependentOfReads() throws Exception {
    MockMvc mvc = mvc(100, 100, 2, 1000);
    mvc.perform(post("/api/mutate-a").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk());
    mvc.perform(post("/api/mutate-b").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk());
    // Third mutation in the window: throttled even across different endpoints
    mvc.perform(post("/api/mutate-a").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.success").value(false));
    // Reads are unaffected by mutation exhaustion
    mvc.perform(get("/api/products"))
        .andExpect(status().isOk());
  }
}
