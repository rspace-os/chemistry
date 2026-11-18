package com.researchspace.chemistry.bondstrength;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Forwards a structure to the bond strength model service. The structure is loaded with Indigo so
 * any supported input format works, and is sent on as a molfile so the model service numbers the
 * atoms in the same order as the input.
 */
@Service
public class BondStrengthService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BondStrengthService.class);

  static final String REACTION_NOT_SUPPORTED =
      """
      {"supported":false,"unsupportedReason":"REACTION","unsupportedDetail":null,"bonds":[],\
      "supportedBondCount":0,"totalBondCount":0,"imageSvg":null,"method":null}\
      """;

  private final String serviceUrl;
  private final RestClient restClient;

  @Autowired
  public BondStrengthService(@Value("${bondstrength.service.url:}") String serviceUrl) {
    this(serviceUrl, RestClient.builder().requestFactory(requestFactory()));
  }

  BondStrengthService(String serviceUrl, RestClient.Builder restClientBuilder) {
    this.serviceUrl = serviceUrl;
    this.restClient = restClientBuilder.build();
  }

  private static JdkClientHttpRequestFactory requestFactory() {
    // HTTP/1.1 only: by default the JDK client asks plain-HTTP servers to upgrade to HTTP/2.
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(30));
    return factory;
  }

  public String predict(String input) {
    if (StringUtils.isBlank(serviceUrl)) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "The bond strength service is not configured.");
    }
    Indigo indigo = new Indigo();
    indigo.setOption("ignore-stereochemistry-errors", true);
    IndigoObject molecule;
    try {
      molecule = indigo.loadMolecule(input);
    } catch (IndigoException e) {
      if (isReaction(indigo, input)) {
        return REACTION_NOT_SUPPORTED;
      }
      throw new IllegalArgumentException(
          "Can't load input as a molecule. Input: " + StringUtils.abbreviate(input, 50), e);
    }
    molecule.dearomatize();
    return callModelService(molecule.molfile());
  }

  private boolean isReaction(Indigo indigo, String input) {
    try {
      indigo.loadReaction(input);
      return true;
    } catch (IndigoException e) {
      try {
        indigo.loadQueryReaction(input);
        return true;
      } catch (IndigoException notAReaction) {
        return false;
      }
    }
  }

  private String callModelService(String molfile) {
    try {
      return restClient
          .post()
          .uri(serviceUrl + "/predict")
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("molfile", molfile))
          .retrieve()
          .body(String.class);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 422) {
        throw new IllegalArgumentException("The bond strength service could not read the input.");
      }
      throw modelServiceFailure(e);
    } catch (RestClientException e) {
      throw modelServiceFailure(e);
    }
  }

  private ResponseStatusException modelServiceFailure(RestClientException e) {
    LOGGER.warn("Bond strength service call to {} failed: {}", serviceUrl, e.getMessage());
    return new ResponseStatusException(
        HttpStatus.BAD_GATEWAY, "The bond strength service did not respond.", e);
  }
}
