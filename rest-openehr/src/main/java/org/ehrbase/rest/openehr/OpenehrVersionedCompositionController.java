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
import static org.ehrbase.api.rest.HttpRestContext.TEMPLATE_ID;
import static org.ehrbase.rest.BaseController.API_CONTEXT_PATH_WITH_VERSION;
import static org.ehrbase.rest.BaseController.VERSIONED_COMPOSITION;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.dto.VersionedCompositionDto;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.sdk.response.dto.OriginalVersionResponseData;
import org.ehrbase.openehr.sdk.response.dto.RevisionHistoryResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.ContributionDto;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.VersionedCompositionApiSpecification;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Controller for /ehr/{ehrId}/versioned_composition resource of openEHR REST API
 */
@ConditionalOnMissingBean(name = "primaryopenehrversionedcompositioncontroller")
@RestController
@RequestMapping(
        path = API_CONTEXT_PATH_WITH_VERSION + "/ehr/{ehr_id}/" + VERSIONED_COMPOSITION,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrVersionedCompositionController extends BaseController
        implements VersionedCompositionApiSpecification {

    private final EhrService ehrService;
    private final CompositionService compositionService;
    private final ContributionService contributionService;

    private final SystemService systemService;

    @Autowired
    public OpenehrVersionedCompositionController(
            EhrService ehrService,
            CompositionService compositionService,
            ContributionService contributionService,
            SystemService systemService) {
        this.ehrService = Objects.requireNonNull(ehrService);
        this.compositionService = Objects.requireNonNull(compositionService);
        this.contributionService = Objects.requireNonNull(contributionService);
        this.systemService = systemService;
    }

    @GetMapping(path = "/{versioned_object_uid}")
    @Override
    public ResponseEntity<VersionedCompositionDto> retrieveVersionedCompositionByVersionedObjectUid(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID versionedCompoUid = getCompositionVersionedObjectUidString(versionedObjectUid);

        // check if parameters are valid
        checkForValidEhrAndCompositionParameter(ehrId, versionedCompoUid);

        VersionedComposition versionedComposition =
                compositionService.getVersionedComposition(ehrId, versionedCompoUid);

        VersionedCompositionDto versionedCompositionDto = new VersionedCompositionDto(
                versionedComposition.getUid(),
                versionedComposition.getOwnerId(),
                DateTimeFormatter.ISO_DATE_TIME.format(
                        versionedComposition.getTimeCreated().getValue()));

        String auditLocation = getLocationUrl(versionedCompoUid, ehrId, 0);
        createRestContext(ehrId, versionedCompoUid, auditLocation);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        return ResponseEntity.ok().headers(respHeaders).body(versionedCompositionDto);
    }

    @GetMapping(path = "/{versioned_object_uid}/revision_history")
    @Override
    public ResponseEntity<RevisionHistoryResponseData> retrieveVersionedCompositionRevisionHistoryByEhr(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID versionedCompoUid = getCompositionVersionedObjectUidString(versionedObjectUid);

        // check if parameters are valid
        checkForValidEhrAndCompositionParameter(ehrId, versionedCompoUid);

        RevisionHistory revisionHistory =
                compositionService.getRevisionHistoryOfVersionedComposition(ehrId, versionedCompoUid);

        RevisionHistoryResponseData response = new RevisionHistoryResponseData(revisionHistory);

        String auditLocation = getLocationUrl(versionedCompoUid, ehrId, 0, "revision_history");
        createRestContext(ehrId, versionedCompoUid, auditLocation);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        return ResponseEntity.ok().headers(respHeaders).body(response);
    }

    @GetMapping(path = "/{versioned_object_uid}/version/{version_uid}")
    @Override
    public ResponseEntity<OriginalVersionResponseData<Composition>> retrieveVersionOfCompositionByVersionUid(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
            @PathVariable(value = "version_uid") String versionUid) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID versionedCompoUid = getCompositionVersionedObjectUidString(versionedObjectUid);

        // check if parameters are valid
        checkForValidEhrAndCompositionParameter(ehrId, versionedCompoUid);

        ObjectVersionId compositionVersionId = new ObjectVersionId(versionUid);
        if (!compositionVersionId.getRoot().getValue().equals(versionedObjectUid)) {
            throw new IllegalArgumentException("Composition parameters are not matching.");
        }

        // parse given version uid
        UUID versionedObjectId;
        int version;
        try {
            versionedObjectId = UUID.fromString(compositionVersionId.getRoot().getValue());
            version = Integer.parseInt(compositionVersionId.getVersionTreeId().getValue());
            if (version <= 0) {
                throw new InvalidApiParameterException(
                        "VERSION UID parameter has wrong format: Version needs to be greater 0");
            }
        } catch (Exception e) {
            throw new InvalidApiParameterException("VERSION UID parameter has wrong format: " + e.getMessage());
        }

        // -----------------

        String auditLocation = getLocationUrl(versionedObjectId, ehrId, version, "version", versionUid);
        createRestContext(ehrId, versionedCompoUid, auditLocation);

        return getOriginalVersionResponseDataResponseEntity(accept, ehrId, versionedObjectId, version);
    }

    @GetMapping(path = "/{versioned_object_uid}/version")
    @Override
    public ResponseEntity<OriginalVersionResponseData<Composition>> retrieveVersionOfCompositionByTime(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
            @RequestParam(value = "version_at_time", required = false) String versionAtTime) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID versionedCompoUid = getCompositionVersionedObjectUidString(versionedObjectUid);

        // check if parameters are valid
        checkForValidEhrAndCompositionParameter(ehrId, versionedCompoUid);

        int version;

        Optional<OffsetDateTime> temporal = decodeVersionAtTime(versionAtTime);
        if (temporal.isPresent()) {
            version = compositionService.getVersionByTimestamp(versionedCompoUid, temporal.get());
        } else {
            version = compositionService.getLastVersionNumber(versionedCompoUid);
        }

        String auditLocation = getLocationUrl(versionedCompoUid, ehrId, version, "version");
        createRestContext(ehrId, versionedCompoUid, auditLocation);

        return getOriginalVersionResponseDataResponseEntity(accept, ehrId, versionedCompoUid, version);
    }

    private void checkForValidEhrAndCompositionParameter(UUID ehrId, UUID versionedCompoUid) {
        // check if EHR is valid
        if (!ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException(RmConstants.EHR, "No EHR with this ID can be found");
        }

        // check if Composition is valid
        if (!compositionService.exists(versionedCompoUid)) {
            throw new ObjectNotFoundException(RmConstants.COMPOSITION, "No composition with this ID can be found.");
        }
    }

    private ResponseEntity<OriginalVersionResponseData<Composition>> getOriginalVersionResponseDataResponseEntity(
            String accept, UUID ehrId, UUID versionedObjectId, int version) {

        Optional<OriginalVersion<Composition>> compositionOriginalVersion =
                compositionService.getOriginalVersionComposition(ehrId, versionedObjectId, version);
        UUID contributionId = compositionOriginalVersion
                .map(i -> UUID.fromString(i.getContribution().getId().getValue()))
                .orElseThrow(() -> new ObjectNotFoundException(
                        RmConstants.ORIGINAL_VERSION,
                        "No VERSIONED_COMPOSITION with given id: %s and version: %d"
                                .formatted(versionedObjectId, version)));

        ContributionDto contributionDto = contributionService.getContribution(ehrId, contributionId);

        OriginalVersionResponseData<Composition> originalVersionResponseData = new OriginalVersionResponseData<>(
                compositionOriginalVersion.orElseThrow(() ->
                        new InternalServerException("Composition exists but can't be retrieved as Original Version.")),
                contributionDto);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        return ResponseEntity.ok().headers(respHeaders).body(originalVersionResponseData);
    }

    private void createRestContext(UUID ehrId, UUID versionedCompoUid, String auditLocation) {
        HttpRestContext.register(
                EHR_ID,
                ehrId,
                TEMPLATE_ID,
                compositionService.retrieveTemplateId(versionedCompoUid),
                HttpRestContext.LOCATION,
                auditLocation);
    }

    private String getLocationUrl(UUID versionedObjectUid, UUID ehrId, int version, String... pathSegments) {
        if (version == 0) {
            version = compositionService.getLastVersionNumber(versionedObjectUid);
        }

        String versionedComposition =
                String.format("%s::%s::%s", versionedObjectUid, systemService.getSystemId(), version);

        UriComponentsBuilder uriComponentsBuilder =
                fromPath("").pathSegment(EHR, ehrId.toString(), VERSIONED_COMPOSITION, versionedComposition);

        if (pathSegments.length > 0) {
            uriComponentsBuilder.pathSegment(pathSegments);
        }

        return uriComponentsBuilder.build().toString();
    }
}
