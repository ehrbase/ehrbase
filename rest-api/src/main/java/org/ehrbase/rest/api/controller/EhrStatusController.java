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
package org.ehrbase.rest.api.controller;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.service.RequestContext;
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
 * REST API v1 controller for EHR_STATUS operations.
 */
@RestController
@RequestMapping("/api/v1/ehrs/{ehr_id}/ehr_status")
@Tag(name = "EHR Status", description = "EHR_STATUS versioned operations")
public class EhrStatusController extends BaseApiController {

    private final EhrService ehrService;
    private final RequestContext requestContext;

    public EhrStatusController(EhrService ehrService, RequestContext requestContext) {
        this.ehrService = ehrService;
        this.requestContext = requestContext;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get current EHR_STATUS", description = "Returns the latest version of EHR_STATUS")
    public ResponseEntity<EhrStatus> getEhrStatus(
            @PathVariable("ehr_id") String ehrIdStr,
            @RequestParam(value = "version_at_time", required = false) String versionAtTime) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        if (versionAtTime != null) {
            OffsetDateTime timestamp = parseVersionAtTime(versionAtTime);
            ObjectVersionId versionUid = ehrService.getEhrStatusVersionByTimestamp(ehrId, timestamp);
            int version = extractVersion(versionUid.getValue());
            UUID versionedObjectUid = extractVersionedObjectUid(versionUid.getValue());
            Optional<OriginalVersion<EhrStatus>> ov =
                    ehrService.getEhrStatusAtVersion(ehrId, versionedObjectUid, version);
            return ov.map(v -> ResponseEntity.ok(v.getData()))
                    .orElseThrow(() ->
                            new ObjectNotFoundException("ehr_status", "No status at time %s".formatted(versionAtTime)));
        }

        EhrStatus status = ehrService.getEhrStatus(ehrId);
        String etag = ehrService.getLatestVersionUidOfStatus(ehrId).getValue();
        return ok(etag).body(status);
    }

    @GetMapping(value = "/{version_uid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get EHR_STATUS by version UID")
    public ResponseEntity<EhrStatus> getEhrStatusByVersion(
            @PathVariable("ehr_id") String ehrIdStr, @PathVariable("version_uid") String versionUid) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        UUID versionedObjectUid = extractVersionedObjectUid(versionUid);
        int version = extractVersion(versionUid);

        Optional<OriginalVersion<EhrStatus>> ov = ehrService.getEhrStatusAtVersion(ehrId, versionedObjectUid, version);
        return ov.map(v -> ok(versionUid).body(v.getData()))
                .orElseThrow(() -> new ObjectNotFoundException("ehr_status", versionUid));
    }

    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update EHR_STATUS", description = "Requires If-Match header for optimistic locking")
    public ResponseEntity<EhrStatus> updateEhrStatus(
            @PathVariable("ehr_id") String ehrIdStr,
            @RequestBody EhrStatus status,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(value = "Prefer", required = false) String prefer) {

        UUID ehrId = parseEhrId(ehrIdStr);
        requestContext.setEhrId(ehrId);

        String cleanIfMatch = ifMatch.replace("\"", "");
        ObjectVersionId targetObjId = new ObjectVersionId(cleanIfMatch);

        EhrStatus updated = ehrService.updateStatus(ehrId, status, targetObjId, null, null);
        String etag = ehrService.getLatestVersionUidOfStatus(ehrId).getValue();
        URI location = locationUri("api", "v1", "ehrs", ehrId.toString(), "ehr_status", etag);

        if (preferRepresentation(prefer)) {
            return ok(etag).location(location).body(updated);
        }
        return ResponseEntity.noContent()
                .eTag("\"" + etag + "\"")
                .location(location)
                .build();
    }
}
