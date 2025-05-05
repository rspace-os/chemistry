package com.researchspace.chemistry.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SearchIT {
  @Autowired TestRestTemplate restTemplate;

  static final String SEARCH_ENDPOINT = "/chemistry/search";
  static final String SAVE_ENDPOINT = "/chemistry/save";
  static final String INDEX_ENDPOINT = "/chemistry/index";
  static final String CLEAR_SEARCH_INDEXES_ENDPOINT = "/chemistry/clearSearchIndexes";

  @BeforeEach
  public void setup() {
    clearSearchIndexes();
  }

  @Test
  public void testSavedChemicalIsFoundBySearch() throws Exception {
    // save chem1
    String chem1 = readFileContent("src/test/resources/chemistry_file_examples/colchicine.mol");
    SaveDTO saveRequest = new SaveDTO(chem1, "123");
    ResponseEntity<String> saveResponse = makeSaveRequest(saveRequest);
    assertEquals(HttpStatus.OK, saveResponse.getStatusCode());
    assertEquals("Saved", saveResponse.getBody());

    // save chem2
    SaveDTO saveRequest2 =
        new SaveDTO(
            readFileContent("src/test/resources/chemistry_file_examples/chlorine.cdxml"), "456");
    ResponseEntity<String> saveResponse2 = makeSaveRequest(saveRequest2);
    assertEquals(HttpStatus.OK, saveResponse2.getStatusCode());
    assertEquals("Saved", saveResponse2.getBody());

    // search for chem1
    Map<String, String> searchParams = new HashMap<>();
    searchParams.put("chemicalSearchTerm", chem1);
    ResponseEntity<List<String>> searchResponse = makeSearchRequest(searchParams);

    // results should only contain chem1
    assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
    List<String> results = searchResponse.getBody();
    assertEquals(1, results.size());
    assertTrue(results.contains("123"));
  }

  @Test
  public void testSubstructureSearch() {
    makeSaveRequest(new SaveDTO("CCC", "12"));
    makeSaveRequest(new SaveDTO("CCCO", "34"));
    makeSaveRequest(new SaveDTO("CCN", "56"));
    makeSaveRequest(new SaveDTO("C", "78")); // shouldn't match

    Map<String, String> searchParams = new HashMap<>();
    searchParams.put("chemicalSearchTerm", "CC");
    searchParams.put("searchType", "SUBSTRUCTURE");
    ResponseEntity<List<String>> searchResponse = makeSearchRequest(searchParams);

    assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
    List<String> results = searchResponse.getBody();
    assertEquals(3, results.size());
    assertTrue(results.containsAll(Arrays.asList("12", "34", "56")));
  }

  @Test
  public void testExactMatchSearch() {
    makeSaveRequest(new SaveDTO("CCC", "12"));
    makeSaveRequest(new SaveDTO("CCCO", "34"));
    makeSaveRequest(new SaveDTO("CCN", "56"));
    makeSaveRequest(new SaveDTO("C", "78"));

    Map<String, String> searchParams = new HashMap<>();
    searchParams.put("chemicalSearchTerm", "CCC");
    searchParams.put("searchType", "EXACT");
    ResponseEntity<List<String>> searchResponse = makeSearchRequest(searchParams);

    assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
    List<String> results = searchResponse.getBody();
    assertEquals(1, results.size());
    assertTrue(results.contains("12"));
  }

  @Test
  public void testSearchWorksBeforeAndAfterIndexing() {
    // save chemicals
    makeSaveRequest(new SaveDTO("CCC", "123"));
    makeSaveRequest(new SaveDTO("CCCO", "456"));

    Map<String, String> searchParams = new HashMap<>();
    searchParams.put("chemicalSearchTerm", "CC");

    // search without indexing
    ResponseEntity<List<String>> nonIndexedSearchResponse = makeSearchRequest(searchParams);
    assertEquals(HttpStatus.OK, nonIndexedSearchResponse.getStatusCode());
    List<String> nonIndexedResults = nonIndexedSearchResponse.getBody();
    assertEquals(2, nonIndexedResults.size());
    assertTrue(nonIndexedResults.containsAll(Arrays.asList("123", "456")));

    // perform indexing
    ResponseEntity<String> indexResponse = makeIndexRequest();
    assertEquals(HttpStatus.OK, indexResponse.getStatusCode());
    assertEquals("Indexed", indexResponse.getBody());

    // search after indexing
    ResponseEntity<List<String>> indexedResponse = makeSearchRequest(searchParams);
    assertEquals(HttpStatus.OK, indexedResponse.getStatusCode());
    List<String> results = indexedResponse.getBody();
    assertEquals(2, results.size());
    assertTrue(results.containsAll(Arrays.asList("123", "456")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testEmptyOrNullSearchTerm(String searchTerm) {
    Map<String, String> searchParams = new HashMap<>();
    searchParams.put("chemicalSearchTerm", searchTerm);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(searchParams, headers);
    ResponseEntity<String> response =
        restTemplate.postForEntity(SEARCH_ENDPOINT, entity, String.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testMissingSearchTerm() {
    Map<String, String> searchParams = new HashMap<>();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(searchParams, headers);
    ResponseEntity<String> response =
        restTemplate.postForEntity(SEARCH_ENDPOINT, entity, String.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testInvalidSearchType() {
    Map<String, String> searchParams = new HashMap<>();
    searchParams.put("chemicalSearchTerm", "CCC");
    searchParams.put("searchType", "INVALID_TYPE");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(searchParams, headers);
    ResponseEntity<String> response =
        restTemplate.postForEntity(SEARCH_ENDPOINT, entity, String.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testClearSearchIndexes() {
    makeSaveRequest(new SaveDTO("CCC", "123"));
    // perform search
    Map<String, String> searchParams = new HashMap<>();
    searchParams.put("chemicalSearchTerm", "CCC");
    ResponseEntity<List<String>> searchResponse = makeSearchRequest(searchParams);
    assertEquals(1, searchResponse.getBody().size());

    // clear indexes
    ResponseEntity<String> clearResponse = clearSearchIndexes();
    assertEquals(HttpStatus.OK, clearResponse.getStatusCode());
    assertEquals("Cleared", clearResponse.getBody());

    // verify no results found
    searchResponse = makeSearchRequest(searchParams);
    assertEquals(0, searchResponse.getBody().size());
  }

  private ResponseEntity<String> makeSaveRequest(SaveDTO request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<SaveDTO> entity = new HttpEntity<>(request, headers);
    return restTemplate.postForEntity(SAVE_ENDPOINT, entity, String.class);
  }

  private ResponseEntity<List<String>> makeSearchRequest(Map<String, String> requestParams) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestParams, headers);
    return restTemplate.exchange(
        SEARCH_ENDPOINT, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {});
  }

  private ResponseEntity<String> makeIndexRequest() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>("", headers);
    return restTemplate.postForEntity(INDEX_ENDPOINT, entity, String.class);
  }

  private ResponseEntity<String> clearSearchIndexes() {
    return restTemplate.exchange(
        CLEAR_SEARCH_INDEXES_ENDPOINT, HttpMethod.DELETE, null, String.class);
  }

  private String readFileContent(String filePath) throws Exception {
    return Files.readString(Paths.get(filePath));
  }
}
