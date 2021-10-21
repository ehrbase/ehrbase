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

package org.ehrbase.rest.openehr;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.EhrStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.response.openehr.EhrStatusResponseData;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.EhrStatusApiSpecification;
import org.ehrbase.rest.util.InternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Controller for /ehr/{ehrId}/ehr_status resource of openEHR REST API
 */
@RestController
@RequestMapping(path = "${openehr-api.context-path:/rest/openehr}/v1/ehr/{ehr_id}/ehr_status", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrEhrStatusController extends BaseController implements EhrStatusApiSpecification {

    private final EhrService ehrService;

    @Autowired
    public OpenehrEhrStatusController(EhrService ehrService) {
        this.ehrService = Objects.requireNonNull(ehrService);
    }

    @GetMapping
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PreAuthorize("checkAbacPre(@openehrEhrStatusController.EHR_STATUS, "
            + "@ehrService.getSubjectExtRef(#ehrIdString))")
    @Override
    public ResponseEntity<EhrStatusResponseData> retrieveEhrStatusByTime(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestParam(value = "version_at_time", required = false) String versionAtTime) {
        UUID ehrId = getEhrUuid(ehrIdString);

        if (ehrService.hasEhr(ehrId).equals(Boolean.FALSE))
            throw new ObjectNotFoundException("EHR", "No EHR with id " + ehrId + " found");

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
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PreAuthorize("checkAbacPre(@openehrEhrStatusController.EHR_STATUS, "
            + "@ehrService.getSubjectExtRef(#ehrIdString))")
    @Override
    public ResponseEntity<EhrStatusResponseData> retrieveEhrStatusById(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "version_uid") String versionUid) {
        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if (ehrService.hasEhr(ehrId).equals(Boolean.FALSE))
            throw new ObjectNotFoundException("EHR", "No EHR with id " + ehrId + " found");

        UUID versionedObjectUid = extractVersionedObjectUidFromVersionUid(versionUid);
        int version = extractVersionFromVersionUid(versionUid);

        Optional<OriginalVersion<EhrStatus>> ehrStatus = ehrService.getEhrStatusAtVersion(ehrId, versionedObjectUid, version);

        UUID ehrStatusId = extractVersionedObjectUidFromVersionUid(ehrStatus.orElseThrow(() -> new ObjectNotFoundException("ehr_status", "EHR_STATUS not found")).getUid().toString());

        return internalGetEhrStatusProcessing(accept, ehrId, ehrStatusId, version);
    }

    private ResponseEntity<EhrStatusResponseData> internalGetEhrStatusProcessing(String accept, UUID ehrId, UUID ehrStatusId, int version) {
        List<String> headerList = Arrays.asList(CONTENT_TYPE, LOCATION, ETAG, LAST_MODIFIED);   // whatever is required by REST spec

        Optional<InternalResponse<EhrStatusResponseData>> respData = buildEhrStatusResponseData(EhrStatusResponseData::new, ehrId, ehrStatusId, version, accept, headerList);

        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @PutMapping
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PreAuthorize("checkAbacPre(@openehrEhrStatusController.EHR_STATUS, "
            + "@ehrService.getSubjectExtRef(#ehrIdString))")
    @Override
    public ResponseEntity<EhrStatusResponseData> updateEhrStatus(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            @RequestHeader(value = PREFER, required = false) String prefer,
            @RequestHeader(value = IF_MATCH) String ifMatch,
            @PathVariable("ehr_id") String ehrIdString,
            @RequestBody() EhrStatus ehrStatus) {
        UUID ehrId = getEhrUuid(ehrIdString);

        if (ehrService.hasEhr(ehrId).equals(Boolean.FALSE))
            throw new ObjectNotFoundException("EHR", "No EHR with id " + ehrId + " found");

        // If-Match header check
        String latestVersionUid = ehrService.getLatestVersionUidOfStatus(ehrId);
        if (!latestVersionUid.equals(ifMatch))
            throw new PreconditionFailedException("Given If-Match header does not match latest existing version");

        // update EHR_STATUS and check for success
        Optional<EhrStatus> updateStatus = ehrService.updateStatus(ehrId, ehrStatus, null);
        EhrStatus status = updateStatus.orElseThrow(() -> new InvalidApiParameterException("Could not update EHR_STATUS"));

        // update and prepare current version number
        String newLatestVersionUid = ehrService.getLatestVersionUidOfStatus(ehrId);
        String[] split = latestVersionUid.split("::");
        if (latestVersionUid.equals(newLatestVersionUid) || split.length != 3)
            throw new InvalidApiParameterException("Update of EHR_STATUS failed");
        int version = Integer.parseInt(split[split.length - 1]) + 1;

        List<String> headerList = Arrays.asList(CONTENT_TYPE, LOCATION, ETAG, LAST_MODIFIED);   // whatever is required by REST spec
        Optional<InternalResponse<EhrStatusResponseData>> respData;   // variable to overload with more specific object if requested
        UUID statusUid = UUID.fromString(status.getUid().getValue().split("::")[0]);
        if (Optional.ofNullable(prefer).map(i -> i.equals(RETURN_REPRESENTATION)).orElse(false)) {      // null safe way to test prefer header
            respData = buildEhrStatusResponseData(EhrStatusResponseData::new, ehrId, statusUid, version, accept, headerList);
        } else {    // "minimal" is default fallback
            respData = buildEhrStatusResponseData(EhrStatusResponseData::new, ehrId, statusUid, version, accept, headerList);
        }

        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Builder method to prepare appropriate HTTP response. Flexible to either allow minimal or full representation of resource.
     *
     * @param factory     Lambda function to constructor of desired object
     * @param ehrId       Ehr reference
     * @param ehrStatusId EhrStatus versioned object ID
     * @param version     EhrStatus version number
     * @param accept      Requested content format
     * @param headerList  Requested headers that need to be set
     * @param <T>         Either only header response or specific class EhrStatusResponseData
     * @return
     */
    private <T extends EhrStatusResponseData> Optional<InternalResponse<T>> buildEhrStatusResponseData(Supplier<T> factory, UUID ehrId, UUID ehrStatusId, int version, String accept, List<String> headerList) {
        // create either EhrStatusResponseData or null (means no body, only headers incl. link to resource), via lambda request
        T minimalOrRepresentation = factory.get();

        // check for valid format header to produce content accordingly
        MediaType contentType = resolveContentType(accept);  // to prepare header input if this header is needed later

        Optional<OriginalVersion<EhrStatus>> ehrStatus = ehrService.getEhrStatusAtVersion(ehrId, ehrStatusId, version);
        if (minimalOrRepresentation != null) {
            // when this "if" is true the following casting can be executed and data manipulated by reference (handled by temporary variable)
            EhrStatusResponseData objByReference = minimalOrRepresentation;

            if (ehrStatus.isPresent()) {
                objByReference.setArchetypeNodeId(ehrStatus.get().getData().getArchetypeNodeId());
                objByReference.setName(ehrStatus.get().getData().getName());
                objByReference.setUid(ehrStatus.get().getUid());
                objByReference.setSubject(ehrStatus.get().getData().getSubject());
                objByReference.setOtherDetails(ehrStatus.get().getData().getOtherDetails());
                objByReference.setModifiable(ehrStatus.get().getData().isModifiable());
                objByReference.setQueryable(ehrStatus.get().getData().isQueryable());
            } else {
                return Optional.empty();
            }
        }

        // create and supplement headers with data depending on which headers are requested
        HttpHeaders respHeaders = new HttpHeaders();
        for (String header : headerList) {
            switch (header) {
                case CONTENT_TYPE:
                    respHeaders.setContentType(contentType);
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
                    ehrStatus.ifPresent(ehrStatusOriginalVersion -> respHeaders.setLastModified(ehrStatusOriginalVersion.getCommitAudit().getTimeCommitted().getMagnitude()));
                    break;
                default:
                    // Ignore header
            }
        }

        return Optional.of(new InternalResponse<>(minimalOrRepresentation, respHeaders));
    }
}
