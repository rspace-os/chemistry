package com.researchspace.chemistry.stoichiometry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.researchspace.chemistry.analysis.ChemicalAnalyser;
import com.researchspace.chemistry.extract.ExtractionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StoichiometryController.class)
public class StoichiometryControllerTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private ChemicalAnalyser chemicalAnalyser;

  private static final String ENDPOINT = "/chemistry/stoichiometry";

  @Test
  void whenValidRequest_thenReturns200AndResult() throws Exception {
    ExtractionResult result = new ExtractionResult();
    when(chemicalAnalyser.analyse(any(), eq(true))).thenReturn(result);
    String validRequestBody =
        """
            {
                "input": "(C(=O)O).(OCC)>>(C(=O)OCC).(O)"
            }
            """;

    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(validRequestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.moleculeInfo").exists());
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
  void whenIncorrectMissingField_thenReturns400() throws Exception {
    String requestWithMissingField =
        """
            {
                "someString": "abc"
            }
            """;

    mockMvc
        .perform(
            post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(requestWithMissingField))
        .andExpect(status().isBadRequest());
  }
}
