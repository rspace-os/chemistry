package com.researchspace.chemistry.bondstrength;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class BondStrengthServiceTest {
  private static final String MODEL_URL = "http://bond-strength:8000";
  private static final String REACTION = "(C(=O)O).(OCC)>>(C(=O)OCC).(O)";

  private MockRestServiceServer modelService;
  private BondStrengthService service;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    modelService = MockRestServiceServer.bindTo(builder).build();
    service = new BondStrengthService(MODEL_URL, builder);
  }

  @Test
  void moleculeIsSentAsKekulisedMolfileAndResponseIsPassedBack() {
    modelService
        .expect(requestTo(MODEL_URL + "/predict"))
        .andExpect(jsonPath("$.molfile", Matchers.containsString("V2000")))
        .andExpect(jsonPath("$.molfile", Matchers.not(Matchers.containsString("  1  2  4"))))
        .andRespond(withSuccess("{\"supported\":true}", MediaType.APPLICATION_JSON));

    assertEquals("{\"supported\":true}", service.predict("c1ccccc1"));
    modelService.verify();
  }

  @Test
  void atomsAreSentInInputOrder() {
    modelService
        .expect(requestTo(MODEL_URL + "/predict"))
        .andExpect(
            jsonPath(
                "$.molfile", Matchers.stringContainsInOrder(" Cl ", " C ", " C ", " N ", " O ")))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    service.predict("ClCC(N)=O");
    modelService.verify();
  }

  @Test
  void trailingSlashInServiceUrlIsIgnored() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(MODEL_URL + "/predict"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    new BondStrengthService(MODEL_URL + "/", builder).predict("CCO");
    server.verify();
  }

  @Test
  void reactionIsReportedAsNotSupportedWithoutCallingTheModelService() {
    assertEquals(BondStrengthService.REACTION_NOT_SUPPORTED, service.predict(REACTION));
    modelService.verify();
  }

  @Test
  void unreadableInputIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> service.predict("not a structure"));
  }

  @Test
  void inputTheModelServiceCannotReadIsRejected() {
    modelService
        .expect(requestTo(MODEL_URL + "/predict"))
        .andRespond(withStatus(HttpStatusCode.valueOf(422)));

    assertThrows(IllegalArgumentException.class, () -> service.predict("CCO"));
  }

  @Test
  void modelServiceFailureIsReportedAsBadGateway() {
    modelService
        .expect(requestTo(MODEL_URL + "/predict"))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    ResponseStatusException e =
        assertThrows(ResponseStatusException.class, () -> service.predict("CCO"));
    assertEquals(HttpStatus.BAD_GATEWAY, e.getStatusCode());
  }

  @Test
  void missingConfigurationIsReportedAsUnavailable() {
    BondStrengthService unconfigured = new BondStrengthService("", RestClient.builder());

    ResponseStatusException e =
        assertThrows(ResponseStatusException.class, () -> unconfigured.predict("CCO"));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatusCode());
  }
}
