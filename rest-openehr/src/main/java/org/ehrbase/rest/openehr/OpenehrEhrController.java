/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.rest.openehr;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.support.identification.HierObjectId;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.response.openehr.EhrResponseData;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.audit.OpenEhrAuditInterceptor;
import org.ehrbase.rest.openehr.specification.EhrApiSpecification;
import org.ehrbase.rest.util.InternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for /ehr resource of openEHR REST API
 */
@TenantAware
@RestController
@RequestMapping(
        path = "${openehr-api.context-path:/rest/openehr}/v1/ehr",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrEhrController extends BaseController implements EhrApiSpecification {

    private final EhrService ehrService;

    @Autowired
    public OpenehrEhrController(EhrService ehrService) {

        this.ehrService = Objects.requireNonNull(ehrService);
    }

    @PostMapping // (consumes = {"application/xml", "application/json"})
    @ResponseStatus(value = HttpStatus.CREATED)
    // TODO auditing headers (openehr*) ignored until auditing is implemented
    @Override
    public ResponseEntity createEhr(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
            @RequestHeader(value = CONTENT_TYPE, required = false)
                    String contentType, // TODO when working on EHR_STATUS
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @RequestHeader(value = PREFER, required = false, defaultValue = RETURN_MINIMAL) String prefer,
            @RequestBody(required = false) EhrStatus ehrStatus,
            HttpServletRequest request) {
        final UUID ehrId;
        if (ehrStatus != null) {
            ehrId = ehrService.create(null, ehrStatus);
        } else {
            ehrId = ehrService.create(null, null);
        }

        return internalPostEhrProcessing(accept, prefer, ehrId, request);
    }

    @PutMapping(path = "/{ehr_id}")
    @ResponseStatus(value = HttpStatus.CREATED)
    @Override
    public ResponseEntity<EhrResponseData> createEhrWithId(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @RequestHeader(value = PREFER, required = false) String prefer,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestBody(required = false) EhrStatus ehrStatus,
            HttpServletRequest request) {

        UUID ehrId; // can't use getEhrUuid(..) because here another exception needs to be thrown (-> 400, not 404 in
        // response)
        try {
            ehrId = UUID.fromString(ehrIdString);
        } catch (IllegalArgumentException e) {
            throw new InvalidApiParameterException("EHR ID format not a UUID");
        }

        if (ehrService.hasEhr(ehrId)) {
            throw new StateConflictException("EHR with this ID already exists");
        }

        final UUID resultEhrId;
        if (ehrStatus != null) {
            resultEhrId = ehrService.create(ehrId, ehrStatus);
        } else {
            resultEhrId = ehrService.create(ehrId, null);
        }

        if (!ehrId.equals(resultEhrId)) {
            throw new InternalServerException("Error creating EHR with custom ID and/or status");
        }

        return internalPostEhrProcessing(accept, prefer, resultEhrId, request);
    }

    private ResponseEntity<EhrResponseData> internalPostEhrProcessing(
            String accept, String prefer, UUID resultEhrId, HttpServletRequest request) {
        URI url = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + resultEhrId.toString()));

        List<String> headerList =
                Arrays.asList(CONTENT_TYPE, LOCATION, ETAG, LAST_MODIFIED); // whatever is required by REST spec

        Optional<InternalResponse<EhrResponseData>>
                respData; // variable to overload with more specific object if requested
        if (Optional.ofNullable(prefer)
                .map(i -> i.equals(RETURN_REPRESENTATION))
                .orElse(false)) { // null safe way to test prefer header
            respData = buildEhrResponseData(EhrResponseData::new, resultEhrId, accept, headerList);
        } else { // "minimal" is default fallback
            respData = buildEhrResponseData(() -> null, resultEhrId, accept, headerList);
        }

        // Enriches request attributes with current EhrId for later audit processing
        request.setAttribute(OpenEhrAuditInterceptor.EHR_ID_ATTRIBUTE, Collections.singleton(resultEhrId));

        // returns 201 with body + headers, 204 only with headers or 500 error depending on what processing above yields
        return respData.map(i -> Optional.ofNullable(i.getResponseData())
                        .map(j -> ResponseEntity.created(url)
                                .headers(i.getHeaders())
                                .body(j))
                        // when the body is empty
                        .orElse(ResponseEntity.noContent()
                                .headers(i.getHeaders())
                                .build()))
                // when no response could be created at all
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Returns EHR by ID
     */
    @GetMapping(path = "/{ehr_id}")
    @PreAuthorize("checkAbacPre(@openehrEhrController.EHR, @ehrService.getSubjectExtRef(#ehrIdString))")
    @Override
    public ResponseEntity<EhrResponseData> retrieveEhrById(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            HttpServletRequest request) {

        UUID ehrId = getEhrUuid(ehrIdString);

        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        return internalGetEhrProcessing(accept, ehrId, request);
    }

    /**
     * Returns EHR by subject (id and namespace)
     */
    @GetMapping(params = {"subject_id", "subject_namespace"})
    @PreAuthorize("checkAbacPre(@openehrEhrController.EHR, #subjectId)")
    @Override
    public ResponseEntity<EhrResponseData> retrieveEhrBySubject(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @RequestParam(value = "subject_id") String subjectId,
            @RequestParam(value = "subject_namespace") String subjectNamespace,
            HttpServletRequest request) {

        Optional<UUID> ehrIdOpt = ehrService.findBySubject(subjectId, subjectNamespace);

        UUID ehrId = ehrIdOpt.orElseThrow(
                () -> new ObjectNotFoundException("ehr", "No EHR with supplied subject parameters found"));

        return internalGetEhrProcessing(accept, ehrId, request);
    }

    private ResponseEntity<EhrResponseData> internalGetEhrProcessing(
            String accept, UUID ehrId, HttpServletRequest request) {
        List<String> headerList =
                Arrays.asList(CONTENT_TYPE, LOCATION, ETAG, LAST_MODIFIED); // whatever is required by REST spec

        Optional<InternalResponse<EhrResponseData>> respData =
                buildEhrResponseData(EhrResponseData::new, ehrId, accept, headerList);

        // Enriches request attributes with current EhrId for later audit processing
        request.setAttribute(OpenEhrAuditInterceptor.EHR_ID_ATTRIBUTE, Collections.singleton(ehrId));

        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Builder method to prepare appropriate HTTP response. Flexible to either allow minimal or full representation of resource.
     *
     * @param factory    Lambda function to constructor of desired object
     * @param ehrId      Current object's reference
     * @param accept     Requested content format
     * @param headerList Requested headers that need to be set
     * @param <T>        Either EhrResponseData itself or more specific sub-class EhrResponseDataRepresentation
     * @return
     */
    private <T extends EhrResponseData> Optional<InternalResponse<T>> buildEhrResponseData(
            Supplier<T> factory, UUID ehrId, /*Action create,*/ String accept, List<String> headerList) {
        // check for valid format header to produce content accordingly
        MediaType contentType = resolveContentType(accept);

        // Optional<EhrStatusDto> ehrStatus = ehrService.getEhrStatusEhrScape(ehrId,
        // CompositionFormat.FLAT);    // older, keep until rework of formatting
        Optional<EhrStatus> ehrStatus = Optional.of(ehrService.getEhrStatus(ehrId));
        if (ehrStatus.isEmpty()) {
            return Optional.empty();
        }

        // create either null or maximum response data class
        T minimalOrRepresentation = factory.get();

        if (minimalOrRepresentation != null) {
            // populate maximum response data
            EhrResponseData objByReference = minimalOrRepresentation;
            objByReference.setEhrId(new HierObjectId(ehrId.toString()));
            objByReference.setEhrStatus(ehrStatus.get());
            objByReference.setSystemId(
                    new HierObjectId(ehrService.getSystemUuid().toString()));
            DvDateTime timeCreated = ehrService.getCreationTime(ehrId);
            objByReference.setTimeCreated(timeCreated.getValue().toString());
            // objByReference.setCompositions(null);    // TODO get actual data from service layer
            // objByReference.setContributions(null);   // TODO get actual data from service layer
        }

        // create and supplement headers with data depending on which headers are requested
        HttpHeaders respHeaders = new HttpHeaders();
        for (String header : headerList) {
            switch (header) {
                case CONTENT_TYPE:
                    if (minimalOrRepresentation != null) // if response is going to have a body
                    respHeaders.setContentType(contentType);
                    break;
                case LOCATION:
                    try {
                        URI url = new URI(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + ehrId);
                        respHeaders.setLocation(url);
                    } catch (Exception e) {
                        throw new InternalServerException(e.getMessage());
                    }
                    break;
                case ETAG:
                    respHeaders.setETag("\"" + ehrId + "\"");
                    break;
                case LAST_MODIFIED:
                    // TODO should be VERSION.commit_audit.time_committed.value which is not implemented yet - mock for
                    // now
                    respHeaders.setLastModified(123124442);
                    break;
                default:
                    // Ignore header
            }
        }

        return Optional.of(new InternalResponse<>(minimalOrRepresentation, respHeaders));
    }
}
