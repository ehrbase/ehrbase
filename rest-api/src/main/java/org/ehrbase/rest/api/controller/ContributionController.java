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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.repository.ContributionRepository;
import org.ehrbase.service.RequestContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v1 controller for contribution operations.
 */
@RestController
@RequestMapping("/api/v1/ehrs/{ehr_id}/contributions")
@Tag(name = "Contribution", description = "Contribution lifecycle: create, retrieve, list")
public class ContributionController extends BaseApiController {

    private final ContributionRepository contributionRepository;
    private final EhrService ehrService;
    private final RequestContext requestContext;

    public ContributionController(
            ContributionRepository contributionRepository, EhrService ehrService, RequestContext requestContext) {
        this.contributionRepository = contributionRepository;
        this.ehrService = ehrService;
        this.requestContext = requestContext;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create contribution", description = "Groups multiple changes atomically with audit details")
    public ResponseEntity<ContributionRepository.ContributionRecord> createContribution(
            @PathVariable("ehr_id") String ehrIdStr,
            @RequestParam(value = "type", defaultValue = "composition") String contributionType,
            @RequestParam(value = "change_type", defaultValue = "creation") String changeType) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        requestContext.setEhrId(ehrId);

        UUID contributionId = contributionRepository.createContribution(ehrId, contributionType, changeType);
        URI location = locationUri("api", "v1", "ehrs", ehrId.toString(), "contributions", contributionId.toString());

        ContributionRepository.ContributionRecord record = contributionRepository
                .findById(contributionId)
                .orElseThrow(() -> new ObjectNotFoundException("contribution", contributionId.toString()));

        return ResponseEntity.created(location).body(record);
    }

    @GetMapping(value = "/{contribution_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get contribution by ID")
    public ResponseEntity<ContributionRepository.ContributionRecord> getContribution(
            @PathVariable("ehr_id") String ehrIdStr, @PathVariable("contribution_id") String contributionIdStr) {

        UUID ehrId = parseEhrId(ehrIdStr);
        UUID contributionId = parseUuid(contributionIdStr, "contribution");
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        ContributionRepository.ContributionRecord record = contributionRepository
                .findById(contributionId)
                .orElseThrow(() -> new ObjectNotFoundException("contribution", contributionIdStr));

        return ResponseEntity.ok(record);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List contributions for EHR")
    public ResponseEntity<List<ContributionRepository.ContributionRecord>> listContributions(
            @PathVariable("ehr_id") String ehrIdStr) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        List<ContributionRepository.ContributionRecord> contributions = contributionRepository.findByEhr(ehrId);
        return ResponseEntity.ok(contributions);
    }
}
