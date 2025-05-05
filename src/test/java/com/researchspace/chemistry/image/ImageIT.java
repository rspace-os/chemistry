package com.researchspace.chemistry.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
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
public class ImageIT {

  @Autowired private TestRestTemplate restTemplate;

  private static final String IMAGE_ENDPOINT = "/chemistry/image";

  @Test
  public void testPngGeneration() throws IOException {
    String input = readFileContent("src/test/resources/chemistry_file_examples/colchicine.mol");
    ImageDTO request = new ImageDTO(input, "smi", "png", "100", "100");
    ResponseEntity<byte[]> response = makeImageRequest(request);
    assertImageGeneratedWithDimensions(response, 100, 100);
  }

  private void assertImageGeneratedWithDimensions(
      ResponseEntity<byte[]> response, int width, int height) throws IOException {
    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertTrue(response.getBody().length > 0);

    BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getBody()));
    assertEquals(width, image.getWidth());
    assertEquals(height, image.getHeight());
  }

  @Test
  public void testSvgGeneration() throws IOException {
    String input = readFileContent("src/test/resources/chemistry_file_examples/chlorine.cdxml");
    ImageDTO request = new ImageDTO(input, "smi", "svg", "100", "100");
    ResponseEntity<byte[]> response = makeImageRequest(request);
    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().length > 0);

    String svgContent = new String(response.getBody());
    assertTrue(svgContent.contains("<?xml") && svgContent.contains("<svg"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"jpg", "jpeg"})
  public void testVariationsOfJpeg(String jpegVariation) throws IOException {
    String filePath = "src/test/resources/chemistry_file_examples/mbi.cdx";
    byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
    String input = Base64.getEncoder().encodeToString(fileBytes);
    ImageDTO request = new ImageDTO(input, "smi", jpegVariation, "100", "100");
    ResponseEntity<byte[]> response = makeImageRequest(request);
    assertImageGeneratedWithDimensions(response, 100, 100);
  }

  @Test
  public void testGenerateImageWithDefaultDimensions() throws IOException {
    String input = readFileContent("src/test/resources/chemistry_file_examples/propane.smiles");
    ImageDTO request = new ImageDTO(input, "smi", "png", null, null);
    ResponseEntity<byte[]> response = makeImageRequest(request);
    assertImageGeneratedWithDimensions(response, 500, 500);
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testNullOrEmptyInput(String inputValue) {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("input", inputValue);
    requestBody.put("inputFormat", "smi");
    requestBody.put("outputFormat", "png");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<byte[]> response =
        restTemplate.postForEntity(IMAGE_ENDPOINT, entity, byte[].class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testNullOrEmptyOutputFormat(String outputFormat) {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("input", "CCC");
    requestBody.put("inputFormat", "smi");
    requestBody.put("outputFormat", outputFormat);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<byte[]> response =
        restTemplate.postForEntity(IMAGE_ENDPOINT, entity, byte[].class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testMissingInputParameter() {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("inputFormat", "smi");
    requestBody.put("outputFormat", "png");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<byte[]> response =
        restTemplate.postForEntity(IMAGE_ENDPOINT, entity, byte[].class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testMissingOutputFormatParameter() {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("input", "CCC");
    requestBody.put("inputFormat", "smi");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<byte[]> response =
        restTemplate.postForEntity(IMAGE_ENDPOINT, entity, byte[].class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testInvalidOutputFormat() {
    ImageDTO request = new ImageDTO("CCC", "smi", "invalid output format", "200", "200");
    ResponseEntity<byte[]> response = makeImageRequest(request);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  @Test
  public void testInvalidChemicalStructure() {
    ImageDTO request = new ImageDTO("not a valid structure", "smi", "png", "200", "200");
    ResponseEntity<byte[]> response = makeImageRequest(request);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  private ResponseEntity<byte[]> makeImageRequest(ImageDTO request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<ImageDTO> entity = new HttpEntity<>(request, headers);
    return restTemplate.postForEntity(IMAGE_ENDPOINT, entity, byte[].class);
  }

  private String readFileContent(String filePath) throws IOException {
    return Files.readString(Paths.get(filePath));
  }
}
