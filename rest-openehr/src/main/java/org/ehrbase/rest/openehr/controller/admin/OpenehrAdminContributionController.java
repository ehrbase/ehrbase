/*
 * Copyright (c) 2020 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.rest.openehr.controller.admin;

import io.swagger.annotations.*;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.response.openehr.admin.AdminDeleteResponseData;
import org.ehrbase.response.openehr.admin.AdminUpdateResponseData;
import org.ehrbase.rest.openehr.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin API controller for Contribution related data. Provides endpoints to update and remove Contributions in
 * database physically.
 */
@Api(tags = {"Admin", "Contribution"})
@ConditionalOnProperty(prefix = "admin-api", name = "active")
@RestController
@RequestMapping(path = "/rest/openehr/v1/admin/ehr", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrAdminContributionController extends BaseController {

    private final EhrService ehrService;

    @Autowired
    public OpenehrAdminContributionController(EhrService ehrService) {
        this.ehrService = ehrService;
    }

    @PutMapping(path = "/{ehr_id}/contribution/{contribution_id}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ApiResponses(value = {
            @ApiResponse(
                    code = 200,
                    message = "Contribution has been updated successfully.",
                    responseHeaders = {
                            @ResponseHeader(
                                    name = CONTENT_TYPE,
                                    description = RESP_CONTENT_TYPE_DESC,
                                    response = MediaType.class
                            )
                    }
            ),
            @ApiResponse(
                    code = 401,
                    message = "Client credentials invalid or have expired."
            ),
            @ApiResponse(
                    code = 403,
                    message = "Client does not have permission to access since admin role is missing."
            ),
            @ApiResponse(
                    code = 404,
                    message = "EHR or Contribution could not be found."
            )
    })
    public ResponseEntity<AdminUpdateResponseData> updateContribution(
            @ApiParam(value = "Target EHR id to update contribution inside.", required = true)
            @PathVariable(value = "ehr_id")
                    String ehrId,
            @ApiParam(value = "Target Contribution id to update", required = true)
            @PathVariable(value = "contribution_id")
                    String contributionId
    ) {
        UUID ehrUuid = UUID.fromString(ehrId);

        // Check if EHR exists
        if (!this.ehrService.hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException(
                    "Admin Contribution", String.format("EHR with id %s does not exist", ehrId)
            );
        }

        // TODO: Implement endpoint functionality

        // Contribution existence check will be done in services

        return ResponseEntity.ok().body(new AdminUpdateResponseData(0));
    }

    @DeleteMapping(path = "/{ehr_id}/contribution/{contribution_id}")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 200,
                    message = "Contribution has been deleted successfully.",
                    responseHeaders = {
                            @ResponseHeader(
                                    name = CONTENT_TYPE,
                                    description = RESP_CONTENT_TYPE_DESC,
                                    response = MediaType.class
                            )
                    }
            ),
            @ApiResponse(
                    code = 401,
                    message = "Client credentials invalid or have expired."
            ),
            @ApiResponse(
                    code = 403,
                    message = "Client does not have permission to access since admin role is missing."
            ),
            @ApiResponse(
                    code = 404,
                    message = "EHR or Contribution could not be found."
            )
    })
    public ResponseEntity<AdminDeleteResponseData> deleteContribution(
            @ApiParam(value = "Target EHR id to update contribution inside.", required = true)
            @PathVariable(value = "ehr_id")
                    String ehrId,
            @ApiParam(value = "Target Contribution id to update", required = true)
            @PathVariable(value = "contribution_id")
                    String contributionId
    ) {
        UUID ehrUuid = UUID.fromString(ehrId);

        // Check if EHR exists
        if (!this.ehrService.hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException(
                    "Admin Contribution", String.format("EHR with id %s does not exist", ehrId)
            );
        }

        // TODO: Implement endpoint functionality

        // Contribution existence check will be done in services

        return ResponseEntity.ok().body(new AdminDeleteResponseData(0));
    }
}
