package com.github.rspaceos.chemistry.convert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(ConvertController.class)
public class ConvertControllerTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private ConvertService convertService;

  private static final String ENDPOINT = "/chemistry/convert";

  @Test
  void whenValidRequest_thenReturns200AndResult() throws Exception {
    String smiles = "CCC";
    when(convertService.convert(any())).thenReturn(smiles);
    String validRequestBody =
        """
            {
                "outputFormat": "cdxml",
                "input": "CCC"
            }
            """;

    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(validRequestBody))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.content().string(smiles));
  }

  @Test
  void whenNoRequestBody_thenReturns400() throws Exception {
    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenEmptyRequestBody_thenReturns400() throws Exception {
    String emptyRequestBody = "{}";

    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(emptyRequestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenRequestMissingField_thenReturns400() throws Exception {
    String requestWithMissingField =
        """
            {
                "outputFormat": "cdxml"
            }
            """;

    mockMvc
        .perform(
            post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(requestWithMissingField))
        .andExpect(status().isBadRequest());
  }
}
