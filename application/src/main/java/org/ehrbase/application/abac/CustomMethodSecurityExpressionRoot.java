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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.rest.openehr.controller.BaseController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
  private Object filterObject;
  private Object returnObject;

  public CustomMethodSecurityExpressionRoot(Authentication authentication,
      AbacConfig abacConfig) {
    super(authentication);
    this.abacConfig = abacConfig;
  }

  /**
   * Custom SpEL expression to be used to check if the remote ABAC allows the operation by given
   * data. For @PostAuthorize cases.
   *
   * @param type    Type of scope's resource
   * @param auth    Authentication stored in the security context
   * @param subject Subject ID from the current EHR context
   * @param payload Payload object, either request's input or response's output
   * @param contentType Content type from the scope
   * @return True if ABAC authorizes given attributes
   * @throws IOException On parsing error
   * @throws InterruptedException On error while communicating with the ABAC server
   */
  public boolean checkAbacPost(String type, Authentication auth, UUID subject, Object payload,
      String contentType)
      throws IOException, InterruptedException {

    return checkAbac(type, auth, subject, payload, contentType, POST);
  }

  /**
   * Custom SpEL expression to be used to check if the remote ABAC allows the operation by given
   * data. For @PreAuthorize cases.
   *
   * @param type    Type of scope's resource
   * @param auth    Authentication stored in the security context
   * @param subject Subject ID from the current EHR context
   * @param payload Payload object, either request's input or response's output
   * @param contentType Content type from the scope
   * @return True if ABAC authorizes given attributes
   * @throws IOException On parsing error
   * @throws InterruptedException On error while communicating with the ABAC server
   */
  public boolean checkAbacPre(String type, Authentication auth, UUID subject, Object payload,
      String contentType)
      throws IOException, InterruptedException {

    // @PreAuthorize will give different types, e.g. String (for composition), EhrStatus,...
    // so just pipe it through to templateHandling and make by-type handling there

    return checkAbac(type, auth, subject, payload, contentType, PRE);
  }

  /**
   * Builds the ABAC request with given data and evaluates the ABAC's response.
   * @param type Object type of scope
   * @param auth Authentication stored in the security context
   * @param subject Subject ID from the current EHR context
   * @param payload Payload object, either request's input or response's output
   * @param contentType Content type from the scope
   * @param authType Pre- or PostAuthorize, determines payload style (string or object)
   * @return True if ABAC returns a positive feedback, False if not
   * @throws IOException On parsing error
   * @throws InterruptedException On error while communicating with the ABAC server
   */
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

    // Build abac server request, depending on type
    String requestUrl = abacConfig.getServer().toString();
    switch (type) {
      case BaseController.EHR:
        requestUrl = requestUrl.concat(abacConfig.getPolicyEhrName());
        break;
      case BaseController.EHR_STATUS:
        requestUrl = requestUrl.concat(abacConfig.getPolicyEhrStatusName());
        break;
      case BaseController.COMPOSITION:
        requestUrl = requestUrl.concat(abacConfig.getPolicyCompositionName());
        break;
      case BaseController.DIRECTORY:
        requestUrl = requestUrl.concat(abacConfig.getPolicyDirectoryName());
        break;
      case BaseController.CONTRIBUTION:
        requestUrl = requestUrl.concat(abacConfig.getPolicyContributionName());
        break;
      case BaseController.QUERY:
        requestUrl = requestUrl.concat(abacConfig.getPolicyQueryName());
        break;
      case BaseController.DEFINITION:
        requestUrl = requestUrl.concat(abacConfig.getPolicyDefinitionName());
        break;
      default:
        throw new InternalServerException("ABAC: Invalid type given from Pre- or PostAuthorize");
    }
    // Fire abac server request
    return evaluateResponse(abacRequest(requestUrl, requestMap));
  }

  /**
   * Handles organization ID extraction. Uses token's claim.
   * @param jwt Token
   * @param requestMap ABAC request attribute map to add the result
   */
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

  /**
   * Handles patient ID extraction. Either uses token's claim or EHR's subject.
   * @param jwt Token
   * @param subject Subject from EHR
   * @param requestMap ABAC request attribute map to add the result
   */
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

  /**
   * Handles template ID extraction of specific payload.
   * <p>
   * Payload will be a response body string, in case of @PostAuthorize.
   * <p>
   * Payload will be request body string, or already deserialized object (e.g. EhrStatus), in case of @PreAuthorize.
   * @param type Object type of scope
   * @param payload Payload object, either request's input or response's output
   * @param contentType Content type from the scope
   * @param requestMap ABAC request attribute map to add the result
   * @param authType Pre- or PostAuthorize, determines payload style (string or object)
   * @throws IOException On parsing error
   */
  private void templateHandling(String type, Object payload, String contentType, Map<String,
      String> requestMap, String authType) throws IOException {
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
        String templateId;
        if (MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON)) {
          templateId = extractTemplateIdFromJson(content);
        } else if (MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_XML)) {
          templateId = extractTemplateIdFromXml(content);
        } else {
          throw new IllegalArgumentException("ABAC: Only JSON and XML composition are supported.");
        }
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

  /**
   * Helper to extract the template ID from a XML composition.
   * @param content String of XML composition
   * @return Template ID
   * @throws IOException On parsing error
   */
  private String extractTemplateIdFromXml(String content) throws IOException {
    String templateId = null;
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = builderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new InternalServerException("ABAC: XML parsing failed: ", e);
    }
    try {
      Document document = builder.parse(new ByteArrayInputStream(content.getBytes()));
      Element rootElement = document.getDocumentElement();
      // traverse path - always: archetype_details->template_id->value
      NodeList archetype = rootElement.getElementsByTagName("archetype_details");
      for (int i = 0; i < archetype.getLength(); i++) {
        // check children of archetype_details
        if (archetype.item(i) instanceof Element) {
          NodeList template = ((Element) archetype.item(i)).getElementsByTagName("template_id");
          for (int j = 0; j < template.getLength(); j++) {
            // check children of template_id
            if (template.item(j) instanceof Element) {
              NodeList id = ((Element) template.item(j)).getElementsByTagName("value");
              for (int k = 0; k < id.getLength(); k++) {
                templateId = id.item(k).getTextContent();
              }
            }
          }
        }
      }
      if (templateId == null) {
        throw new IllegalArgumentException("ABAC: Failed to get template ID from composition");
      }
    } catch (SAXException e) {
      throw new IllegalArgumentException("ABAC: Failed to parse XML composition: " + e.getMessage());
    }
    return templateId;
  }

  /**
   * Helper to extract the template ID from a JSON composition.
   * @param content String of JSON composition
   * @return Template ID
   * @throws JsonProcessingException On parsing error
   */
  private String extractTemplateIdFromJson(String content) throws JsonProcessingException {
    String templateId;
    // extract template ID from JSON composition
    ObjectMapper mapper = new ObjectMapper();
    JsonNode actualObj = mapper.readTree(content);
    templateId = actualObj.get("archetype_details").get("template_id").get("value").asText();
    return templateId;
  }

  private boolean evaluateResponse(HttpResponse<?> response) {
    return response.statusCode() == 200;
  }

  /**
   * Helper to build and send the actual HTTP request to the ABAC server.
   * @param url URL for ABAC server request
   * @param bodyMap Map of attributes for the request
   * @return HTTP response
   * @throws IOException On error during attribute or HTTP handling
   * @throws InterruptedException On error during HTTP handling
   */
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
