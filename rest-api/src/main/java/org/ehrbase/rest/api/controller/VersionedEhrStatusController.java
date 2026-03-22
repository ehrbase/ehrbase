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
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.RevisionHistory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.service.RequestContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v2 controller for versioned EHR_STATUS container operations.
 */
@RestController
@RequestMapping("/api/v2/ehrs/{ehr_id}/versioned_ehr_status")
@Tag(name = "Versioned EHR Status", description = "Versioned EHR_STATUS container metadata and history")
public class VersionedEhrStatusController extends BaseApiController {

    private final EhrService ehrService;
    private final RequestContext requestContext;

    public VersionedEhrStatusController(EhrService ehrService, RequestContext requestContext) {
        this.ehrService = ehrService;
        this.requestContext = requestContext;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get versioned EHR_STATUS container metadata")
    public ResponseEntity<VersionedEhrStatus> getVersionedEhrStatus(@PathVariable("ehr_id") String ehrIdStr) {
        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        VersionedEhrStatus vs = ehrService.getVersionedEhrStatus(ehrId);
        return ResponseEntity.ok(vs);
    }

    @GetMapping(value = "/revision_history", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get EHR_STATUS revision history")
    public ResponseEntity<RevisionHistory> getRevisionHistory(@PathVariable("ehr_id") String ehrIdStr) {
        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        RevisionHistory history = ehrService.getRevisionHistoryOfVersionedEhrStatus(ehrId);
        return ResponseEntity.ok(history);
    }

    @GetMapping(value = "/version/{version_uid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get original version of EHR_STATUS")
    public ResponseEntity<OriginalVersion<EhrStatus>> getVersionByUid(
            @PathVariable("ehr_id") String ehrIdStr, @PathVariable("version_uid") String versionUid) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        UUID versionedObjectUid = extractVersionedObjectUid(versionUid);
        int version = extractVersion(versionUid);

        Optional<OriginalVersion<EhrStatus>> ov = ehrService.getEhrStatusAtVersion(ehrId, versionedObjectUid, version);
        return ov.map(ResponseEntity::ok).orElseThrow(() -> new ObjectNotFoundException("ehr_status", versionUid));
    }
}
