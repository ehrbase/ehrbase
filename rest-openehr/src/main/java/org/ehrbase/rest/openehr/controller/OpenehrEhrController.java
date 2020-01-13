/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.rest.openehr.controller;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.openehr.controller.OperationNotesResourcesReaderOpenehr.ApiNotes;
import org.ehrbase.rest.openehr.response.EhrResponseData;
import org.ehrbase.rest.openehr.response.EhrResponseDataRepresentation;
import org.ehrbase.rest.openehr.response.InternalResponse;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.support.identification.HierObjectId;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;

/**
 * Controller for /ehr resource of openEHR REST API
 * <p>
 * TODO WIP state only implements endpoints from outer server side, everything else is a stub. Also with a lot of duplication at the moment, which should be reduced when implementing functionality.
 */
@Api(tags = {"EHR"})
@RestController
@RequestMapping(path = "/rest/openehr/v1/ehr", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrEhrController extends BaseController {

    private final EhrService ehrService;

    @Autowired
    public OpenehrEhrController(EhrService ehrService) {

        this.ehrService = Objects.requireNonNull(ehrService);
    }

    @PostMapping//(consumes = {"application/xml", "application/json"})
    @ApiOperation(value = "Create a new EHR with an auto-generated identifier.", response = EhrResponseData.class)
    @ApiNotes("ehrPostPutEhrWithStatus.md")     // TODO this utilizes a workaround, see source class for info
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created - new EHR has been successfully created. The EHR resource is returned in the body when the Prefer header has the value of return=representation. The default for Prefer header (or when Prefer header if missing) is return=minimal. The Location header is always returned.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }),
            @ApiResponse(code = 400, message = "Bad request - Request body (if provided) could not be parsed."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format."),
            @ApiResponse(code = 409, message = "Conflict - Unable to create a new EHR due to a conflict with an already existing EHR with the same subject id, namespace pair.")})
    @ResponseStatus(value = HttpStatus.CREATED)
    // overwrites default 200, fixes the wrong listing of 200 in swagger-ui (EHR-56)
    // TODO auditing headers (openehr*) ignored until auditing is implemented
    public ResponseEntity createEhr(@ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
                                    @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
                                    @ApiParam(value = REQ_CONTENT_TYPE_BODY) @RequestHeader(value = CONTENT_TYPE, required = false) String contentType,    // TODO when working on EHR_STATUS
                                    @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                    @ApiParam(value = "May be used by clients for resource representation negotiation") @RequestHeader(value = PREFER, required = false, defaultValue = RETURN_MINIMAL) String prefer,
                                    @ApiParam(value = "An ehr_status may be supplied as the request body") @RequestBody(required = false) EhrStatus ehrStatus) {
        final UUID ehrId;
        if (ehrStatus != null) {
            ehrId = ehrService.create(ehrStatus, null);
        } else {
            ehrId = ehrService.create(null, null);
        }

        return internalPostEhrProcessing(accept, prefer, ehrId);
    }

    @PutMapping(path = "/{ehr_id}")
    @ApiOperation(value = "Create a new EHR with the specified EHR identifier.", response = EhrResponseData.class)
    @ApiNotes("ehrPostPutEhrWithStatus.md")     // TODO this utilizes a workaround, see source class for info
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created - new EHR has been successfully created. The EHR resource is returned in the body when the Prefer header has the value of return=representation. The default for Prefer header (or when Prefer header if missing) is return=minimal. The Location header is always returned.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }),
            @ApiResponse(code = 400, message = "Bad request - Request body (if provided) or when supplied ehr_id doesn't follow the specification."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format."),
            @ApiResponse(code = 409, message = "Conflict - Unable to create a new EHR due to a conflict with an already existing EHR. Can happen when the supplied ehr_id is already already used by an existing EHR.")})
    @ResponseStatus(value = HttpStatus.CREATED)
    // overwrites default 200, fixes the wrong listing of 200 in swagger-ui (EHR-56)
    public ResponseEntity<EhrResponseData> createEhrWithId(@ApiParam(value = "Optional custom request header for versioning") @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
                                                           @ApiParam(value = "Optional custom request header for auditing") @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
                                                           @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                                           @ApiParam(value = "May be used by clients for resource representation negotiation") @RequestHeader(value = PREFER, required = false) String prefer,
                                                           @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
                                                           @ApiParam(value = "An ehr_status may be supplied as the request body") @RequestBody(required = false) EhrStatus ehrStatus) {
        UUID ehrId = getEhrUuid(ehrIdString);
        if (ehrService.hasEhr(ehrId)) {
            throw new StateConflictException("EHR with this ID already exists");
        }

        final UUID resultEhrId;
        if (ehrStatus != null) {
            resultEhrId = ehrService.create(ehrStatus, ehrId);
        } else {
            resultEhrId = ehrService.create(null, ehrId);
        }

        if (!ehrId.equals(resultEhrId)) {
            throw new InternalServerException("Error creating EHR with custom ID and/or status");
        }

        return internalPostEhrProcessing(accept, prefer, resultEhrId);
    }

    private ResponseEntity<EhrResponseData> internalPostEhrProcessing( String accept, String prefer, UUID resultEhrId) {
        URI url = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + resultEhrId.toString()));

        List<String> headerList = Arrays.asList(CONTENT_TYPE, LOCATION, ETAG, LAST_MODIFIED);   // whatever is required by REST spec

        Optional<InternalResponse<EhrResponseData>> respData;   // variable to overload with more specific object if requested
        if (Optional.ofNullable(prefer).map(i -> i.equals(RETURN_REPRESENTATION)).orElse(false)) {      // null safe way to test prefer header
            respData = buildEhrResponseData(() -> new EhrResponseDataRepresentation(), resultEhrId, accept, headerList);
        } else {    // "minimal" is default fallback
            respData = buildEhrResponseData(() -> new EhrResponseData(), resultEhrId, accept, headerList);
        }

        return respData.map(i -> ResponseEntity.created(url).headers(i.getHeaders()).body(i.getResponseData()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Returns EHR by ID
     */
    @GetMapping(path = "/{ehr_id}")
    @ApiOperation(value = "Retrieve the EHR with the specified ehr_id.", response = EhrResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - EHR resource is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist"),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format."),
            @ApiResponse(code = 415, message = "Unsupported Media Type - Type not supported.")})
    public ResponseEntity<EhrResponseData> retrieveEhrById(@ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                                           @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString) {

        UUID ehrId = getEhrUuid(ehrIdString);

        if(ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        return internalGetEhrProcessing(accept, ehrId);
    }

    /**
     * Returns EHR by subject (id and namespace)
     */
    @GetMapping(params = {"subject_id", "subject_namespace"})
    @ApiOperation(value = "Retrieve the EHR with the specified subject_id and subject_namespace.", response = EhrResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - EHR resource is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with supplied subject parameters does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format."),
            @ApiResponse(code = 415, message = "Unsupported Media Type - Type not supported.")})
    public ResponseEntity<EhrResponseData> retrieveEhrBySubject(@ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                                                @ApiParam(value = "subject id", required = true) @RequestParam(value = "subject_id") String subjectId,
                                                                @ApiParam(value = "subject namespace", required = true) @RequestParam(value = "subject_namespace") String subjectNamespace) {

        Optional<UUID> ehrIdOpt = ehrService.findBySubject(subjectId, subjectNamespace);

        UUID ehrId = ehrIdOpt.orElseThrow(() -> new ObjectNotFoundException("ehr", "No EHR with supplied subject parameters found"));

        return internalGetEhrProcessing(accept, ehrId);
    }

    private ResponseEntity<EhrResponseData> internalGetEhrProcessing(String accept, UUID ehrId) {
        List<String> headerList = Arrays.asList(CONTENT_TYPE, LOCATION, ETAG, LAST_MODIFIED);   // whatever is required by REST spec

        Optional<InternalResponse<EhrResponseData>> respData;   // variable to overload with more specific object if requested
        if (Optional.ofNullable(false).map(i -> i.equals(RETURN_REPRESENTATION)).orElse(false)) {      // null safe way to test prefer header
            respData = buildEhrResponseData(() -> new EhrResponseDataRepresentation(), ehrId, accept, headerList);
        } else {    // "minimal" is default fallback
            respData = buildEhrResponseData(() -> new EhrResponseData(), ehrId, accept, headerList);
        }

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
    private <T extends EhrResponseData> Optional<InternalResponse<T>> buildEhrResponseData(Supplier<T> factory, UUID ehrId, /*Action create,*/ String accept, List<String> headerList) {
        // check for valid format header to produce content accordingly
        MediaType contentTypeHeaderInput;  // to prepare header input if this header is needed later
        if (StringUtils.isBlank(accept) || accept.equals("*/*")) {  // "*/*" is standard for "any mime-type"
            // assign default if no header was set
            contentTypeHeaderInput = MediaType.APPLICATION_JSON;
        } else {
            // if header was set process it
            MediaType mediaType = MediaType.parseMediaType(accept);

            if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                contentTypeHeaderInput = MediaType.APPLICATION_JSON;
            } else if (mediaType.isCompatibleWith(MediaType.APPLICATION_XML)) {
                contentTypeHeaderInput = MediaType.APPLICATION_XML;
            } else {
                throw new InvalidApiParameterException("Wrong Content-Type header in request");
            }
        }

        //Optional<EhrStatusDto> ehrStatus = ehrService.getEhrStatusEhrScape(ehrId, CompositionFormat.FLAT);    // older, keep until rework of formatting
        Optional<EhrStatus> ehrStatus = ehrService.getEhrStatus(ehrId);
        if (!ehrStatus.isPresent()) {
            return Optional.empty();
        }

        // create either EhrResponseData or EhrResponseDataRepresentation, via lambda request
        T minimalOrRepresentation = factory.get();
        // do steps of "minimal" (EhrResponseData) scope
        minimalOrRepresentation.setEhrId(new HierObjectId(ehrId.toString()));
        minimalOrRepresentation.setEhrStatus(ehrStatus.get());
        minimalOrRepresentation.setSystemId(new HierObjectId(ehrService.getSystemUuid().toString()));
        minimalOrRepresentation.setTimeCreated(ehrService.getCreationTime(ehrId).toString());

        // if response data objects was created as "representation" do all task from wider scope, too
        if (minimalOrRepresentation.getClass().equals(EhrResponseDataRepresentation.class)) {
            // when this "if" is true the following casting can be executed and data manipulated by reference (handled by temporary variable)
            EhrResponseDataRepresentation objByReference = (EhrResponseDataRepresentation) minimalOrRepresentation;
            objByReference.setCompositions(null);    // TODO get actual data from service layer
            objByReference.setContributions(null);   // TODO get actual data from service layer
        }

        // create and supplement headers with data depending on which headers are requested
        HttpHeaders respHeaders = new HttpHeaders();
        for (String header : headerList) {
            switch (header) {
                case CONTENT_TYPE:
                    respHeaders.setContentType(contentTypeHeaderInput);
                    break;
                case LOCATION:
                    try {
                        URI url = new URI(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + minimalOrRepresentation.getEhrId().getValue());
                        respHeaders.setLocation(url);
                    } catch (Exception e) {
                        throw new InternalServerException(e.getMessage());
                    }
                    break;
                case ETAG:
                    respHeaders.setETag("\"" + ehrId + "\"");
                    break;
                case LAST_MODIFIED:
                    // TODO should be VERSION.commit_audit.time_committed.value which is not implemented yet - mock for now
                    respHeaders.setLastModified(123124442);
                    break;
            }
        }

        return Optional.of(new InternalResponse<>(minimalOrRepresentation, respHeaders));
    }
}
