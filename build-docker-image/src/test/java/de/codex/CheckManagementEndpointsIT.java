package de.codex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class CheckManagementEndpointsIT {

  private final RestTemplate rest = new RestTemplate();

  @Test
  void testHealth() {
    ResponseEntity<String> response = rest.getForEntity("http://localhost:9999/management/health", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    log.info("----- HEALTH_ENDPOINT_RESPONSE_BODY ------");
    log.info(response.getBody());
  }

  @Test
  void testLiveness() {
    ResponseEntity<String> response = rest.getForEntity("http://localhost:9999/management/health/liveness", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    log.info("----- LIVENESS_ENDPOINT_RESPONSE_BODY ------");
    log.info(response.getBody());
  }
  @Test
  void testReadiness() {
    ResponseEntity<String> response = rest.getForEntity("http://localhost:9999/management/health/readiness", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    log.info("----- READINESS_ENDPOINT_RESPONSE_BODY ------");
    log.info(response.getBody());
  }
}
