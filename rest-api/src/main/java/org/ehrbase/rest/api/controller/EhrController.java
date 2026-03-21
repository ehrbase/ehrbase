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

import com.nedap.archie.rm.ehr.EhrStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.rest.api.dto.EhrResponseDto;
import org.ehrbase.service.RequestContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v1 controller for EHR lifecycle operations.
 */
@RestController
@RequestMapping("/api/v1/ehrs")
@Tag(name = "EHR", description = "EHR lifecycle: create, retrieve, find by subject")
public class EhrController extends BaseApiController {

    private final EhrService ehrService;
    private final SystemService systemService;
    private final RequestContext requestContext;

    public EhrController(EhrService ehrService, SystemService systemService, RequestContext requestContext) {
        this.ehrService = ehrService;
        this.systemService = systemService;
        this.requestContext = requestContext;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new EHR", description = "Creates a new EHR with optional EHR_STATUS body")
    public ResponseEntity<EhrResponseDto> createEhr(
            @RequestBody(required = false) EhrStatus status,
            @RequestHeader(value = "Prefer", required = false) String prefer) {

        UUID ehrId = ehrService.create(null, status);
        requestContext.setEhrId(ehrId);

        URI location = locationUri("api", "v1", "ehrs", ehrId.toString());
        EhrStatus createdStatus = ehrService.getEhrStatus(ehrId);
        String etag = ehrService.getLatestVersionUidOfStatus(ehrId).getValue();

        if (preferRepresentation(prefer)) {
            return created(location, etag).body(buildEhrResponse(ehrId, createdStatus));
        }
        return created(location, etag).build();
    }

    @PutMapping(value = "/{ehr_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create EHR with specific ID")
    public ResponseEntity<EhrResponseDto> createEhrWithId(
            @PathVariable("ehr_id") String ehrIdStr,
            @RequestBody(required = false) EhrStatus status,
            @RequestHeader(value = "Prefer", required = false) String prefer) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.create(ehrId, status);
        requestContext.setEhrId(ehrId);

        URI location = locationUri("api", "v1", "ehrs", ehrId.toString());
        EhrStatus createdStatus = ehrService.getEhrStatus(ehrId);
        String etag = ehrService.getLatestVersionUidOfStatus(ehrId).getValue();

        if (preferRepresentation(prefer)) {
            return created(location, etag).body(buildEhrResponse(ehrId, createdStatus));
        }
        return created(location, etag).build();
    }

    @GetMapping(value = "/{ehr_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get EHR by ID")
    public ResponseEntity<EhrResponseDto> getEhr(@PathVariable("ehr_id") String ehrIdStr) {
        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        EhrStatus status = ehrService.getEhrStatus(ehrId);
        return ResponseEntity.ok(buildEhrResponse(ehrId, status));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, params = "subject_id")
    @Operation(summary = "Find EHR by subject", description = "Look up EHR by external subject identifier")
    public ResponseEntity<EhrResponseDto> findBySubject(
            @RequestParam("subject_id") String subjectId,
            @RequestParam(value = "subject_namespace", required = false) String subjectNamespace) {

        UUID ehrId = ehrService
                .findBySubject(subjectId, subjectNamespace)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "ehr", "No EHR found for subject_id=%s, namespace=%s".formatted(subjectId, subjectNamespace)));

        requestContext.setEhrId(ehrId);
        EhrStatus status = ehrService.getEhrStatus(ehrId);
        return ResponseEntity.ok(buildEhrResponse(ehrId, status));
    }

    private EhrResponseDto buildEhrResponse(UUID ehrId, EhrStatus status) {
        var dvDateTime = ehrService.getCreationTime(ehrId);
        OffsetDateTime created = dvDateTime.getValue() instanceof OffsetDateTime odt
                ? odt
                : OffsetDateTime.parse(dvDateTime.getValue().toString());
        return new EhrResponseDto(ehrId, systemService.getSystemId(), status, created);
    }
}
