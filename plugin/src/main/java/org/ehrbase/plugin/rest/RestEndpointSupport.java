package org.ehrbase.plugin.rest;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class RestEndpointSupport{
  public static ResponseEntity<Object> prepareErrorResponse(Exception ex, String message, HttpHeaders headers, HttpStatus status) {
    Map<String, Object> body = Map.of(
        "error", status.getReasonPhrase(),
        "message", message);
    return new ResponseEntity<>(body, headers, status);
  }
}
