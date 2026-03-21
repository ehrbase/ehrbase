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

import com.nedap.archie.rm.composition.Composition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.rest.api.dto.BulkResponseDto;
import org.ehrbase.rest.api.dto.BulkResponseDto.BulkItemResult;
import org.ehrbase.service.RequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v1 controller for bulk composition operations.
 * Returns 207 Multi-Status with per-item success/failure.
 */
@RestController
@Tag(name = "Bulk Composition", description = "Batch create/update/delete compositions with 207 Multi-Status")
public class BulkCompositionController extends BaseApiController {

    private final CompositionService compositionService;
    private final EhrService ehrService;
    private final SystemService systemService;
    private final RequestContext requestContext;

    public BulkCompositionController(
            CompositionService compositionService,
            EhrService ehrService,
            SystemService systemService,
            RequestContext requestContext) {
        this.compositionService = compositionService;
        this.ehrService = ehrService;
        this.systemService = systemService;
        this.requestContext = requestContext;
    }

    @SuppressWarnings("deprecation")
    @PostMapping(
            value = "/api/v1/ehrs/{ehr_id}/compositions/bulk",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create multiple compositions", description = "Returns 207 Multi-Status with per-item results")
    public ResponseEntity<BulkResponseDto> bulkCreate(
            @PathVariable("ehr_id") String ehrIdStr, @RequestBody Map<String, Object> body) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        requestContext.setEhrId(ehrId);

        List<?> compositions = (List<?>) body.getOrDefault("compositions", List.of());
        List<BulkItemResult> results = new ArrayList<>();

        for (int i = 0; i < compositions.size(); i++) {
            try {
                String json = compositions.get(i).toString();
                Composition composition = compositionService.buildComposition(json, CompositionFormat.JSON, null);
                UUID compositionId =
                        compositionService.create(ehrId, composition).orElseThrow();
                String uid = compositionId + "::" + systemService.getSystemId() + "::1";
                results.add(BulkItemResult.success(i, uid));
            } catch (Exception e) {
                results.add(BulkItemResult.failure(i, 400, e.getMessage()));
            }
        }

        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(new BulkResponseDto(results));
    }

    @PatchMapping(
            value = "/api/v1/compositions/bulk",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update multiple compositions")
    public ResponseEntity<BulkResponseDto> bulkUpdate(@RequestBody Map<String, Object> body) {
        List<?> updates = (List<?>) body.getOrDefault("compositions", List.of());
        List<BulkItemResult> results = new ArrayList<>();

        for (int i = 0; i < updates.size(); i++) {
            results.add(BulkItemResult.failure(i, 501, "Bulk update not yet fully implemented"));
        }

        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(new BulkResponseDto(results));
    }

    @DeleteMapping(
            value = "/api/v1/compositions/bulk",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete multiple compositions")
    public ResponseEntity<BulkResponseDto> bulkDelete(@RequestBody Map<String, Object> body) {
        List<?> deletes = (List<?>) body.getOrDefault("compositions", List.of());
        List<BulkItemResult> results = new ArrayList<>();

        for (int i = 0; i < deletes.size(); i++) {
            results.add(BulkItemResult.failure(i, 501, "Bulk delete not yet fully implemented"));
        }

        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(new BulkResponseDto(results));
    }
}
