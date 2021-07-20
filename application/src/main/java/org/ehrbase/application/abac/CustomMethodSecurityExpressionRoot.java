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

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.application.abac.AbacConfig.AbacCheck;
import org.ehrbase.application.abac.AbacConfig.AbacType;
import org.ehrbase.application.abac.AbacConfig.Policy;
import org.ehrbase.application.abac.AbacConfig.PolicyParameter;
import org.ehrbase.aql.compiler.AuditVariables;
import org.ehrbase.response.ehrscape.CompositionDto;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.openehr.OriginalVersionResponseData;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.OpenehrQueryController;
import org.springframework.http.HttpStatus;
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
  private final AbacCheck abacCheck;
  private CompositionService compositionService;
  private ContributionService contributionService;
  private EhrService ehrService;
  private Object filterObject;
  private Object returnObject;

  public CustomMethodSecurityExpressionRoot(Authentication authentication,
      AbacConfig abacConfig, AbacCheck abacCheck) {
    super(authentication);
    this.abacConfig = abacConfig;
    this.abacCheck = abacCheck;
  }

  public void setCompositionService(CompositionService compositionService) {
    this.compositionService = compositionService;
  }

  public void setContributionService(ContributionService contributionService) {
    this.contributionService = contributionService;
  }

  public void setEhrService(EhrService ehrService) {
    this.ehrService = ehrService;
  }

  /**
   * Custom SpEL expression to be used to check if the remote ABAC allows the operation by given
   * data. For @PostAuthorize cases.
   *
   * @param type    Type of scope's resource
   * @param subject Subject ID from the current EHR context
   * @param payload Payload object, either request's input or response's output
   * @param contentType Content type from the scope
   * @return True if ABAC authorizes given attributes
   * @throws IOException On parsing error
   * @throws InterruptedException On error while communicating with the ABAC server
   */
  public boolean checkAbacPost(String type, String subject, Object payload,
      String contentType)
      throws IOException, InterruptedException {

    return checkAbac(type, subject, payload, contentType, POST);
  }

  public boolean checkAbacPostQuery(Object payload)
      throws IOException, InterruptedException {

    return checkAbac(OpenehrQueryController.QUERY, null, payload, null, POST);
  }

  /**
   * Custom SpEL expression to be used to check if the remote ABAC allows the operation by given
   * data. For @PreAuthorize cases.
   *
   * @param type    Type of scope's resource
   * @param subject Subject ID from the current EHR context
   * @param payload Payload object, either request's input or response's output
   * @param contentType Content type from the scope
   * @return True if ABAC authorizes given attributes
   * @throws IOException On parsing error
   * @throws InterruptedException On error while communicating with the ABAC server
   */
  public boolean checkAbacPre(String type, String subject, Object payload,
      String contentType)
      throws IOException, InterruptedException {

    // @PreAuthorize will give different types, e.g. String (for composition), EhrStatus,...
    // so just pipe it through to templateHandling and make by-type handling there

    return checkAbac(type, subject, payload, contentType, PRE);
  }

  /*
  Short call with less parameters.
   */
  public boolean checkAbacPre(String type, String subject)
      throws IOException, InterruptedException {

    return checkAbac(type, subject, null, null, PRE);
  }

  /**
   * Builds the ABAC request with given data and evaluates the ABAC's response.
   * @param type Object type of scope
   * @param subject Subject ID from the current EHR context
   * @param payload Payload object, either request's input or response's output
   * @param contentType Content type from the scope
   * @param authType Pre- or PostAuthorize, determines payload style (string or object)
   * @return True if ABAC returns a positive feedback, False if not
   * @throws IOException On parsing error
   * @throws InterruptedException On error while communicating with the ABAC server
   */
  private boolean checkAbac(String type, String subject, Object payload,
      String contentType, String authType) throws IOException, InterruptedException {
    // Set type specific settings:
    // Extract and set parameters according to which parameters are configured
    List<PolicyParameter> policyParameters;
    // Build abac server request, depending on type
    var requestUrl = abacConfig.getServer().toString();

    Map<AbacType, Policy> policy = abacConfig.getPolicy();

    switch (type) {
      case BaseController.EHR:
        policyParameters = Arrays.asList(policy.get(AbacType.EHR).getParameters());
        requestUrl = requestUrl.concat(policy.get(AbacType.EHR).getName());
        break;
      case BaseController.EHR_STATUS:
        policyParameters = Arrays.asList(policy.get(AbacType.EHR_STATUS).getParameters());
        requestUrl = requestUrl.concat(policy.get(AbacType.EHR_STATUS).getName());
        break;
      case BaseController.COMPOSITION:
        policyParameters = Arrays.asList(policy.get(AbacType.COMPOSITION).getParameters());
        requestUrl = requestUrl.concat(policy.get(AbacType.COMPOSITION).getName());
        break;
      case BaseController.CONTRIBUTION:
        policyParameters = Arrays.asList(policy.get(AbacType.CONTRIBUTION).getParameters());
        requestUrl = requestUrl.concat(policy.get(AbacType.CONTRIBUTION).getName());
        break;
      case BaseController.QUERY:
        policyParameters = Arrays.asList(policy.get(AbacType.QUERY).getParameters());
        requestUrl = requestUrl.concat(policy.get(AbacType.QUERY).getName());
        break;
      default:
        throw new InternalServerException("ABAC: Invalid type given from Pre- or PostAuthorize");
    }

    // Check and extract JWT
    var jwt = getJwtAuthenticationToken(this.authentication);

    // Request body map. will result in simple JSON like {"patient_id":"...", ...}
    // but requires "Object" for template handling, which can have a Set<String> for multiple IDs
    Map<String, Object> requestMap = new HashMap<>();

    // Organization attribute handling
    if (policyParameters.contains(PolicyParameter.ORGANIZATION)) {
      organizationHandling(jwt, requestMap);
    }

    // Patient attribute handling
    if (policyParameters.contains(PolicyParameter.PATIENT)) {
      // populate requestMap, but also already check if subject from token and request matches
      boolean patientMatch = patientHandling(jwt, subject, requestMap, type, payload);
      if (!patientMatch) {
        // doesn't match -> requesting data for patient X with token for patient Y
        return false;
      }
    }

    // Extract template ID from object of type "type"
    if (policyParameters.contains(PolicyParameter.TEMPLATE)) {
      templateHandling(type, payload, contentType, requestMap, authType);
    }

    // Final check, if request would be empty even though params were configured to be used
    if ((policyParameters.contains(PolicyParameter.ORGANIZATION) ||
        policyParameters.contains(PolicyParameter.PATIENT) ||
        policyParameters.contains(PolicyParameter.TEMPLATE))
        && requestMap.size() == 0) {
      throw new InternalServerException("ABAC: Parameters were configured, but request parameters "
          + "are empty.");
    }

    return abacCheckRequest(requestUrl, requestMap);
  }

  /**
   * Handles organization ID extraction. Uses token's claim.
   * @param jwt Token
   * @param requestMap ABAC request attribute map to add the result
   */
  private void organizationHandling(JwtAuthenticationToken jwt, Map<String, Object> requestMap) {
    if (jwt.getTokenAttributes().containsKey(abacConfig.getOrganizationClaim())) {
      String orgaId = (String) jwt.getTokenAttributes().get(abacConfig.getOrganizationClaim());
      requestMap.put(ORGANIZATION, orgaId);
    } else {
      // organization configured but claim not available
      throw new IllegalArgumentException("ABAC use of an organization claim is configured but "
          + "can't be retrieved from the given JWT.");
    }
  }

  /**
   * Handles patient ID extraction. Either uses token's claim or EHR's subject.
   * @param jwt Token
   * @param subject Subject from EHR
   * @param requestMap ABAC request attribute map to add the result
   */
  private boolean patientHandling(JwtAuthenticationToken jwt, String subject,
      Map<String, Object> requestMap, String type, Object payload) {

    if (!jwt.getTokenAttributes().containsKey(abacConfig.getPatientClaim())) {
      throw new IllegalArgumentException("ABAC: Patient parameter configured, but no claim "
          + "attribute available.");
    }
    String tokenPatient = (String) jwt.getTokenAttributes().get(abacConfig.getPatientClaim());

    if (type.equals(BaseController.QUERY)) {
      // special case of type QUERY, where multiple subjects are possible
      if (payload instanceof Map) {
        if (((Map<?, ?>) payload).containsKey(AuditVariables.EHR_PATH)) {
          Set<UUID> ehrs = (Set) ((Map<?, ?>) payload).get(AuditVariables.EHR_PATH);
          Set<String> patientSet = new HashSet<>();
          for (UUID ehr : ehrs) {
            String subjectId = ehrService.getSubjectExtRef(ehr.toString());
            // check if patient token is available and if it matches OR internal reference is null
            if (tokenPatient.equals(subjectId) || subjectId == null) {
              // matches OR EHR's external ref is null, so add our subject from token
              patientSet.add(tokenPatient);
            } else {
              // doesn't match -> requesting data for patient X with token for patient Y
              return false;
            }

          }
          // put result set into the requestMap and exit
          requestMap.put(PATIENT, patientSet);
          return true;
        } else {
          throw new InternalServerException("ABAC: AQL audit patient data unavailable.");
        }
      } else {
        throw new InternalServerException("ABAC: AQL audit patient data malformed.");
      }
    }

    // in all other cases just handle the one String "subject" variable
    // check if matches (to block accessing patient X with token from patient Y) OR null reference
    if (tokenPatient.equals(subject) || subject != null) {
      // matches OR EHR's external ref is null, so add our subject from token
      requestMap.put(PATIENT, tokenPatient);
    } else {
      // doesn't match -> requesting data for patient X with token for patient Y
      return false;
    }

    return true;
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
            // can have "No content" here (even with some data in the body) if the compo was (logically) deleted
            if (((ResponseEntity<?>) payload).getStatusCode().equals(HttpStatus.NO_CONTENT)) {
              if (body instanceof Map) {
                Object error = ((Map<?, ?>) body).get("error");
                if (error != null) {
                  if (((String) error).contains("delet")) {
                    //composition was deleted, so nothing to check here, skip
                    break;
                  }
                }
              }
              throw new InternalServerException("ABAC: Unexpected empty response from composition reuquest");
            }
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
        // special case of type QUERY, where multiple subjects are possible
        if (payload instanceof Map) {
          if (((Map<?, ?>) payload).containsKey(AuditVariables.TEMPLATE_PATH)) {
            Set<String> templates = (Set) ((Map<?, ?>) payload).get(AuditVariables.TEMPLATE_PATH);
            Set<String> templateSet = new HashSet<>(templates);
            // put result set into the requestMap and exit
            requestMap.put(TEMPLATE, templateSet);
            break;
          } else {
            throw new InternalServerException("ABAC: AQL audit template data unavailable.");
          }
        } else {
          throw new InternalServerException("ABAC: AQL audit template data malformed.");
        }
      default:
        throw new InternalServerException("ABAC: Invalid type given from Pre- or PostAuthorize");
    }
  }

  private boolean abacCheckRequest(String url, Map<String, Object> bodyMap)
      throws IOException, InterruptedException {
    // prepare request attributes and convert from <String, Object> to <String, String>
    Map<String, String> request = new HashMap<>();
    if (bodyMap.containsKey(ORGANIZATION)) {
      request.put(ORGANIZATION, (String) bodyMap.get(ORGANIZATION));
    }
    // check if patient attribues are available and see if it contains a Set or simple String
    if (bodyMap.containsKey(PATIENT)) {
      if (bodyMap.get(PATIENT) instanceof Set) {
        // check if templates are also configured
        if (bodyMap.containsKey(TEMPLATE)) {
          if (bodyMap.get(TEMPLATE) instanceof Set) {
            // multiple templates possible: need cartesian product of n patients and m templates
            // so: for each patient, go through templates and do a request each
            Set<String> setP = (Set<String>) bodyMap.get(PATIENT);
            for (String p : setP) {
              request.put(PATIENT, p);
              boolean success = sendRequestForEach(TEMPLATE, url, bodyMap, request);
              if (!success) {
                return false;
              }
            }
            // in case all combinations were validated successfully
            return true;
          }
        } else {
          // only patients (or + orga) set. So run request for each patient, without template.
          return sendRequestForEach(PATIENT, url, bodyMap, request);
        }
      } else if (bodyMap.get(PATIENT) instanceof String) {
        request.put(PATIENT, (String) bodyMap.get(PATIENT));
      } else {
        // if it is just a String, set it and continue normal
        throw new InternalServerException("ABAC: Invalid patient attribute content.");
      }
    }
    // check if template attributes are available and see if it contains a Set or simple String
    if (bodyMap.containsKey(TEMPLATE)) {
      if (bodyMap.get(TEMPLATE) instanceof Set) {
        // set each template and send separate ABAC requests
        return sendRequestForEach(TEMPLATE, url, bodyMap, request);
      } else if (bodyMap.get(TEMPLATE) instanceof String) {
        // if it is just a String, set it and continue normal
        request.put(TEMPLATE, (String) bodyMap.get(TEMPLATE));
      } else {
        throw new InternalServerException("ABAC: Invalid template attribute content.");
      }
    }
    return abacCheck.execute(url, request);
  }

  /**
   * Goes through all template IDs and sends an ABAC request for each.
   * @param type Type, either ORGANIZATION, TEMPLATE, PATIENT
   * @param url ABAC server request URL
   * @param bodyMap Unprocessed attributes for the request
   * @param request Processed attributes for the request
   * @return True on success, False if one combinations is rejected by the ABAC server
   * @throws IOException On error during attribute or HTTP handling
   * @throws InterruptedException On error during HTTP handling
   */
  private boolean sendRequestForEach(String type, String url, Map<String, Object> bodyMap,
      Map<String, String> request) throws IOException, InterruptedException {
    Set<String> set = (Set<String>) bodyMap.get(type);
    for (String s : set) {
      request.put(type, s);
      boolean allowed = abacCheck.execute(url, request);
      if (!allowed) {
        // if only one combination of attributes is rejected by ABAC return false for all
        return false;
      }
    }
    // in case all combinations were validated successfully
    return true;
  }

  /**
   * Extracts the JWT auth token.
   * @param auth Auth object.
   * @return JWT Auth Token
   */
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
