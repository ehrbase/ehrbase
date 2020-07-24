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
import org.ehrbase.rest.openehr.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin API controller for directories. Provides endpoint to remove complete directory trees from database physically.
 */
@Api(tags = {"Admin", "Directory"})
@ConditionalOnProperty(prefix = "admin-api", name = "active")
@RestController
@RequestMapping(path = "/rest/openehr/v1/admin/ehr", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrAdminDirectoryController extends BaseController {

    private final EhrService ehrService;

    @Autowired
    public OpenehrAdminDirectoryController(EhrService ehrService) {
        this.ehrService = ehrService;
    }

    @DeleteMapping(path = "/{ehr_id}/directory/{directory_id}")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 200,
                    message = "Directory has been deleted successfully",
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
                    message = "Client credentials are invalid or have expired."
            ),
            @ApiResponse(
                    code = 403,
                    message = "Client has no permission to access since admin role is missing."
            ),
            @ApiResponse(
                    code = 404,
                    message = "EHR or Directory could not be found."
            )
    })
    public ResponseEntity<AdminDeleteResponseData> deleteDirectory(
            @ApiParam(value = "Target EHR ed to remove Directory from", required = true)
            @PathVariable(value = "ehr_id")
                    String ehrId,
            @ApiParam(value = "Target Directory id to delete", required = true)
            @PathVariable(value = "directory_id")
                    String directoryId
    ) {

        UUID ehrUuid = UUID.fromString(ehrId);

        // Check if EHR exists
        if (!this.ehrService.hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException(
                    "Admin Directory", String.format("EHR with id %s does not exist", ehrId)
            );
        }

        // TODO: Implement endpoint functionality

        // Directory existence check will be done during processing

        return ResponseEntity.ok().body(new AdminDeleteResponseData(0));
    }
}
