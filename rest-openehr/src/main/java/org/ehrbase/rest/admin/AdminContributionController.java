/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.rest.admin;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.response.openehr.admin.AdminDeleteResponseData;
import org.ehrbase.response.openehr.admin.AdminUpdateResponseData;
import org.ehrbase.rest.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin API controller for Contribution related data. Provides endpoints to update and remove Contributions in
 * database physically.
 */
@Tag(name = "Admin - Contribution")
@ConditionalOnProperty(prefix = "admin-api", name = "active")
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

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_ADMIN_ACCESS)
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_CONTRIBUTION_UPDATE)
    @PutMapping(
            path = "/{ehr_id}/contribution/{contribution_id}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Contribution has been updated successfully.",
                        headers = {
                            @Header(
                                    name = CONTENT_TYPE,
                                    description = RESP_CONTENT_TYPE_DESC,
                                    schema = @Schema(implementation = MediaType.class))
                        }),
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

        // TODO: Implement endpoint functionality

        // Contribution existence check will be done in services

        return ResponseEntity.ok().body(new AdminUpdateResponseData(0));
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_ADMIN_ACCESS)
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_CONTRIBUTION_DELETE)
    @DeleteMapping(path = "/{ehr_id}/contribution/{contribution_id}")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Contribution has been deleted successfully.",
                        headers = {
                            @Header(
                                    name = CONTENT_TYPE,
                                    description = RESP_CONTENT_TYPE_DESC,
                                    schema = @Schema(implementation = MediaType.class))
                        }),
                @ApiResponse(responseCode = "401", description = "Client credentials invalid or have expired."),
                @ApiResponse(
                        responseCode = "403",
                        description = "Client does not have permission to access since admin role is missing."),
                @ApiResponse(responseCode = "404", description = "EHR or Contribution could not be found.")
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

        UUID contributionUUID = UUID.fromString(contributionId);

        contributionService.adminDelete(contributionUUID);

        return ResponseEntity.noContent().build();
    }
}
