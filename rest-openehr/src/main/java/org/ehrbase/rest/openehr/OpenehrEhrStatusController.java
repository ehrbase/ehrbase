/*
 * Copyright (c) 2019-2022 vitasystems GmbH and Hannover Medical School.
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

import static org.apache.commons.lang3.StringUtils.unwrap;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.EhrStatus;
import java.net.URI;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.audit.msg.AuditMsgBuilder;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.openehr.sdk.response.dto.EhrStatusResponseData;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.EhrStatusApiSpecification;
import org.ehrbase.rest.util.InternalResponse;
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

/**
 * Controller for /ehr/{ehrId}/ehr_status resource of openEHR REST API
 *
 * @author Jake Smolka
 * @author Renaud Subiger
 * @since 1.0
 */
@TenantAware
@RestController
@RequestMapping(path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/ehr/{ehr_id}/ehr_status")
public class OpenehrEhrStatusController extends BaseController implements EhrStatusApiSpecification {

    private final EhrService ehrService;

    public OpenehrEhrStatusController(EhrService ehrService) {
        this.ehrService = ehrService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_READ_STATUS)
    @PreAuthorize("checkAbacPre(@openehrEhrStatusController.EHR_STATUS, @ehrService.getSubjectExtRef(#ehrId))")
    public ResponseEntity<EhrStatusResponseData> getEhrStatusVersionByTime(
            @PathVariable(name = "ehr_id") UUID ehrId,
            @RequestParam(name = "version_at_time", required = false) String versionAtTime,
            @RequestHeader(name = HttpHeaders.ACCEPT, required = false) String accept) {

        assertEhrExists(ehrId);

        // timestamp optional, otherwise latest
        int version;
        if (versionAtTime != null) {
            OffsetDateTime time = OffsetDateTime.parse(versionAtTime);
            Timestamp timestamp = Timestamp.valueOf(time.toLocalDateTime());
            version = ehrService.getEhrStatusVersionByTimestamp(ehrId, timestamp);
        } else {
            version = Integer.parseInt(
                    ehrService.getLatestVersionUidOfStatus(ehrId).split("::")[2]);
        }

        UUID statusUid = ehrService.getEhrStatusVersionedObjectUidByEhr(ehrId);

        return internalGetEhrStatusProcessing(accept, ehrId, statusUid, version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping(path = "/{version_uid}")
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_READ_STATUS)
    @PreAuthorize("checkAbacPre(@openehrEhrStatusController.EHR_STATUS, @ehrService.getSubjectExtRef(#ehrId))")
    public ResponseEntity<EhrStatusResponseData> getEhrStatusByVersionId(
            @PathVariable(name = "ehr_id") UUID ehrId,
            @PathVariable(name = "version_uid") String versionUid,
            @RequestHeader(name = HttpHeaders.ACCEPT, required = false) String accept) {

        assertEhrExists(ehrId);

        UUID versionedObjectUid = extractVersionedObjectUidFromVersionUid(versionUid);
        int version = extractVersionFromVersionUid(versionUid);

        Optional<OriginalVersion<EhrStatus>> ehrStatus =
                ehrService.getEhrStatusAtVersion(ehrId, versionedObjectUid, version);

        UUID ehrStatusId = extractVersionedObjectUidFromVersionUid(ehrStatus
                .orElseThrow(() -> new ObjectNotFoundException("ehr_status", "EHR_STATUS not found"))
                .getUid()
                .toString());

        return internalGetEhrStatusProcessing(accept, ehrId, ehrStatusId, version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_UPDATE_STATUS)
    @PreAuthorize("checkAbacPre(@openehrEhrStatusController.EHR_STATUS, @ehrService.getSubjectExtRef(#ehrId))")
    public ResponseEntity<EhrStatusResponseData> updateEhrStatus(
            @PathVariable("ehr_id") UUID ehrId,
            @RequestHeader(name = IF_MATCH) String versionUid,
            @RequestHeader(name = PREFER, required = false) String prefer,
            @RequestHeader(name = HttpHeaders.ACCEPT, required = false) String accept,
            @RequestHeader(name = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            @RequestBody EhrStatus ehrStatus) {

        assertEhrExists(ehrId);

        // If-Match header check
        String latestVersionUid = ehrService.getLatestVersionUidOfStatus(ehrId);
        versionUid = unwrap(versionUid, '"');
        if (!latestVersionUid.equals(versionUid)) {
            throw new PreconditionFailedException("Given If-Match header does not match latest existing version");
        }

        // update EHR_STATUS and check for success
        UUID statusUid = ehrService.updateStatus(ehrId, ehrStatus, null, null);

        // update and prepare current version number
        String newLatestVersionUid = ehrService.getLatestVersionUidOfStatus(ehrId);
        String[] split = latestVersionUid.split("::");
        if (latestVersionUid.equals(newLatestVersionUid) || split.length != 3) {
            throw new InvalidApiParameterException("Update of EHR_STATUS failed");
        }
        int version = Integer.parseInt(split[split.length - 1]) + 1;

        List<String> headerList =
                Arrays.asList(CONTENT_TYPE, LOCATION, ETAG, LAST_MODIFIED); // whatever is required by REST spec
        Optional<InternalResponse<EhrStatusResponseData>>
                respData; // variable to overload with more specific object if requested

        respData =
                buildEhrStatusResponseData(EhrStatusResponseData::new, ehrId, statusUid, version, accept, headerList);

        createAuditLogsMsgBuilder(ehrId);

        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Assert that an EHR with the given ID exists.
     *
     * @param ehrId the EHR ID to check
     * @throws ObjectNotFoundException if the EHR does not exist
     */
    private void assertEhrExists(UUID ehrId) {
        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException("EHR", "The EHR with the specified id does not exist");
        }
    }

    private ResponseEntity<EhrStatusResponseData> internalGetEhrStatusProcessing(
            String accept, UUID ehrId, UUID ehrStatusId, int version) {
        List<String> headerList =
                Arrays.asList(CONTENT_TYPE, LOCATION, ETAG, LAST_MODIFIED); // whatever is required by REST spec

        Optional<InternalResponse<EhrStatusResponseData>> respData =
                buildEhrStatusResponseData(EhrStatusResponseData::new, ehrId, ehrStatusId, version, accept, headerList);

        createAuditLogsMsgBuilder(ehrId);

        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    private void createAuditLogsMsgBuilder(UUID ehrId) {
        AuditMsgBuilder.getInstance().setEhrIds(ehrId);
    }

    /**
     * Builder method to prepare appropriate HTTP response. Flexible to either allow minimal or full
     * representation of resource.
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
    private <T extends EhrStatusResponseData> Optional<InternalResponse<T>> buildEhrStatusResponseData(
            Supplier<T> factory, UUID ehrId, UUID ehrStatusId, int version, String accept, List<String> headerList) {
        // create either EhrStatusResponseData or null (means no body, only headers incl. link to resource), via lambda
        // request
        T minimalOrRepresentation = factory.get();

        // check for valid format header to produce content accordingly
        MediaType contentType = resolveContentType(accept); // to prepare header input if this header is needed later

        Optional<OriginalVersion<EhrStatus>> ehrStatus = ehrService.getEhrStatusAtVersion(ehrId, ehrStatusId, version);
        if (minimalOrRepresentation != null) {
            // when this "if" is true the following casting can be executed and data manipulated by reference (handled
            // by temporary variable)
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
                        URI url = createLocationUri(
                                EHR,
                                ehrId.toString(),
                                EHR_STATUS,
                                String.format(
                                        "%s::%s::%s",
                                        ehrStatusId,
                                        ehrService.getServerConfig().getNodename(),
                                        version));
                        respHeaders.setLocation(url);
                    } catch (Exception e) {
                        throw new InternalServerException(e.getMessage());
                    }
                    break;
                case ETAG:
                    respHeaders.setETag("\"" + ehrStatusId + "::"
                            + ehrService.getServerConfig().getNodename() + "::" + version + "\"");
                    break;
                case LAST_MODIFIED:
                    ehrStatus.ifPresent(ehrStatusOriginalVersion -> respHeaders.setLastModified(ehrStatusOriginalVersion
                            .getCommitAudit()
                            .getTimeCommitted()
                            .getMagnitude()));
                    break;
                default:
                    // Ignore header
            }
        }

        return Optional.of(new InternalResponse<>(minimalOrRepresentation, respHeaders));
    }
}
