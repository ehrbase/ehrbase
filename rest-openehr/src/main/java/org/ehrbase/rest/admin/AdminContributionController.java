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
package org.ehrbase.rest.admin;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.ehrbase.api.rest.HttpRestContext.EHR_ID;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.openehr.sdk.response.dto.admin.AdminDeleteResponseData;
import org.ehrbase.openehr.sdk.response.dto.admin.AdminUpdateResponseData;
import org.ehrbase.rest.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API controller for Contribution related data. Provides endpoints to update and remove Contributions in
 * database physically.
 */
@ConditionalOnMissingBean(name = "primaryadmincontributioncontroller")
@ConditionalOnProperty(prefix = "admin-api", name = "active")
@Tag(name = "Admin - Contribution")
@RestController
@RequestMapping(
        path = "${admin-api.context-path:/rest/admin}/ehr",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class AdminContributionController extends BaseController {

    private final EhrService ehrService;
    private final ContributionService contributionService;

    @Autowired
    public AdminContributionController(EhrService ehrService, ContributionService contributionService) {
        this.ehrService = ehrService;
        this.contributionService = contributionService;
    }

    @PutMapping(
            path = "/{ehr_id}/contribution/{contribution_id}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Contribution has been updated successfully.",
                        headers = {@Header(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC)}),
                @ApiResponse(responseCode = "401", description = "Client credentials invalid or have expired."),
                @ApiResponse(
                        responseCode = "403",
                        description = "Client does not have permission to access since admin role is missing."),
                @ApiResponse(responseCode = "404", description = "EHR or Contribution could not be found.")
            })
    public ResponseEntity<AdminUpdateResponseData> updateContribution(
            @Parameter(description = "Target EHR id to update contribution inside.", required = true)
                    @PathVariable(value = "ehr_id")
                    String ehrId,
            @Parameter(description = "Target Contribution id to update", required = true)
                    @PathVariable(value = "contribution_id")
                    String contributionId) {
        UUID ehrUuid = UUID.fromString(ehrId);

        // Check if EHR exists
        if (!this.ehrService.hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException(
                    "Admin Contribution", String.format("EHR with id %s does not exist", ehrId));
        }
        UUID contributionUUID = UUID.fromString(contributionId);

        // TODO: Implement endpoint functionality

        // Contribution existence check will be done in services

        createRestContext(ehrUuid, contributionUUID);

        return ResponseEntity.ok().body(new AdminUpdateResponseData(0));
    }

    @DeleteMapping(path = "/{ehr_id}/contribution/{contribution_id}")
    @Operation(summary = "Not supported since 2.0.0")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "501", description = "Contribution delete is not supported since 2.0.0."),
            })
    public ResponseEntity<AdminDeleteResponseData> deleteContribution(
            @Parameter(description = "Target EHR id to update contribution inside.", required = true)
                    @PathVariable(value = "ehr_id")
                    String ehrId,
            @Parameter(description = "Target Contribution id to update", required = true)
                    @PathVariable(value = "contribution_id")
                    String contributionId) {

        UUID ehrUuid = UUID.fromString(ehrId);

        // Check if EHR exists
        if (!this.ehrService.hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException(
                    "Admin Contribution", String.format("EHR with id %s does not exist", ehrId));
        }

        contributionService.adminDelete(ehrUuid, UUID.fromString(contributionId));

        createRestContext(ehrUuid, UUID.fromString(contributionId));

        return ResponseEntity.noContent().build();
    }

    private void createRestContext(UUID ehrId, UUID contributionId) {
        HttpRestContext.register(
                EHR_ID,
                ehrId,
                HttpRestContext.LOCATION,
                fromPath(EMPTY)
                        .pathSegment(EHR, ehrId.toString(), CONTRIBUTION, contributionId.toString())
                        .build()
                        .toString());
    }
}
