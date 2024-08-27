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

import static org.ehrbase.api.rest.HttpRestContext.EHR_ID;
import static org.ehrbase.api.rest.HttpRestContext.VERSION;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.changecontrol.VersionedObject;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.dto.VersionedEhrStatusDto;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.openehr.sdk.response.dto.OriginalVersionResponseData;
import org.ehrbase.openehr.sdk.response.dto.RevisionHistoryResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.ContributionDto;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.VersionedEhrStatusApiSpecification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ResponseEntity<VersionedEhrStatusDto> retrieveVersionedEhrStatusByEhr(
            @PathVariable(value = "ehr_id") String ehrIdString) {

        UUID ehrId = getEhrUuid(ehrIdString);
        createRestContext(ehrId, Map.of());

        VersionedObject<EhrStatusDto> versionedEhrStatus = ehrService.getVersionedEhrStatus(ehrId);
        VersionedEhrStatusDto versionedEhrStatusDto = new VersionedEhrStatusDto(
                versionedEhrStatus.getUid(),
                versionedEhrStatus.getOwnerId(),
                DateTimeFormatter.ISO_DATE_TIME.format(
                        versionedEhrStatus.getTimeCreated().getValue()));

        return ResponseEntity.ok().body(versionedEhrStatusDto);
    }

    @GetMapping(path = "/revision_history")
    @Override
    public ResponseEntity<RevisionHistoryResponseData> retrieveVersionedEhrStatusRevisionHistoryByEhr(
            @PathVariable(value = "ehr_id") String ehrIdString) {

        UUID ehrId = getEhrUuid(ehrIdString);
        RevisionHistory revisionHistory = ehrService.getRevisionHistoryOfVersionedEhrStatus(ehrId);

        createRestContext(ehrId, Map.of(), REVISION_HISTORY);

        return ResponseEntity.ok().body(new RevisionHistoryResponseData(revisionHistory));
    }

    @GetMapping(path = "/version")
    @Override
    public ResponseEntity<OriginalVersionResponseData<EhrStatusDto>> retrieveVersionOfEhrStatusByTime(
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestParam(value = "version_at_time", required = false) String versionAtTime) {

        UUID ehrId = getEhrUuid(ehrIdString);
        ObjectVersionId objectVersionId;
        Map<String, String> contextParams;

        if (versionAtTime != null) {
            OffsetDateTime time = decodeVersionAtTime(versionAtTime).orElseThrow();
            objectVersionId = ehrService.getEhrStatusVersionByTimestamp(ehrId, time);
            contextParams = Map.of("version_at_time", versionAtTime);
        } else {
            objectVersionId = ehrService.getLatestVersionUidOfStatus(ehrId);
            contextParams = Map.of();
        }

        int version = extractVersionFromVersionUid(objectVersionId.getValue()).orElseThrow();
        UUID ehrStatusId = extractVersionedObjectUidFromVersionUid(objectVersionId.getValue());

        return retrieveVersionOfEhrStatus(
                ehrId, ehrStatusId, version, versionId -> createRestContext(ehrId, contextParams, "version"));
    }

    @GetMapping(path = "/version/{version_uid}")
    @Override
    public ResponseEntity<OriginalVersionResponseData<EhrStatusDto>> retrieveVersionOfEhrStatusByVersionUid(
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "version_uid") String versionUid) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // parse given version uid
        UUID ehrStatusId;
        int version;
        try {
            ehrStatusId = extractVersionedObjectUidFromVersionUid(versionUid);
            version = extractVersionFromVersionUid(versionUid)
                    .orElseThrow(() -> new IllegalArgumentException("no version found"));
        } catch (Exception e) {
            throw new InvalidApiParameterException("VERSION UID parameter has wrong format: " + e.getMessage());
        }

        return retrieveVersionOfEhrStatus(
                ehrId,
                ehrStatusId,
                version,
                versionId -> createRestContext(ehrId, Map.of(), "version", versionId.toString()));
    }

    private ResponseEntity<OriginalVersionResponseData<EhrStatusDto>> retrieveVersionOfEhrStatus(
            UUID ehrId, UUID ehrStatusId, int version, Consumer<ObjectVersionId> initContext) {

        HttpRestContext.register(VERSION, version);

        OriginalVersion<EhrStatusDto> originalVersion = ehrService
                .getEhrStatusAtVersion(ehrId, ehrStatusId, version)
                .orElseThrow(() -> new ObjectNotFoundException(
                        RmConstants.EHR_STATUS, "Couldn't retrieve EhrStatus with given parameters"));

        ObjectRef<? extends ObjectId> contribution = originalVersion.getContribution();
        UUID contributionId = UUID.fromString(contribution.getId().getValue());

        ContributionDto contributionDto = contributionService.getContribution(ehrId, contributionId);

        OriginalVersionResponseData<EhrStatusDto> originalVersionResponseData =
                new OriginalVersionResponseData<>(originalVersion, contributionDto);
        initContext.accept(originalVersionResponseData.getVersionId());

        return ResponseEntity.ok().body(originalVersionResponseData);
    }

    private void createRestContext(UUID ehrId, Map<String, String> queryParams, String... pathSegments) {

        UriComponentsBuilder uriComponentsBuilder = fromPath("")
                .pathSegment(EHR, ehrId.toString(), VERSIONED_EHR_STATUS)
                .pathSegment(pathSegments);

        queryParams.forEach(uriComponentsBuilder::queryParam);

        HttpRestContext.register(
                EHR_ID,
                ehrId,
                HttpRestContext.LOCATION,
                uriComponentsBuilder.build().toString());
    }
}
