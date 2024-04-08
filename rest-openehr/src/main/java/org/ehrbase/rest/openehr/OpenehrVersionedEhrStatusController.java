/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.rest.openehr;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.audit.msg.AuditMsgBuilder;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.openehr.sdk.response.dto.OriginalVersionResponseData;
import org.ehrbase.openehr.sdk.response.dto.RevisionHistoryResponseData;
import org.ehrbase.openehr.sdk.response.dto.VersionedObjectResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.ContributionDto;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.VersionedEhrStatusApiSpecification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Controller for /ehr/{ehrId}/versioned_ehr_status resource of openEHR REST API
 */
@ConditionalOnMissingBean(name = "primaryopenehrversionedehrstatuscontroller")
@RestController
@RequestMapping(
        path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/ehr/{ehr_id}/versioned_ehr_status",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrVersionedEhrStatusController extends BaseController implements VersionedEhrStatusApiSpecification {

    private static final String REVISION_HISTORY = "revision_history";

    private final ContributionService contributionService;

    private final EhrService ehrService;

    public OpenehrVersionedEhrStatusController(EhrService ehrService, ContributionService contributionService) {
        this.ehrService = Objects.requireNonNull(ehrService);
        this.contributionService = Objects.requireNonNull(contributionService);
    }

    @GetMapping
    @Override
    public ResponseEntity<VersionedObjectResponseData<EhrStatus>> retrieveVersionedEhrStatusByEhr(
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept) {

        UUID ehrId = getEhrUuid(ehrIdString);

        VersionedEhrStatus versionedEhrStatus = ehrService.getVersionedEhrStatus(ehrId);

        VersionedObjectResponseData<EhrStatus> response = new VersionedObjectResponseData<>(versionedEhrStatus);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        createAuditLogsMsgBuilder(ehrId);

        return ResponseEntity.ok().headers(respHeaders).body(response);
    }

    @GetMapping(path = "/revision_history")
    @Override
    public ResponseEntity<RevisionHistoryResponseData> retrieveVersionedEhrStatusRevisionHistoryByEhr(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString) {

        UUID ehrId = getEhrUuid(ehrIdString);

        RevisionHistory revisionHistory = ehrService.getRevisionHistoryOfVersionedEhrStatus(ehrId);

        RevisionHistoryResponseData response = new RevisionHistoryResponseData(revisionHistory);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        createAuditLogsMsgBuilder(ehrId, REVISION_HISTORY);

        return ResponseEntity.ok().headers(respHeaders).body(response);
    }

    @GetMapping(path = "/version")
    @Override
    public ResponseEntity<OriginalVersionResponseData<EhrStatus>> retrieveVersionOfEhrStatusByTime(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestParam(value = "version_at_time", required = false) String versionAtTime) {

        UUID ehrId = getEhrUuid(ehrIdString);

        final ObjectVersionId objectVersionId;

        if (versionAtTime != null) {

            OffsetDateTime time = decodeVersionAtTime(versionAtTime).orElseThrow();
            objectVersionId = ehrService.getEhrStatusVersionByTimestamp(ehrId, time);

        } else {

            objectVersionId = ehrService.getLatestVersionUidOfStatus(ehrId);
        }

        int version = extractVersionFromVersionUid(objectVersionId.getValue()).orElseThrow();
        UUID statusUid = extractVersionedObjectUidFromVersionUid(objectVersionId.getValue());

        Optional<OriginalVersion<EhrStatus>> ehrStatusOriginalVersion =
                ehrService.getEhrStatusAtVersion(ehrId, statusUid, version);
        UUID contributionId = ehrStatusOriginalVersion
                .map(i -> UUID.fromString(i.getContribution().getId().getValue()))
                .orElseThrow(() -> new ObjectNotFoundException(
                        RmConstants.EHR_STATUS, "Couldn't retrieve EhrStatus with given parameters"));

        ContributionDto contributionDto = contributionService.getContribution(ehrId, contributionId);

        OriginalVersionResponseData<EhrStatus> originalVersionResponseData =
                new OriginalVersionResponseData<>(ehrStatusOriginalVersion.orElseThrow(), contributionDto);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        createAuditLogsMessageBuilder(ehrId, new ImmutablePair<>("version_at_time", versionAtTime), "version")
                .setVersion(version);

        return ResponseEntity.ok().headers(respHeaders).body(originalVersionResponseData);
    }

    @GetMapping(path = "/version/{version_uid}")
    @Override
    public ResponseEntity<OriginalVersionResponseData<EhrStatus>> retrieveVersionOfEhrStatusByVersionUid(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "version_uid") String versionUid) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException(RmConstants.EHR, "No EHR with this ID can be found.");
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
                .orElseThrow(() -> new ObjectNotFoundException(
                        RmConstants.EHR_STATUS, "Couldn't retrieve EhrStatus with given parameters"));

        ContributionDto contributionDto = contributionService.getContribution(ehrId, contributionId);

        OriginalVersionResponseData<EhrStatus> originalVersionResponseData =
                new OriginalVersionResponseData<>(ehrStatusOriginalVersion.orElseThrow(), contributionDto);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        createAuditLogsMsgBuilder(
                        ehrId,
                        "version",
                        originalVersionResponseData.getVersionId().toString())
                .setVersion(version);

        return ResponseEntity.ok().headers(respHeaders).body(originalVersionResponseData);
    }

    private AuditMsgBuilder createAuditLogsMsgBuilder(UUID ehrId, String... pathSegments) {
        return createAuditLogsMessageBuilder(ehrId, new ImmutablePair<>("", ""), pathSegments);
    }

    private AuditMsgBuilder createAuditLogsMessageBuilder(
            UUID ehrId, Pair<String, String> queryParam, String... pathSegments) {
        UriComponentsBuilder uriComponentsBuilder = fromPath("")
                .pathSegment(EHR, ehrId.toString(), VERSIONED_EHR_STATUS)
                .pathSegment(pathSegments);

        if (isNotBlank(queryParam.getKey()) && isNotBlank(queryParam.getValue()))
            uriComponentsBuilder.queryParam(queryParam.getKey(), queryParam.getValue());

        return AuditMsgBuilder.getInstance()
                .setEhrIds(ehrId)
                .setLocation(uriComponentsBuilder.build().toString());
    }
}
