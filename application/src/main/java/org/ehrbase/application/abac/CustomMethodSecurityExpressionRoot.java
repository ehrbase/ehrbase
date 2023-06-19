/*
 * Copyright (c) 2021-2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.application.abac;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.ehrbase.rest.util.AuthHelper.getRequestedJwtClaim;
import static org.springframework.http.HttpStatus.NO_CONTENT;

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
import java.util.stream.Collectors;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.application.abac.AbacConfig.AbacCheck;
import org.ehrbase.application.abac.AbacConfig.AbacType;
import org.ehrbase.application.abac.AbacConfig.Policy;
import org.ehrbase.application.abac.AbacConfig.PolicyParameter;
import org.ehrbase.aql.compiler.AuditVariables;
import org.ehrbase.openehr.sdk.response.dto.OriginalVersionResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.rest.BaseController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Implementation of custom security expression, to be used in e.g. @PreAuthorize(..) to allow ABAC
 * requests.
 *
 * @author Jake Smolka
 * @since 1.0
 */
@SuppressWarnings("unused")
public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot
        implements MethodSecurityExpressionOperations {

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

    public CustomMethodSecurityExpressionRoot(
            Authentication authentication, AbacConfig abacConfig, AbacCheck abacCheck) {
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
     */
    public boolean checkAbacPost(String type, String subject, Object payload, String contentType) throws IOException {
        return checkAbac(type, subject, payload, contentType, POST);
    }

    public boolean checkAbacPostQuery(Object payload) throws IOException {
        return checkAbac(BaseController.QUERY, null, payload, null, POST);
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
     */
    public boolean checkAbacPre(String type, String subject, Object payload, String contentType) throws IOException {
        // @PreAuthorize will give different types, e.g. String (for composition), EhrStatus,...
        // so just pipe it through to templateHandling and make by-type handling there
        return checkAbac(type, subject, payload, contentType, PRE);
    }

    /*
    Short call with less parameters.
     */
    public boolean checkAbacPre(String type, String subject) throws IOException {
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
     */
    private boolean checkAbac(String type, String subject, Object payload, String contentType, String authType)
            throws IOException {
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
                policyParameters =
                        Arrays.asList(policy.get(AbacType.COMPOSITION).getParameters());
                requestUrl = requestUrl.concat(policy.get(AbacType.COMPOSITION).getName());
                break;
            case BaseController.CONTRIBUTION:
                policyParameters =
                        Arrays.asList(policy.get(AbacType.CONTRIBUTION).getParameters());
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
        if ((policyParameters.contains(PolicyParameter.ORGANIZATION)
                        || policyParameters.contains(PolicyParameter.PATIENT)
                        || policyParameters.contains(PolicyParameter.TEMPLATE))
                && requestMap.size() == 0) {
            throw new InternalServerException(
                    "ABAC: Parameters were configured, but request parameters " + "are empty.");
        }

        return abacCheckRequest(requestUrl, requestMap);
    }

    /**
     * Handles organization ID extraction. Uses token's claim.
     * @param token Token
     * @param requestMap ABAC request attribute map to add the result
     */
    private void organizationHandling(AbstractAuthenticationToken token, Map<String, Object> requestMap) {
        String orgId = getRequestedJwtClaim(token, abacConfig.getOrganizationClaim());

        if (isBlank(orgId)) {
            // organization configured but claim not available or empty
            throw new IllegalArgumentException(
                    "ABAC use of an organization claim is configured but can't be retrieved from the given JWT.");
        }

        requestMap.put(ORGANIZATION, orgId);
    }

    /**
     * Extracts the patient claim from the token's claims.
     * @param token Token
     * @return The patient claim
     */
    private String getPatient(AbstractAuthenticationToken token) {
        String tokenPatient = getRequestedJwtClaim(token, abacConfig.getPatientClaim());

        if (isBlank(tokenPatient)) {
            throw new IllegalArgumentException("ABAC: Patient parameter configured, but no claim attribute available.");
        }

        return tokenPatient;
    }

    /**
     * Handles patient ID extraction. Either uses token's claim or EHR's subject.
     * @param token Token
     * @param subject Subject from EHR
     * @param requestMap ABAC request attribute map to add the result
     */
    @SuppressWarnings("unchecked")
    boolean patientHandling(
            AbstractAuthenticationToken token,
            String subject,
            Map<String, Object> requestMap,
            String type,
            Object payload) {

        String tokenPatient = getPatient(token);

        boolean isQuery = type.equals(BaseController.QUERY);

        if (!isQuery && (tokenPatient.equals(subject) || subject == null)) {
            requestMap.put(PATIENT, tokenPatient);
            return true;
        } else if (!isQuery) return false;
        else if (!(payload instanceof Map))
            throw new InternalServerException("ABAC: AQL audit patient data malformed.");
        else {
            if (((Map<?, ?>) payload).containsKey(AuditVariables.EHR_PATH)) {
                Set<UUID> ehrs = (Set<UUID>) ((Map<?, ?>) payload).get(AuditVariables.EHR_PATH);
                List<String> allSubjectExtRefs = ehrService.getSubjectExtRefs(
                        ehrs.stream().map(UUID::toString).collect(Collectors.toList()));
                boolean isValidRefs =
                        allSubjectExtRefs.stream().map(tokenPatient::equals).reduce(true, (b1, b2) -> b1 && b2);

                if (!isValidRefs) return false;

                Set<String> patientSet = new HashSet<>();
                patientSet.add(tokenPatient);
                requestMap.put(PATIENT, patientSet);
                return true;
            } else throw new InternalServerException("ABAC: AQL audit patient data unavailable.");
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
    @SuppressWarnings("unchecked")
    private void templateHandling(
            String type, Object payload, String contentType, Map<String, Object> requestMap, String authType) {
        switch (type) {
            case BaseController.EHR:
                throw new IllegalArgumentException(
                        "ABAC: Unsupported configuration: Can't set template ID for EHR type.");
            case BaseController.EHR_STATUS:
                throw new IllegalArgumentException(
                        "ABAC: Unsupported configuration: Can't set template ID for EHR_STATUS type.");
            case BaseController.COMPOSITION:
                String content = "";
                if (authType.equals(POST)) {
                    // @PostAuthorize gives a ResponseEntity type for "returnObject", so payload is of that type
                    if (!(payload instanceof ResponseEntity<?> responseEntity)) {
                        throw new InternalServerException("ABAC: unexpected payload type");
                    }
                    if (!responseEntity.hasBody()) {
                        if (NO_CONTENT.equals(responseEntity.getStatusCode())) {
                            return;
                        }
                        throw new InternalServerException("ABAC: unexpected empty response body");
                    }
                    Object body = responseEntity.getBody();
                    if (NO_CONTENT.equals(responseEntity.getStatusCode())) {
                        if (body instanceof Map) {
                            Object error = ((Map<?, ?>) body).get("error");
                            if (error != null && ((String) error).contains("delet")) {
                                // composition was deleted, so nothing to check here, skip
                                break;
                            }
                        }
                        throw new InternalServerException("ABAC: Unexpected empty response from composition request");
                    }
                    if (body instanceof OriginalVersionResponseData) {
                        // case of versioned_composition --> fast path, because template is easy to get
                        Object data = ((OriginalVersionResponseData<?>) body).getData();
                        if (data instanceof Composition composition) {
                            String template = Objects.requireNonNull(
                                            composition.getArchetypeDetails().getTemplateId())
                                    .getValue();
                            requestMap.put(TEMPLATE, template);
                            break; // special case, so done here, exit
                        }
                    } else if (body instanceof String) {
                        content = (String) body;
                    } else {
                        throw new InternalServerException("ABAC: unexpected composition payload object");
                    }
                } else if (authType.equals(PRE)) {
                    try {
                        // try if this is the Delete composition case. Payload would contain the UUID of the compo.
                        ObjectVersionId versionId = new ObjectVersionId((String) payload);
                        UUID compositionUid =
                                UUID.fromString(versionId.getRoot().getValue());
                        Optional<Composition> compoDto = compositionService.retrieve(
                                compositionService.getEhrId(compositionUid), compositionUid, null);
                        if (compoDto.isPresent()) {
                            Composition c = compoDto.get();
                            if (c.getArchetypeDetails() != null
                                    && c.getArchetypeDetails().getTemplateId() != null) {
                                requestMap.put(
                                        TEMPLATE,
                                        c.getArchetypeDetails().getTemplateId().getValue());
                            }
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
                        Set<String> templates = (Set<String>) ((Map<?, ?>) payload).get(AuditVariables.TEMPLATE_PATH);
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

    @SuppressWarnings("unchecked")
    private boolean abacCheckRequest(String url, Map<String, Object> bodyMap) throws IOException {
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
     */
    @SuppressWarnings("unchecked")
    private boolean sendRequestForEach(
            String type, String url, Map<String, Object> bodyMap, Map<String, String> request) throws IOException {
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
    private AbstractAuthenticationToken getJwtAuthenticationToken(Authentication auth) {
        if (auth instanceof AbstractAuthenticationToken jwt) {
            return jwt;
        } else {
            throw new IllegalArgumentException("ABAC: Invalid authentication, no JWT available.");
        }
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
