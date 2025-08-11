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

import static org.ehrbase.api.rest.HttpRestContext.DIRECTORY_ID;
import static org.ehrbase.api.rest.HttpRestContext.EHR_ID;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.UUID;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.EhrStatusApiSpecification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 */
@ConditionalOnMissingBean(name = "primaryopenehrehrstatuscontroller")
@RestController
@RequestMapping(path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/ehr/{ehr_id}/ehr_status")
public class OpenehrEhrStatusController extends BaseController implements EhrStatusApiSpecification {

    private final EhrService ehrService;

    public OpenehrEhrStatusController(EhrService ehrService) {
        this.ehrService = ehrService;
    }

    @Override
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<EhrStatusDto> getEhrStatusVersionByTime(
            @PathVariable(name = "ehr_id") UUID ehrId,
            @RequestParam(name = "version_at_time", required = false) String versionAtTime,
            @RequestParam(value = PRETTY, required = false) String pretty) {

        final ObjectVersionId objectVersionId;

        if (versionAtTime != null) {
            OffsetDateTime time = decodeVersionAtTime(versionAtTime).orElseThrow();
            objectVersionId = ehrService.getEhrStatusVersionByTimestamp(ehrId, time);
        } else {
            objectVersionId = ehrService.getLatestVersionUidOfStatus(ehrId);
        }

        UUID ehrStatusId = extractVersionedObjectUidFromVersionUid(objectVersionId.getValue());
        int version = extractVersionFromVersionUid(objectVersionId.getValue()).orElseThrow();

        setPrettyPrintResponse(pretty);

        OriginalVersion<EhrStatusDto> originalVersion = ehrStatusVersion(ehrId, ehrStatusId, version);
        return responseBuilder(HttpStatus.OK, ehrId, originalVersion).body(originalVersion.getData());
    }

    @Override
    @GetMapping(
            path = "/{version_uid}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<EhrStatusDto> getEhrStatusByVersionId(
            @PathVariable(name = "ehr_id") UUID ehrId,
            @PathVariable(name = "version_uid") String versionUid,
            @RequestParam(value = PRETTY, required = false) String pretty) {

        UUID ehrStatusId = extractVersionedObjectUidFromVersionUid(versionUid);
        int version = extractVersionFromVersionUid(versionUid)
                .orElseThrow(
                        () -> new InvalidApiParameterException("VERSION UID parameter does not contain a version"));

        setPrettyPrintResponse(pretty);

        OriginalVersion<EhrStatusDto> originalVersion = ehrStatusVersion(ehrId, ehrStatusId, version);
        return responseBuilder(HttpStatus.OK, ehrId, originalVersion).body(originalVersion.getData());
    }

    @Override
    @PutMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<EhrStatusDto> updateEhrStatus(
            @PathVariable("ehr_id") UUID ehrId,
            @RequestHeader(name = IF_MATCH) String versionUid,
            @RequestHeader(name = PREFER, required = false) String prefer,
            @RequestParam(value = PRETTY, required = false) String pretty,
            @RequestBody EhrStatusDto ehrStatusDto) {

        HttpRestContext.register(EHR_ID, ehrId);

        // update EHR_STATUS and check for success
        ObjectVersionId targetObjId = new ObjectVersionId(versionUid);
        EhrService.EhrResult ehrResult = ehrService.updateStatus(ehrId, ehrStatusDto, targetObjId, null, null);
        ObjectVersionId statusUid = ehrResult.statusVersionId();

        // update and prepare current version number
        int version = extractVersionFromVersionUid(statusUid.getValue()).orElseThrow();
        UUID ehrStatusId = UUID.fromString(statusUid.getObjectId().getValue());

        // load status
        OriginalVersion<EhrStatusDto> originalVersion = ehrStatusVersion(ehrId, ehrStatusId, version);

        // return either representation body or only the created response
        if (RETURN_REPRESENTATION.equals(prefer)) {
            setPrettyPrintResponse(pretty);
            return responseBuilder(HttpStatus.OK, ehrId, originalVersion).body(originalVersion.getData());
        } else {
            return responseBuilder(HttpStatus.NO_CONTENT, ehrId, originalVersion)
                    .build();
        }
    }

    private ResponseEntity.BodyBuilder responseBuilder(
            HttpStatus status, UUID ehrId, OriginalVersion<EhrStatusDto> originalVersion) {

        createRestContext(ehrId, originalVersion.getUid());

        ObjectVersionId versionId = originalVersion.getUid();
        URI uri = createLocationUri(EHR, ehrId.toString(), EHR_STATUS, versionId.getValue());
        return ResponseEntity.status(status)
                .location(uri)
                .eTag("\"" + versionId.getValue() + "\"")
                .lastModified(lastModifiedValue(originalVersion.getCommitAudit().getTimeCommitted()));
    }

    private OriginalVersion<EhrStatusDto> ehrStatusVersion(UUID ehrId, UUID ehrStatusId, int version) {
        return ehrService
                .getEhrStatusAtVersion(ehrId, ehrStatusId, version)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "EHR_STATUS",
                        "Could not find EhrStatus[id=%s, version=%s]".formatted(ehrStatusId.toString(), version)));
    }

    private static Instant lastModifiedValue(DvDateTime dvDateTime) {
        TemporalAccessor timeCommitted = dvDateTime.getValue();
        if (timeCommitted.query(TemporalQueries.zone()) != null) {
            return ZonedDateTime.from(timeCommitted).toInstant();
        } else {
            return LocalDateTime.from(timeCommitted).toInstant(ZoneOffset.UTC);
        }
    }

    private void createRestContext(UUID ehrId, ObjectVersionId versionId) {

        HttpRestContext.register(
                EHR_ID,
                ehrId,
                DIRECTORY_ID,
                versionId.getValue(),
                HttpRestContext.LOCATION,
                fromPath("")
                        .pathSegment(EHR, ehrId.toString(), EHR_STATUS, versionId.getValue())
                        .build()
                        .toString());
    }
}
