/*
 * Copyright (c) 2021 Jake Smolka (Hannover Medical School) and Vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.application.abac;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.rest.openehr.controller.BaseController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Implementation of custom security expression, to be used in e.g. @PreAuthorize(..) to allow ABAC
 * requests.
 */
public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements
    MethodSecurityExpressionOperations {

  static final String ORGANIZATION = "organization";
  static final String PATIENT = "patient";
  static final String TEMPLATE = "template";
  static final String PRE = "pre";
  static final String POST = "post";

  private final AbacConfig abacConfig;
  private final CompositionService compositionService;
  private Object filterObject;
  private Object returnObject;

  public CustomMethodSecurityExpressionRoot(Authentication authentication,
      AbacConfig abacConfig, CompositionService compositionService) {
    super(authentication);
    this.abacConfig = abacConfig;
    this.compositionService = compositionService;
  }

  /**
   * Custom SpEL expression to be used to check if the remote ABAC allows the operation by given
   * data.
   * <p>
   * TODO-505: only guarantees to work with JSON as input or output for now
   *
   * @param type    Type of scope's resource
   * @param auth    Authentication stored in the security context
   * @param subject Subject ID from the current EHR context
   * @param payload Payload of the request (post-request condition)
   * @return True if ABAC authorizes given attributes
   */
  public boolean checkAbacPost(String type, Authentication auth, UUID subject, Object payload,
      String contentType)
      throws IOException, InterruptedException {

    return checkAbac(type, auth, subject, payload, contentType, POST);
  }

  // TODO-505: doc
  public boolean checkAbacPre(String type, Authentication auth, UUID subject, Object payload,
      String contentType)
      throws IOException, InterruptedException {

    // @PreAuthorize will give different types, e.g. String (for composition), EhrStatus,...
    // so just pipe it through to templateHandling and make by-type handling there

    return checkAbac(type, auth, subject, payload, contentType, PRE);
  }

  private boolean checkAbac(String type, Authentication auth, UUID subject, Object payload,
      String contentType, String authType) throws IOException, InterruptedException {
    // Extract and set parameters according to which parameters are configured
    List<String> policyParameters = new ArrayList<>(
        Arrays.asList(abacConfig.getPolicyCompositionParameters()));

    // Check and extract JWT
    JwtAuthenticationToken jwt = getJwtAuthenticationToken(auth);

    // Request body map. will result in simple JSON like {"patient_id":"...", ...}
    Map<String, String> requestMap = new HashMap<>();

    // Organization attribute handling
    if (policyParameters.contains(ORGANIZATION)) {
      organizationHandling(jwt, requestMap);
    }

    // Patient attribute handling
    if (policyParameters.contains(PATIENT)) {
      patientHandling(jwt, subject, requestMap);
    }

    // Extract template ID from composition
    if (policyParameters.contains(TEMPLATE)) {
      templateHandling(type, payload, contentType, requestMap, authType);
    }

    // Build and fire abac server request
    String requestUrl = abacConfig.getServer() + abacConfig.getPolicyCompositionName();
    return evaluateResponse(abacRequest(requestUrl, requestMap));
  }

  private void organizationHandling(JwtAuthenticationToken jwt, Map<String, String> requestMap) {
    if (jwt.getTokenAttributes().containsKey(abacConfig.getOrganizationClaim())) {
      // "patient_id" not available, use EHRbase subject
      String orgaId = (String) jwt.getTokenAttributes().get(abacConfig.getOrganizationClaim());
      requestMap.put(ORGANIZATION, orgaId);
    } else {
      // TODO-505: reactivate later
      /*throw new IllegalArgumentException("ABAC use of an organization claim is configured but "
          + "can't be retrieved from the given JWT.");*/
    }
  }

  private void patientHandling(JwtAuthenticationToken jwt, UUID subject,
      Map<String, String> requestMap) {
    if (!jwt.getTokenAttributes().containsKey(abacConfig.getPatientClaim())) {
      // "patient_id" not available, use EHRbase subject as fallback
      requestMap.put(PATIENT, subject.toString());
    } else {
      // use "patient_id" if available
      String patientId = (String) jwt.getTokenAttributes().get(abacConfig.getPatientClaim());
      requestMap.put(PATIENT, patientId);
    }
  }

  // payload will be a response body string, in case of @PostAuthorize.
  // payload will be request body string, or already deserialized object (e.g. EhrStatus), in case of @PreAuthorize.
  private void templateHandling(String type, Object payload, String contentType, Map<String,
      String> requestMap, String authType) {
    switch (type) {
      case BaseController.EHR:
        break;  // TODO-505: write handling
      case BaseController.EHR_STATUS:
        break; // TODO-505: write handling
      case BaseController.COMPOSITION:
        String content;
        if (authType.equals(POST)) {
          // @PostAuthorize gives a ResponseEntity type for "returnObject", so payload is of that type
          content = ((ResponseEntity) payload).getBody().toString();
        } else if (authType.equals(PRE)) {
          content = (String) payload;
        } else {
          throw new InternalServerException("ABAC: invalid auth type given.");
        }
        final CompositionFormat compositionFormat;
        if (MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_XML)) {
          compositionFormat = CompositionFormat.XML;
        } else if (MediaType.parseMediaType(contentType)
            .isCompatibleWith(MediaType.APPLICATION_JSON)) {
          compositionFormat = CompositionFormat.JSON;
        } else {
          throw new NotAcceptableException(
              "Only compositions in XML or JSON are supported at the moment");
        }
        String templateId = compositionService
            .getTemplateIdFromInputComposition(content, compositionFormat);
        requestMap.put(TEMPLATE, templateId);
        break;
      case BaseController.DIRECTORY:
        break; // TODO-505: write handling
      case BaseController.CONTRIBUTION:
        break; // TODO-505: write handling
      case BaseController.QUERY:
        // TODO-505: what's the concept?
        break; // TODO-505: write handling
      case BaseController.DEFINITION:
        break; // TODO-505: write handling
      default:
        throw new InternalServerException("ABAC: Invalid type given from Pre- or PostAuthorize");
    }
  }

  private boolean evaluateResponse(HttpResponse<?> response) {
    return response.statusCode() == 200;
  }

  private HttpResponse<?> abacRequest(String url, Map<String, String> bodyMap)
      throws IOException, InterruptedException {
    // convert bodyMap to JSON
    ObjectMapper objectMapper = new ObjectMapper();
    String requestBody = objectMapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(bodyMap);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(BodyPublishers.ofString(requestBody))
        .build();

    return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
  }

  private JwtAuthenticationToken getJwtAuthenticationToken(Authentication auth) {
    JwtAuthenticationToken jwt;
    if (auth instanceof JwtAuthenticationToken) {
      jwt = (JwtAuthenticationToken) auth;
    } else {
      throw new IllegalArgumentException("ABAC: Invalid authentication, no JWT available.");
    }
    return jwt;
  }

  @Override
  public Object getFilterObject() {
    return this.filterObject;
  }

  @Override
  public void setFilterObject(Object filterObject) {
    this.filterObject = filterObject;
  }

  @Override
  public Object getReturnObject() {
    return this.returnObject;
  }

  @Override
  public void setReturnObject(Object returnObject) {
    this.returnObject = returnObject;
  }

  @Override
  public Object getThis() {
    return this;
  }
}
