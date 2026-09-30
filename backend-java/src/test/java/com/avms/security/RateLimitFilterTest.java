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

    @GetMapping("/api/products")
    String products() {
      return "ok";
    }
  }

  private MockMvc mvc(int loginMax, int refreshMax, int defaultMax) {
    return MockMvcBuilders.standaloneSetup(new PingController())
        .addFilters(new RateLimitFilter(true, loginMax, refreshMax, defaultMax, 60))
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
}
