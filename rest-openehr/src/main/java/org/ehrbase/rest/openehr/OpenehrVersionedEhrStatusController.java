/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.RevisionHistory;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.audit.msg.AuditMsgBuilder;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.openehr.sdk.response.dto.OriginalVersionResponseData;
import org.ehrbase.openehr.sdk.response.dto.RevisionHistoryResponseData;
import org.ehrbase.openehr.sdk.response.dto.VersionedObjectResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.ContributionDto;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.VersionedEhrStatusApiSpecification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for /ehr/{ehrId}/versioned_ehr_status resource of openEHR REST API
 */
@TenantAware
@RestController
@RequestMapping(
        path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/ehr/{ehr_id}/versioned_ehr_status",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrVersionedEhrStatusController extends BaseController implements VersionedEhrStatusApiSpecification {

    private final ContributionService contributionService;

    private final EhrService ehrService;

    public OpenehrVersionedEhrStatusController(EhrService ehrService, ContributionService contributionService) {
        this.ehrService = Objects.requireNonNull(ehrService);
        this.contributionService = Objects.requireNonNull(contributionService);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_READ_STATUS)
    @GetMapping
    @Override
    public ResponseEntity<VersionedObjectResponseData<EhrStatus>> retrieveVersionedEhrStatusByEhr(
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        VersionedEhrStatus versionedEhrStatus = ehrService.getVersionedEhrStatus(ehrId);

        VersionedObjectResponseData<EhrStatus> response = new VersionedObjectResponseData<>(versionedEhrStatus);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        createAuditLogsMsgBuilder(ehrId);

        return ResponseEntity.ok().headers(respHeaders).body(response);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_READ_STATUS)
    @GetMapping(path = "/revision_history")
    @Override
    public ResponseEntity<RevisionHistoryResponseData> retrieveVersionedEhrStatusRevisionHistoryByEhr(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        RevisionHistory revisionHistory = ehrService.getRevisionHistoryOfVersionedEhrStatus(ehrId);

        RevisionHistoryResponseData response = new RevisionHistoryResponseData(revisionHistory);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        createAuditLogsMsgBuilder(ehrId);

        return ResponseEntity.ok().headers(respHeaders).body(response);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_READ_STATUS)
    @GetMapping(path = "/version")
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PreAuthorize("checkAbacPre(@openehrVersionedEhrStatusController.EHR_STATUS, "
            + "@ehrService.getSubjectExtRef(#ehrIdString), null, null)")
    @Override
    public ResponseEntity<OriginalVersionResponseData<EhrStatus>> retrieveVersionOfEhrStatusByTime(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestParam(value = "version_at_time", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime versionAtTime) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        UUID versionedObjectId = ehrService.getEhrStatusVersionedObjectUidByEhr(ehrId);
        int version;
        if (versionAtTime != null) {
            version = ehrService.getEhrStatusVersionByTimestamp(ehrId, Timestamp.valueOf(versionAtTime));
        } else {
            version = Integer.parseInt(
                    ehrService.getLatestVersionUidOfStatus(ehrId).split("::")[2]);
        }

        Optional<OriginalVersion<EhrStatus>> ehrStatusOriginalVersion =
                ehrService.getEhrStatusAtVersion(ehrId, versionedObjectId, version);
        UUID contributionId = ehrStatusOriginalVersion
                .map(i -> UUID.fromString(i.getContribution().getId().getValue()))
                .orElseThrow(
                        () -> new InvalidApiParameterException("Couldn't retrieve EhrStatus with given parameters"));

        Optional<ContributionDto> optionalContributionDto = contributionService.getContribution(ehrId, contributionId);
        ContributionDto contributionDto = optionalContributionDto.orElseThrow(() ->
                new InternalServerException("Couldn't fetch contribution for existing EhrStatus")); // shouldn't happen

        OriginalVersionResponseData<EhrStatus> originalVersionResponseData =
                new OriginalVersionResponseData<>(ehrStatusOriginalVersion.get(), contributionDto);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        createAuditLogsMsgBuilder(ehrId).setVersion(version);

        return ResponseEntity.ok().headers(respHeaders).body(originalVersionResponseData);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_READ_STATUS)
    @GetMapping(path = "/version/{version_uid}")
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PreAuthorize("checkAbacPre(@openehrVersionedEhrStatusController.EHR_STATUS, "
            + "@ehrService.getSubjectExtRef(#ehrIdString), null, null)")
    @Override
    public ResponseEntity<OriginalVersionResponseData<EhrStatus>> retrieveVersionOfEhrStatusByVersionUid(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "version_uid") String versionUid) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found.");
        }

        // parse given version uid
        UUID versionedObjectId;
        int version;
        try {
            versionedObjectId = UUID.fromString(versionUid.split("::")[0]);
            version = Integer.parseInt(versionUid.split("::")[2]);
        } catch (Exception e) {
            throw new InvalidApiParameterException("VERSION UID parameter has wrong format: " + e.getMessage());
        }

        if (version < 1) throw new InvalidApiParameterException("Version can't be negative.");

        Optional<OriginalVersion<EhrStatus>> ehrStatusOriginalVersion =
                ehrService.getEhrStatusAtVersion(ehrId, versionedObjectId, version);
        UUID contributionId = ehrStatusOriginalVersion
                .map(i -> UUID.fromString(i.getContribution().getId().getValue()))
                .orElseThrow(
                        () -> new InvalidApiParameterException("Couldn't retrieve EhrStatus with given parameters"));

        Optional<ContributionDto> optionalContributionDto = contributionService.getContribution(ehrId, contributionId);
        ContributionDto contributionDto = optionalContributionDto.orElseThrow(() ->
                new InternalServerException("Couldn't fetch contribution for existing EhrStatus")); // shouldn't happen

        OriginalVersionResponseData<EhrStatus> originalVersionResponseData =
                new OriginalVersionResponseData<>(ehrStatusOriginalVersion.get(), contributionDto);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        createAuditLogsMsgBuilder(ehrId).setVersion(version);

        return ResponseEntity.ok().headers(respHeaders).body(originalVersionResponseData);
    }

    private AuditMsgBuilder createAuditLogsMsgBuilder(UUID ehrId) {
        return AuditMsgBuilder.getInstance().setEhrIds(ehrId);
    }
}
