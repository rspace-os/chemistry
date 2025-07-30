package com.researchspace.chemistry.stoichiometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.researchspace.chemistry.analysis.Molecule;
import com.researchspace.chemistry.analysis.MoleculeRole;
import com.researchspace.chemistry.extract.ExtractionRequest;
import com.researchspace.chemistry.extract.ExtractionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
public class StoichiometryIT {

  @Autowired private TestRestTemplate restTemplate;

  private static final String STOICHIOMETRY_ENDPOINT = "/chemistry/stoichiometry";

  @Test
  public void testStoichiometryAnalysisFromSmiles() {
    // Arrange
    String reaction = "(C(=O)O).(OCC)>>(C(=O)OCC).(O)";
    ExtractionRequest request = new ExtractionRequest(reaction);
    
    // Act
    ResponseEntity<ExtractionResult> response = makeStoichiometryRequest(request);
    
    // Assert
    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());
    
    ExtractionResult result = response.getBody();
    assertTrue(result.isReaction());
    
    // Verify reactants
    List<Molecule> reactants = result.getReactants();
    assertFalse(reactants.isEmpty());
    assertEquals(MoleculeRole.REACTANT, reactants.get(0).getRole());
    
    // Verify products
    List<Molecule> products = result.getProducts();
    assertFalse(products.isEmpty());
    assertEquals(MoleculeRole.PRODUCT, products.get(0).getRole());
  }

  @Test
  public void testStoichiometryAnalysisFromRxnFile() throws IOException {
    // Arrange
    String filePath = "src/test/resources/chemistry_file_examples/methane-combustion.rxn";
    String fileContent = readFileContent(filePath);
    ExtractionRequest request = new ExtractionRequest(fileContent);
    
    // Act
    ResponseEntity<ExtractionResult> response = makeStoichiometryRequest(request);
    
    // Assert
    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());
    
    ExtractionResult result = response.getBody();
    assertTrue(result.isReaction());
    assertFalse(result.getMoleculeInfo().isEmpty());
    
    // Verify that we have both reactants and products
    assertFalse(result.getReactants().isEmpty());
    assertFalse(result.getProducts().isEmpty());
  }

  @Test
  public void testStoichiometryAnalysisWithNonReaction() {
    // Arrange
    String nonReaction = "CCC";
    ExtractionRequest request = new ExtractionRequest(nonReaction);
    
    // Act
    ResponseEntity<ExtractionResult> response = makeStoichiometryRequest(request);
    
    // Assert
    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());
    
    ExtractionResult result = response.getBody();
    assertFalse(result.isReaction());
    assertTrue(result.getMoleculeInfo().isEmpty());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testNullOrEmptyInput(String inputValue) {
    // Arrange
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    
    String requestBody = String.format("{\"input\": %s}", 
        inputValue == null ? "null" : "\"" + inputValue + "\"");
    
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
    
    // Act
    ResponseEntity<String> response = 
        restTemplate.postForEntity(STOICHIOMETRY_ENDPOINT, request, String.class);
    
    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testMissingInputParameter() {
    // Arrange
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    
    String requestBody = "{}";
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
    
    // Act
    ResponseEntity<String> response = 
        restTemplate.postForEntity(STOICHIOMETRY_ENDPOINT, request, String.class);
    
    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testInvalidChemicalStructure() {
    // Arrange
    ExtractionRequest request = new ExtractionRequest("invalid");
    
    // Act
    ResponseEntity<String> response = 
        restTemplate.postForEntity(STOICHIOMETRY_ENDPOINT, request, String.class);
    
    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  private ResponseEntity<ExtractionResult> makeStoichiometryRequest(ExtractionRequest request) {
    return restTemplate.postForEntity(
        STOICHIOMETRY_ENDPOINT, request, ExtractionResult.class);
  }

  private String readFileContent(String filePath) throws IOException {
    return new String(Files.readAllBytes(Paths.get(filePath)));
  }
}