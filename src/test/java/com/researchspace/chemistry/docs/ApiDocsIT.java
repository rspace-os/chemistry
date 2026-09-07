package com.researchspace.chemistry.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class ApiDocsIT {
  @Autowired private TestRestTemplate restTemplate;

  @Value("${server.servlet.context-path:}")
  private String contextPath;

  @Test
  void servesScalarWithLocalAssets() {
    var response = restTemplate.getForEntity("/api-docs", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String html = response.getBody();
    assertTrue(html.contains("Scalar.createApiReference"));
    assertTrue(html.contains("\"favicon\":\"favicon.png\""));
    assertTrue(html.contains("src=\"api-docs/scalar.js\""));
    assertTrue(html.contains("\"url\":\"v3/api-docs\""));

    var specification = restTemplate.getForEntity("/v3/api-docs", String.class);
    assertEquals(HttpStatus.OK, specification.getStatusCode());
    assertFalse(specification.getBody().contains("\"/api-docs"));

    var script = restTemplate.getForEntity("/api-docs/scalar.js", String.class);
    assertEquals(HttpStatus.OK, script.getStatusCode());
    assertTrue(script.getHeaders().getContentType().toString().contains("javascript"));
    assertTrue(script.getBody().contains("createApiReference"));
    assertEquals(
        HttpStatus.OK, restTemplate.getForEntity("/favicon.png", byte[].class).getStatusCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/swagger-ui.html", "/swagger-ui/index.html", "/api-docs/"})
  void redirectsLandingPagesWithinTheContext(String path) {
    var response =
        restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW).getForEntity(path, String.class);
    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    URI location = response.getHeaders().getLocation();
    assertEquals(contextPath + "/api-docs", location.getPath());
  }
}
