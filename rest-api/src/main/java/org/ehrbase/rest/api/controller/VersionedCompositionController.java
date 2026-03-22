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
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.RevisionHistory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.service.RequestContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v2 controller for versioned composition container operations.
 */
@RestController
@RequestMapping("/api/v2/ehrs/{ehr_id}/versioned_composition/{versioned_object_uid}")
@Tag(name = "Versioned Composition", description = "Versioned composition container metadata and history")
public class VersionedCompositionController extends BaseApiController {

    private final CompositionService compositionService;
    private final EhrService ehrService;
    private final RequestContext requestContext;

    public VersionedCompositionController(
            CompositionService compositionService, EhrService ehrService, RequestContext requestContext) {
        this.compositionService = compositionService;
        this.ehrService = ehrService;
        this.requestContext = requestContext;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get versioned composition container metadata")
    public ResponseEntity<VersionedComposition> getVersionedComposition(
            @PathVariable("ehr_id") String ehrIdStr,
            @PathVariable("versioned_object_uid") String versionedObjectUidStr) {

        UUID ehrId = parseEhrId(ehrIdStr);
        UUID compositionId = parseUuid(versionedObjectUidStr, "composition");
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);
        requestContext.setCompositionId(compositionId);

        VersionedComposition vc = compositionService.getVersionedComposition(ehrId, compositionId);
        return ResponseEntity.ok(vc);
    }

    @GetMapping(value = "/revision_history", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get composition revision history")
    public ResponseEntity<RevisionHistory> getRevisionHistory(
            @PathVariable("ehr_id") String ehrIdStr,
            @PathVariable("versioned_object_uid") String versionedObjectUidStr) {

        UUID ehrId = parseEhrId(ehrIdStr);
        UUID compositionId = parseUuid(versionedObjectUidStr, "composition");
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);
        requestContext.setCompositionId(compositionId);

        RevisionHistory history = compositionService.getRevisionHistoryOfVersionedComposition(ehrId, compositionId);
        return ResponseEntity.ok(history);
    }

    @GetMapping(value = "/version/{version_uid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get original version of composition with audit")
    public ResponseEntity<OriginalVersion<Composition>> getVersionByUid(
            @PathVariable("ehr_id") String ehrIdStr,
            @PathVariable("versioned_object_uid") String versionedObjectUidStr,
            @PathVariable("version_uid") String versionUid) {

        UUID ehrId = parseEhrId(ehrIdStr);
        UUID compositionId = parseUuid(versionedObjectUidStr, "composition");
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);
        requestContext.setCompositionId(compositionId);

        int version = extractVersion(versionUid);

        Optional<OriginalVersion<Composition>> ov =
                compositionService.getOriginalVersionComposition(ehrId, compositionId, version);
        return ov.map(ResponseEntity::ok).orElseThrow(() -> new ObjectNotFoundException("composition", versionUid));
    }
}
