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
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.response.ehrscape.CompositionDto;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.openehr.OriginalVersionResponseData;
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
  private CompositionService compositionService;
  private ContributionService contributionService;
  private Object filterObject;
  private Object returnObject;

  public CustomMethodSecurityExpressionRoot(Authentication authentication,
      AbacConfig abacConfig) {
    super(authentication);
    this.abacConfig = abacConfig;
  }

  public void setCompositionService(CompositionService compositionService) {
    this.compositionService = compositionService;
  }

  public void setContributionService(ContributionService contributionService) {
    this.contributionService = contributionService;
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
  public boolean checkAbacPost(String type, Authentication auth, String subject, Object payload,
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
  public boolean checkAbacPre(String type, Authentication auth, String subject, Object payload,
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
  private boolean checkAbac(String type, Authentication auth, String subject, Object payload,
      String contentType, String authType) throws IOException, InterruptedException {
    // Set type specific settings:
    // Extract and set parameters according to which parameters are configured
    List<String> policyParameters;
    // Build abac server request, depending on type
    String requestUrl = abacConfig.getServer().toString();
    switch (type) {
      case BaseController.EHR:
        policyParameters = new ArrayList<>(Arrays.asList(abacConfig.getPolicyEhrParameters()));
        requestUrl = requestUrl.concat(abacConfig.getPolicyEhrName());
        break;
      case BaseController.EHR_STATUS:
        policyParameters = new ArrayList<>(Arrays.asList(abacConfig.getPolicyEhrStatusParameters()));
        requestUrl = requestUrl.concat(abacConfig.getPolicyEhrStatusName());
        break;
      case BaseController.COMPOSITION:
        policyParameters = new ArrayList<>(Arrays.asList(abacConfig.getPolicyCompositionParameters()));
        requestUrl = requestUrl.concat(abacConfig.getPolicyCompositionName());
        break;
      case BaseController.CONTRIBUTION:
        policyParameters = new ArrayList<>(Arrays.asList(abacConfig.getPolicyContributionParameters()));
        requestUrl = requestUrl.concat(abacConfig.getPolicyContributionName());
        break;
      case BaseController.QUERY:
        policyParameters = new ArrayList<>(Arrays.asList(abacConfig.getPolicyQueryParameters()));
        requestUrl = requestUrl.concat(abacConfig.getPolicyQueryName());
        break;
      default:
        throw new InternalServerException("ABAC: Invalid type given from Pre- or PostAuthorize");
    }

    // Check and extract JWT
    JwtAuthenticationToken jwt = getJwtAuthenticationToken(auth);

    // Request body map. will result in simple JSON like {"patient_id":"...", ...}
    // but requires "Object" for template handling, which can have a Set<String> for multiple IDs
    Map<String, Object> requestMap = new HashMap<>();

    // Organization attribute handling
    if (policyParameters.contains(ORGANIZATION)) {
      organizationHandling(jwt, requestMap);
    }

    // Patient attribute handling
    if (policyParameters.contains(PATIENT)) {
      patientHandling(jwt, subject, requestMap);
    }

    // Extract template ID from object of type "type"
    if (policyParameters.contains(TEMPLATE)) {
      templateHandling(type, payload, contentType, requestMap, authType);
    }

    // Fire abac server request
    return abacCheckRequest(requestUrl, requestMap);
  }

  /**
   * Handles organization ID extraction. Uses token's claim.
   * @param jwt Token
   * @param requestMap ABAC request attribute map to add the result
   */
  private void organizationHandling(JwtAuthenticationToken jwt, Map<String, Object> requestMap) {
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
  private void patientHandling(JwtAuthenticationToken jwt, String subject,
      Map<String, Object> requestMap) {
    if (!jwt.getTokenAttributes().containsKey(abacConfig.getPatientClaim())) {
      // "patient_id" not available, use EHRbase subject as fallback
      requestMap.put(PATIENT, subject);
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
   */
  private void templateHandling(String type, Object payload, String contentType, Map<String,
      Object> requestMap, String authType) {
    switch (type) {
      case BaseController.EHR:
        throw new IllegalArgumentException("ABAC: Unsupported configuration: Can't set template ID for EHR type.");
      case BaseController.EHR_STATUS:
        throw new IllegalArgumentException("ABAC: Unsupported configuration: Can't set template ID for EHR_STATUS type.");
      case BaseController.COMPOSITION:
        String content = "";
        if (authType.equals(POST)) {
          // @PostAuthorize gives a ResponseEntity type for "returnObject", so payload is of that type
          if (((ResponseEntity) payload).hasBody()) {
            Object body = ((ResponseEntity) payload).getBody();
            if (body instanceof OriginalVersionResponseData) {
              // case of versioned_composition --> fast path, because template is easy to get
              if (((OriginalVersionResponseData<?>) body).getData() instanceof Composition) {
                String template = Objects.requireNonNull(
                    ((Composition) ((OriginalVersionResponseData<?>) body).getData())
                        .getArchetypeDetails().getTemplateId()).getValue();
                requestMap.put(TEMPLATE, template);
                break;  // special case, so done here, exit
              }
            } else if (body instanceof String) {
              content = (String) body;
            } else {
              throw new InternalServerException("ABAC: unexpected composition payload object");
            }
          } else {
            throw new InternalServerException("ABAC: unexpected empty response body");
          }
        } else if (authType.equals(PRE)) {
          try {
            // try if this is the Delete composition case. Payload would contain the UUID of the compo.
            ObjectVersionId versionId = new ObjectVersionId((String) payload);
            UUID compositionUid = UUID.fromString(versionId.getRoot().getValue());
            Optional<CompositionDto> compoDto = compositionService.retrieve(compositionUid, null);
            if (compoDto.isPresent()) {
              Composition c = compoDto.get().getComposition();
              requestMap.put(TEMPLATE, c.getArchetypeDetails().getTemplateId().getValue());
              break; // special case, so done here, exit
            } else {
              throw new InternalServerException(
                  "ABAC: unexpected empty response from composition delete");
            }
          } catch (IllegalArgumentException e) {
            // if not an UUID, the payload is a composition itself so continue
            content = (String) payload;
          }
        } else {
          throw new InternalServerException("ABAC: invalid auth type given.");
        }
        String templateId;
        if (MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON)) {
          templateId = compositionService.getTemplateIdFromInputComposition(content, CompositionFormat.JSON);
        } else if (MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_XML)) {
          templateId = compositionService.getTemplateIdFromInputComposition(content, CompositionFormat.XML);
        } else {
          throw new IllegalArgumentException("ABAC: Only JSON and XML composition are supported.");
        }
        requestMap.put(TEMPLATE, templateId);
        break;
      case BaseController.CONTRIBUTION:
        CompositionFormat format;
        if (MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON)) {
          format = CompositionFormat.JSON;
        } else if (MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_XML)) {
          format = CompositionFormat.XML;
        } else {
          throw new IllegalArgumentException("ABAC: Only JSON and XML composition are supported.");
        }
        if (payload instanceof String) {
          Set<String> templates = contributionService.getListOfTemplates((String) payload, format);
          requestMap.put(TEMPLATE, templates);
          break;
        } else {
          throw new InternalServerException("ABAC: invalid POST contribution payload.");
        }
      case BaseController.QUERY:
        // TODO-505: what's the concept?
        break; // TODO-505: write handling
      default:
        throw new InternalServerException("ABAC: Invalid type given from Pre- or PostAuthorize");
    }
  }

  private boolean abacCheckRequest(String url, Map<String, Object> bodyMap)
      throws IOException, InterruptedException {
    // prepare request attributes and convert from <String, Object> to <String, String>
    Map<String, String> request = new HashMap<>();
    if (bodyMap.containsKey(PATIENT)) {
      request.put(PATIENT, (String) bodyMap.get(PATIENT));
    }
    if (bodyMap.containsKey(ORGANIZATION)) {
      request.put(ORGANIZATION, (String) bodyMap.get(ORGANIZATION));
    }
    // check if template attributes are available and see if it contains a Set
    if (bodyMap.containsKey(TEMPLATE)) {
      if (bodyMap.get(TEMPLATE) instanceof Set) {
        // set each template and send separate ABAC requests
        Set<String> set = (Set<String>) bodyMap.get(TEMPLATE);
        for (String s : set) {
          request.put(TEMPLATE, s);
          boolean allowed = evaluateResponse(abacRequest(url, request));
          if (!allowed) {
            // if only one combination of attributes is rejected by ABAC return false for all
            return false;
          }
        }
        // in case all combinations were validated successfully
        return true;
      } else if (bodyMap.get(TEMPLATE) instanceof String) {
        // if it is just a String, set it and continue normal
        request.put(TEMPLATE, (String) bodyMap.get(TEMPLATE));
      } else {
        throw new InternalServerException("ABAC: Invalid template attribute content.");
      }
    }
    return evaluateResponse(abacRequest(url, request));
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

  private boolean evaluateResponse(HttpResponse<?> response) {
    return response.statusCode() == 200;
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
