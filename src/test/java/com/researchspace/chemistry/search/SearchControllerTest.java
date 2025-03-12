package com.researchspace.chemistry.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SearchController.class)
public class SearchControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SearchService searchService;

  private static final String SEARCH_ENDPOINT = "/chemistry/search";

  private static final String SAVE_ENDPOINT = "/chemistry/save";

  private static final String CLEAR_SEARCH_INDEXES_ENDPOINT = "/chemistry/clearSearchIndexes";


  @Test
  void whenValidSearchRequest_thenReturns200AndResult() throws Exception {
    List<String> results = List.of("123", "456");
    when(searchService.search(any())).thenReturn(results);
    String validRequestBody =
        """
            {
                "chemicalSearchTerm": "CCC"
            }
            """;

    mockMvc
        .perform(
            post(SEARCH_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(validRequestBody))
        .andExpect(status().isOk())
        .andExpect(content().string("[\"123\",\"456\"]"));
  }

  @Test
  void whenNoSearchRequestBody_thenReturns400() throws Exception {
    mockMvc
        .perform(post(SEARCH_ENDPOINT).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenEmptySearchRequestBody_thenReturns400() throws Exception {
    String emptyRequestBody = "{}";

    mockMvc
        .perform(
            post(SEARCH_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(emptyRequestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenIncorrectSearchField_thenReturns400() throws Exception {
    String requestWithIncorrectField =
        """
            {
                "someString": "abc"
            }
            """;

    mockMvc
        .perform(
            post(SEARCH_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithIncorrectField))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenValidSaveRequest_thenReturns200AndResult() throws Exception {
    String validRequestBody =
        """
            {
                "chemical": "CCC",
                "chemicalId": "123"
            }
            """;

    mockMvc
        .perform(
            post(SAVE_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(validRequestBody))
        .andExpect(status().isOk())
        .andExpect(content().string("Saved"));
  }

  @Test
  void whenNoSaveRequestBody_thenReturns400() throws Exception {
    mockMvc
        .perform(post(SAVE_ENDPOINT).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenEmptySaveRequestBody_thenReturns400() throws Exception {
    String emptyRequestBody = "{}";

    mockMvc
        .perform(
            post(SAVE_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(emptyRequestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenIncorrectSaveField_thenReturns400() throws Exception {
    String requestWithIncorrectField =
        """
            {
                "someString": "abc"
            }
            """;

    mockMvc
        .perform(
            post(SAVE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithIncorrectField))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenValidCleanSearchIndexesRequest_thenReturns200() throws Exception {
    doNothing().when(searchService).clearIndexFiles();
    verify(searchService, Mockito.never()).clearIndexFiles();

    mockMvc
        .perform(
            delete(CLEAR_SEARCH_INDEXES_ENDPOINT))
        .andExpect(status().isOk());

    verify(searchService, Mockito.times(1)).clearIndexFiles();
  }

}
