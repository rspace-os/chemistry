package com.researchspace.chemistry.image;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImageController.class)
public class ImageControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ImageService imageService;

  private static final String ENDPOINT = "/chemistry/image";

  @Test
  void whenValidRequest_thenReturns200AndResult() throws Exception {
    byte[] results = "an image".getBytes();
    when(imageService.exportImage(any())).thenReturn(results);
    String validRequestBody =
        """
            {
                "input": "CCC",
                "inputFormat": "smiles",
                "outputFormat": "png"
            }
            """;

    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(validRequestBody))
        .andExpect(status().isOk())
        .andExpect(content().string("an image"));
  }

  @Test
  void whenNoSearchRequestBody_thenReturns400() throws Exception {
    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenEmptySearchRequestBody_thenReturns400() throws Exception {
    String emptyRequestBody = "{}";

    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(emptyRequestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenIncorrectRequestField_thenReturns400() throws Exception {
    String requestWithIncorrectField =
        """
            {
                "someString": "abc"
            }
            """;

    mockMvc
        .perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithIncorrectField))
        .andExpect(status().isBadRequest());
  }
}
