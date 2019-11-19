/*
 * Copyright (c) 2019 Jake Smolka (Hannover Medical School).
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

import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.openehr.response.EhrStatusResponseData;
import org.ehrbase.rest.openehr.response.InternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Supplier;

/**
 * Controller for /ehr/{ehrId}/ehr_status resource of openEHR REST API
 */
@Api(tags = {"EHR_STATUS"})
@RestController
@RequestMapping(path = "/rest/openehr/v1/ehr/{ehr_id}/ehr_status", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrEhrStatusController extends BaseController {

    private final EhrService ehrService;

    @Autowired
    public OpenehrEhrStatusController(EhrService ehrService) {
        this.ehrService = Objects.requireNonNull(ehrService);
    }

    @GetMapping
    @ApiOperation(value = "Retrieves the version of the EHR_STATUS associated with the EHR identified by ehr_id. If version_at_time is supplied, retrieves the version extant at specified time, otherwise retrieves the latest EHR_STATUS version.", response = EhrStatusResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - requested EHR_STATUS resource is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = String.class)
                    }),
            @ApiResponse(code = 400, message = "Bad Request - the request has invalid content such as an invalid version_at_time format."),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist or a version of an EHR_STATUS resource does not exist at the specified version_at_time."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<EhrStatusResponseData> retrieveEhrStatusByTime(
            @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
            @ApiParam(value = "Timestamp in the extended ISO8601 format, e.g. 2015-01-20T19:30:22.765+01:00") @RequestParam(value = "version_at_time", required = false) String versionAtTime) {
        UUID ehrId = getEhrUuid(ehrIdString);

        // timestamp optional, otherwise latest
        int version;
        if (versionAtTime != null) {
            OffsetDateTime time = OffsetDateTime.parse(versionAtTime);
            Timestamp timestamp = Timestamp.valueOf(time.toLocalDateTime());
            version = ehrService.getEhrStatusVersionByTimestamp(ehrId, timestamp);
        } else {
            version = Integer.parseInt(ehrService.getLatestVersionUidOfStatus(ehrId).split("::")[2]);
        }

        UUID statusUid = ehrService.getEhrStatusVersionedObjectUidByEhr(ehrId);

        return internalGetEhrStatusProcessing(accept, ehrId, statusUid, version);
    }

    @GetMapping(path = "/{version_uid}")
    @ApiOperation(value = "Retrieves a particular version of the EHR_STATUS identified by version_uid and associated with the EHR identified by ehr_id.", response = EhrStatusResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - requested EHR_STATUS is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = String.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist or when an EHR_STATUS with version_uid does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<EhrStatusResponseData> retrieveEhrStatusById(
            @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
            @ApiParam(value = "User supplied version UID of EHR_STATUS", required = true) @PathVariable(value = "version_uid") String versionUid) {
        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if(ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        UUID versionedObjectUid = extractVersionedObjectUidFromVersionUid(versionUid);
        int version = extractVersionFromVersionUid(versionUid);

        Optional<EhrStatus> ehrStatus = ehrService.getEhrStatusAtVersion(ehrId, versionedObjectUid, version);

        UUID ehrStatusId = UUID.fromString(ehrStatus.orElseThrow(() -> new ObjectNotFoundException("ehr_status", "EHR_STATUS not found")).getUid().toString());

        return internalGetEhrStatusProcessing(accept, ehrId, ehrStatusId, version);
    }

    private ResponseEntity<EhrStatusResponseData> internalGetEhrStatusProcessing(String accept, UUID ehrId, UUID ehrStatusId, int version) {
        List<String> headerList = Arrays.asList(CONTENT_TYPE, LOCATION, ETAG, LAST_MODIFIED);   // whatever is required by REST spec

        Optional<InternalResponse<EhrStatusResponseData>> respData = buildEhrStatusResponseData(EhrStatusResponseData::new, ehrId, ehrStatusId, version, accept, headerList);

        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @PutMapping
    @ApiOperation(value = "Updates EHR_STATUS associated with the EHR identified by ehr_id. The existing latest version_uid of EHR_STATUS resource (i.e the preceding_version_uid) must be specified in the If-Match header. The response will contain the updated EHR_STATUS resource when the Prefer header has a value of return=representation")
    @OperationNotesResourcesReaderOpenehr.ApiNotes("ehrStatusPut.md")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - EHR_STATUS is successfully updated and the updated resource is returned in the body when Prefer header value is return=representation.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = String.class)
                    }),
            @ApiResponse(code = 204, message = "Not Content - Prefer header is missing or is set to return=minimal."),
            @ApiResponse(code = 400, message = "Bad Request - request has invalid content."),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist."),
            @ApiResponse(code = 412, message = "Precondition Failed - If-Match request header doesn’t match the latest version on the service side. Returns also latest version_uid in the Location and ETag headers."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<EhrStatusResponseData> updateEhrStatus(
            @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @ApiParam(value = REQ_CONTENT_TYPE_BODY) @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            @ApiParam(value = REQ_PREFER) @RequestHeader(value = PREFER, required = false) String prefer,
            @ApiParam(value = "{preceding_version_uid}", required = true) @RequestHeader(value = IF_MATCH) String ifMatch,
            @ApiParam(value = "EHR ID", required = true) @PathVariable("ehr_id") String ehrIdString,
            @ApiParam(value = "EHR status.", required = true) @RequestBody() EhrStatus ehrStatus) {
        UUID ehrId = getEhrUuid(ehrIdString);

        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException("EHR", "EHR with this ID not found");
        }

        // If-Match header check
        String latestVersionUid = ehrService.getLatestVersionUidOfStatus(ehrId);
        if (!latestVersionUid.equals(ifMatch))
            throw new PreconditionFailedException("Given If-Match header does not match latest existing version");

        // update EHR_STATUS and check for success
        Optional<EhrStatus> updateStatus = ehrService.updateStatus(ehrId, ehrStatus);
        EhrStatus status = updateStatus.orElseThrow(() -> new InvalidApiParameterException("Could not update EHR_STATUS"));

        // update and prepare current version number
        String newLatestVersionUid = ehrService.getLatestVersionUidOfStatus(ehrId);
        String[] split = latestVersionUid.split("::");
        if (latestVersionUid.equals(newLatestVersionUid) || split.length != 3)
            throw new InvalidApiParameterException("Update of EHR_STATUS failed");
        int version = Integer.parseInt(split[split.length-1]) + 1;

        List<String> headerList = Arrays.asList(CONTENT_TYPE, LOCATION, ETAG, LAST_MODIFIED);   // whatever is required by REST spec
        Optional<InternalResponse<EhrStatusResponseData>> respData;   // variable to overload with more specific object if requested
        if (Optional.ofNullable(prefer).map(i -> i.equals(RETURN_REPRESENTATION)).orElse(false)) {      // null safe way to test prefer header
            respData = buildEhrStatusResponseData(EhrStatusResponseData::new, ehrId, UUID.fromString(status.getUid().getRoot().getValue()), version, accept, headerList);
        } else {    // "minimal" is default fallback
            respData = buildEhrStatusResponseData(EhrStatusResponseData::new, ehrId, UUID.fromString(status.getUid().getRoot().getValue()), version, accept, headerList);
        }

        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Builder method to prepare appropriate HTTP response. Flexible to either allow minimal or full representation of resource.
     *
     * @param factory       Lambda function to constructor of desired object
     * @param ehrId         Ehr reference
     * @param ehrStatusId   EhrStatus versioned object ID
     * @param version       EhrStatus version number
     * @param accept        Requested content format
     * @param headerList    Requested headers that need to be set
     * @param <T>           Either only header response or specific class EhrStatusResponseData
     * @return
     */
    private <T extends EhrStatusResponseData> Optional<InternalResponse<T>> buildEhrStatusResponseData(Supplier<T> factory, UUID ehrId, UUID ehrStatusId, int version, String accept, List<String> headerList) {
        // create either EhrStatusResponseData or null (means no body, only headers incl. link to resource), via lambda request
        T minimalOrRepresentation = factory.get();

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

        if (minimalOrRepresentation != null) {
            // when this "if" is true the following casting can be executed and data manipulated by reference (handled by temporary variable)
            EhrStatusResponseData objByReference = (EhrStatusResponseData) minimalOrRepresentation;

            Optional<EhrStatus> ehrStatus = ehrService.getEhrStatusAtVersion(ehrId, ehrStatusId, version);
            if (ehrStatus.isPresent()) {
                objByReference.setArchetypeNodeId(ehrStatus.get().getArchetypeNodeId());
                objByReference.setName(ehrStatus.get().getName());
                objByReference.setUid(new ObjectVersionId(ehrStatus.get().getUid().toString() + "::" + ehrService.getServerConfig().getNodename() + "::" + version));
                objByReference.setSubject(ehrStatus.get().getSubject());
                objByReference.setOtherDetails(ehrStatus.get().getOtherDetails());
                objByReference.setModifiable(ehrStatus.get().isModifiable());
                objByReference.setQueryable(ehrStatus.get().isQueryable());
            } else {
                return Optional.empty();
            }
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
                        URI url = new URI(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + ehrId + "/ehr_status/" + ehrStatusId + "::" + ehrService.getServerConfig().getNodename() + "::" + version);
                        respHeaders.setLocation(url);
                    } catch (Exception e) {
                        throw new InternalServerException(e.getMessage());
                    }
                    break;
                case ETAG:
                    respHeaders.setETag("\"" + ehrStatusId + "::" + ehrService.getServerConfig().getNodename() + "::" + version + "\"");
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
