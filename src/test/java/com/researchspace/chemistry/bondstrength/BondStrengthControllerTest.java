package com.researchspace.chemistry.bondstrength;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BondStrengthController.class)
class BondStrengthControllerTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private BondStrengthService bondStrengthService;

  private static final String ENDPOINT = "/chemistry/bondStrengths";

  @Test
  void whenValidRequest_thenReturnsModelServiceJson() throws Exception {
    when(bondStrengthService.predict("CCO")).thenReturn("{\"supported\":true}");

    mockMvc
        .perform(
            post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"input\": \"CCO\"}"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.supported").value(true));
  }

  @Test
  void whenEmptyRequestBody_thenReturns400() throws Exception {
    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }
}
