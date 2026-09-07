package com.researchspace.chemistry.stoichiometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.researchspace.chemistry.analysis.Molecule;
import com.researchspace.chemistry.analysis.MoleculeRole;
import com.researchspace.chemistry.extract.ExtractionRequest;
import com.researchspace.chemistry.extract.ExtractionResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class StoichiometryIT {

  @Autowired private TestRestTemplate restTemplate;

  private static final String STOICHIOMETRY_ENDPOINT = "/chemistry/stoichiometry";

  @Test
  public void testStoichiometryAnalysisFromSmiles() {
    String reaction = "(C(=O)O).(OCC)>>(C(=O)OCC).(O)";
    ExtractionRequest request = new ExtractionRequest(reaction);

    ResponseEntity<ExtractionResult> response = makeStoichiometryRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());

    ExtractionResult result = response.getBody();
    assertTrue(result.isReaction());

    List<Molecule> reactants = result.getReactants();
    assertEquals(2, reactants.size());

    Molecule firstReactant = reactants.get(0);
    assertEquals(MoleculeRole.REACTANT, firstReactant.getRole());
    assertEquals(3, firstReactant.getAtomCount());
    assertEquals(2, firstReactant.getBondCount());
    assertEquals("C H2 O2", firstReactant.getFormula());

    List<Molecule> products = result.getProducts();
    assertEquals(2, products.size());

    Molecule product = products.get(0);
    assertEquals(MoleculeRole.PRODUCT, product.getRole());
    assertEquals(5, product.getAtomCount());
    assertEquals(4, product.getBondCount());
    assertEquals("C3 H6 O2", product.getFormula());
  }

  @Test
  public void testStoichiometryAnalysisFromRxnFile() throws IOException {
    String filePath = "src/test/resources/chemistry_file_examples/methane-combustion.rxn";
    String fileContent = readFileContent(filePath);
    ExtractionRequest request = new ExtractionRequest(fileContent);

    ResponseEntity<ExtractionResult> response = makeStoichiometryRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());

    ExtractionResult result = response.getBody();
    assertTrue(result.isReaction());

    List<Molecule> reactants = result.getReactants();
    assertEquals(3, reactants.size());

    Molecule firstReactant = reactants.get(0);
    assertEquals(MoleculeRole.REACTANT, firstReactant.getRole());
    assertEquals(1, firstReactant.getAtomCount());
    assertEquals(0, firstReactant.getBondCount());
    assertEquals("C H4", firstReactant.getFormula());

    List<Molecule> products = result.getProducts();
    assertEquals(3, products.size());

    Molecule product = products.get(0);
    assertEquals(MoleculeRole.PRODUCT, product.getRole());
    assertEquals(3, product.getAtomCount());
    assertEquals(2, product.getBondCount());
    assertEquals("C O2", product.getFormula());
  }

  @Test
  public void testStoichiometryAnalysisWithNonReaction() {
    String nonReaction = "CCC";
    ExtractionRequest request = new ExtractionRequest(nonReaction);

    ResponseEntity<String> response =
        restTemplate.postForEntity(STOICHIOMETRY_ENDPOINT, request, String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("For Stoichiometry analysis, the input must be a reaction.", response.getBody());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testNullOrEmptyInput(String inputValue) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    String requestBody =
        String.format("{\"input\": %s}", inputValue == null ? "null" : "\"" + inputValue + "\"");

    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(STOICHIOMETRY_ENDPOINT, request, String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testMissingInputParameter() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    String requestBody = "{}";
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(STOICHIOMETRY_ENDPOINT, request, String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testInvalidChemicalStructure() {
    ExtractionRequest request = new ExtractionRequest("invalid");

    ResponseEntity<String> response =
        restTemplate.postForEntity(STOICHIOMETRY_ENDPOINT, request, String.class);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  private ResponseEntity<ExtractionResult> makeStoichiometryRequest(ExtractionRequest request) {
    return restTemplate.postForEntity(STOICHIOMETRY_ENDPOINT, request, ExtractionResult.class);
  }

  private String readFileContent(String filePath) throws IOException {
    return new String(Files.readAllBytes(Paths.get(filePath)));
  }
}
