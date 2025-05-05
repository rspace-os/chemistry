package com.researchspace.chemistry.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ConvertIT {

  @Autowired private TestRestTemplate restTemplate;

  private static final String CONVERT_ENDPOINT = "/chemistry/convert";
  private static final String VALID_CDXML_START = "<?xml version=\"1.0\"";
  private static final String VALID_SMILES_PATTERN = "[A-Za-z0-9@+\\-\\[\\]().\\\\=#$:*]+";
  private static final String VALID_KETCHER_START = "{\"root\":{\"nodes";

  @Test
  public void testConvertSmilesToCdxml() throws Exception {
    String input =
        readFileContent(
            "src/test/resources/chemistry_file_examples/acid_orange_CompTox_DTXSID60883437.mol");
    ConvertDTO request = new ConvertDTO(input, "mol", "cdxml");
    ResponseEntity<String> response = makeConvertRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertTrue(response.getBody().startsWith(VALID_CDXML_START));
  }

  @Test
  public void testConvertSdfToSmiles() throws Exception {
    String input = readFileContent("src/test/resources/chemistry_file_examples/caffeine.sdf");
    ConvertDTO request = new ConvertDTO(input, "mol2", "smi");
    ResponseEntity<String> response = makeConvertRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertTrue(response.getBody().matches(VALID_SMILES_PATTERN));
  }

  @Test
  public void testConvertMrvToKetcher() throws Exception {
    String input =
        readFileContent(
            "src/test/resources/chemistry_file_examples/SGroupExpanded_GitHub_chemaxon_jchem-examples.mrv");
    ConvertDTO request = new ConvertDTO(input, "smi", "ket");
    ResponseEntity<String> response = makeConvertRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().startsWith(VALID_KETCHER_START));
  }

  @Test
  public void testConvertCdxToKetcher() throws IOException {
    byte[] fileBytes =
        Files.readAllBytes(Paths.get("src/test/resources/chemistry_file_examples/aspirin.cdx"));
    String base64Content = Base64.getEncoder().encodeToString(fileBytes);

    ConvertDTO request = new ConvertDTO(base64Content, "cdx", "ket");
    ResponseEntity<String> response = makeConvertRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().startsWith(VALID_KETCHER_START));
  }

  @Test
  public void testConvertWithoutInputFormat() {
    String smiles = "c1ccccc1";

    ConvertDTO request = new ConvertDTO(smiles, "cdxml");
    ResponseEntity<String> response = makeConvertRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().startsWith(VALID_CDXML_START));
  }

  @Test
  public void testMissingInputParameter() {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("outputFormat", "cdxml");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(CONVERT_ENDPOINT, entity, String.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testMissingOutputFormatParameter() {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("input", "CCC");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(CONVERT_ENDPOINT, entity, String.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testEmptyRequestBody() {
    Map<String, String> requestBody = new HashMap<>();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(CONVERT_ENDPOINT, entity, String.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testNullOrEmptyInputValue(String inputValue) {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("input", inputValue);
    requestBody.put("outputFormat", "cdxml");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(CONVERT_ENDPOINT, entity, String.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testNullOrEmptyOutputFormatValue(String outputFormatValue) {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("input", "CCC");
    requestBody.put("outputFormat", outputFormatValue);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(CONVERT_ENDPOINT, entity, String.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalidFormat", "abc", "123"})
  public void testUnsupportedOutputFormat(String outputFormat) {
    ConvertDTO request = new ConvertDTO("CCC", "smi", outputFormat);
    ResponseEntity<String> response = makeConvertRequest(request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  @Test
  public void testInvalidChemicalStructure() {
    ConvertDTO request = new ConvertDTO("this-is-not-a-valid-structure", "smi", "cdxml");
    ResponseEntity<String> response = makeConvertRequest(request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  private ResponseEntity<String> makeConvertRequest(ConvertDTO request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<ConvertDTO> entity = new HttpEntity<>(request, headers);
    return restTemplate.postForEntity(CONVERT_ENDPOINT, entity, String.class);
  }

  private String readFileContent(String filePath) throws IOException {
    return Files.readString(Paths.get(filePath));
  }
}
