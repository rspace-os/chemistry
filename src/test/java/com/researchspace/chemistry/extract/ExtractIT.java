package com.researchspace.chemistry.extract;

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
public class ExtractIT {

  @Autowired private TestRestTemplate restTemplate;

  private static final String EXTRACT_ENDPOINT = "/chemistry/extract";

  @Test
  public void testExtractFromSmiles() {
    ExtractionRequest request = new ExtractionRequest("CCC");
    ResponseEntity<ExtractionResult> response = makeExtractionRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());

    ExtractionResult result = response.getBody();
    Molecule expected =
        new Molecule.Builder()
            .atomCount(3)
            .bondCount(2)
            .formalCharge(0)
            .exactMass(44.06)
            .mass(44.1)
            .formula("C3 H8")
            .name("")
            .role(MoleculeRole.MOLECULE)
            .build();
    assertEquals(1, result.getMoleculeInfo().size());
    assertEquals("C3 H8", result.getFormula());
    assertMoleculesAreEqual(expected, result.getMoleculeInfo().get(0));
  }

  private void assertMoleculesAreEqual(Molecule expected, Molecule actual) {
    assertEquals(expected.getAtomCount(), actual.getAtomCount());
    assertEquals(expected.getBondCount(), actual.getBondCount());
    assertEquals(expected.getFormalCharge(), actual.getFormalCharge());
    assertEquals(expected.getExactMass(), actual.getExactMass());
    assertEquals(expected.getMass(), actual.getMass());
    assertEquals(expected.getFormula(), actual.getFormula());
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getRole(), actual.getRole());
  }

  @Test
  public void testExtractFromMolFile() throws IOException {
    String filePath = "src/test/resources/chemistry_file_examples/colchicine.mol";
    String fileContent = readFileContent(filePath);

    ExtractionRequest request = new ExtractionRequest(fileContent);
    ResponseEntity<ExtractionResult> response = makeExtractionRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    ExtractionResult result = response.getBody();
    Molecule expected =
        new Molecule.Builder()
            .atomCount(29)
            .bondCount(31)
            .formalCharge(0)
            .exactMass(399.17)
            .mass(399.44)
            .formula("C22 H25 N O6")
            .name("")
            .role(MoleculeRole.MOLECULE)
            .build();
    assertEquals(1, result.getMoleculeInfo().size());
    assertEquals("C22 H25 N O6", result.getFormula());
    assertMoleculesAreEqual(expected, result.getMoleculeInfo().get(0));
  }

  @Test
  public void testExtractFromSdfFile() throws IOException {
    String filePath = "src/test/resources/chemistry_file_examples/caffeine.sdf";
    String fileContent = readFileContent(filePath);

    ExtractionRequest request = new ExtractionRequest(fileContent);
    ResponseEntity<ExtractionResult> response = makeExtractionRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    ExtractionResult result = response.getBody();
    Molecule expected =
        new Molecule.Builder()
            .atomCount(24)
            .bondCount(25)
            .formalCharge(0)
            .exactMass(194.08)
            .mass(194.19)
            .formula("C8 H10 N4 O2")
            .name("")
            .role(MoleculeRole.MOLECULE)
            .build();
    assertEquals(1, result.getMoleculeInfo().size());
    assertEquals("C8 H10 N4 O2", result.getFormula());
    assertMoleculesAreEqual(expected, result.getMoleculeInfo().get(0));
  }

  @Test
  public void testExtractReaction() throws IOException {
    String filePath = "src/test/resources/chemistry_file_examples/methane-combustion.rxn";
    String fileContent = readFileContent(filePath);

    ExtractionRequest request = new ExtractionRequest(fileContent);
    ResponseEntity<ExtractionResult> response = makeExtractionRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    ExtractionResult result = response.getBody();
    assertEquals("C H4 + O2 + O2 > C O2 + H2 O + H2 O", result.getFormula());
    assertTrue(result.isReaction());
    assertEquals(0, result.getMoleculeInfo().size());
  }

  @Test
  public void testExtractFromMrvFile() throws IOException {
    String filePath =
        "src/test/resources/chemistry_file_examples/benzylpenicillin_GitHub_fusion809_artwork.mrv";
    String fileContent = readFileContent(filePath);

    ExtractionRequest request = new ExtractionRequest(fileContent);
    ResponseEntity<ExtractionResult> response = makeExtractionRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    ExtractionResult result = response.getBody();
    Molecule expected =
        new Molecule.Builder()
            .atomCount(23)
            .bondCount(25)
            .formalCharge(0)
            .exactMass(334.1)
            .mass(334.39)
            .formula("C16 H18 N2 O4 S")
            .name("")
            .role(MoleculeRole.MOLECULE)
            .build();
    assertEquals(1, result.getMoleculeInfo().size());
    assertEquals("C16 H18 N2 O4 S", result.getFormula());
    assertMoleculesAreEqual(expected, result.getMoleculeInfo().get(0));
  }

  @Test
  public void testExtractFromCdxFile() throws IOException {
    String filePath = "src/test/resources/chemistry_file_examples/aspirin.cdx";
    byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
    String base64Content = Base64.getEncoder().encodeToString(fileBytes);

    ExtractionRequest request = new ExtractionRequest(base64Content);
    ResponseEntity<ExtractionResult> response = makeExtractionRequest(request);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    ExtractionResult result = response.getBody();
    Molecule expected =
        new Molecule.Builder()
            .atomCount(13)
            .bondCount(13)
            .formalCharge(0)
            .exactMass(180.04)
            .mass(180.16)
            .formula("C9 H8 O4")
            .name("")
            .role(MoleculeRole.MOLECULE)
            .build();
    assertEquals(1, result.getMoleculeInfo().size());
    assertEquals("C9 H8 O4", result.getFormula());
    assertMoleculesAreEqual(expected, result.getMoleculeInfo().get(0));
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testNullOrEmptyInput(String inputValue) {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("input", inputValue);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<ExtractionResult> response =
        restTemplate.postForEntity(EXTRACT_ENDPOINT, entity, ExtractionResult.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testMissingInputParameter() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(new HashMap<>(), headers);

    ResponseEntity<ExtractionResult> response =
        restTemplate.postForEntity(EXTRACT_ENDPOINT, entity, ExtractionResult.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testInvalidChemicalStructure() {
    ExtractionRequest request = new ExtractionRequest("this-is-not-a-valid-structure");
    ResponseEntity<ExtractionResult> response = makeExtractionRequest(request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  private ResponseEntity<ExtractionResult> makeExtractionRequest(ExtractionRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<ExtractionRequest> entity = new HttpEntity<>(request, headers);
    return restTemplate.postForEntity(EXTRACT_ENDPOINT, entity, ExtractionResult.class);
  }

  private String readFileContent(String filePath) throws IOException {
    return Files.readString(Paths.get(filePath));
  }
}
