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

import static org.ehrbase.api.rest.HttpRestContext.DIRECTORY_ID;
import static org.ehrbase.api.rest.HttpRestContext.EHR_ID;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.DirectoryService;
import org.ehrbase.openehr.sdk.response.dto.admin.AdminDeleteResponseData;
import org.ehrbase.rest.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API controller for directories. Provides endpoint to remove complete directory trees from database physically.
 */
@ConditionalOnMissingBean(name = "primaryadmindirectorycontroller")
@ConditionalOnProperty(prefix = "admin-api", name = "active")
@Tag(name = "Admin - Directory")
@RestController
@RequestMapping(
        path = "${admin-api.context-path:/rest/admin}/ehr",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class AdminDirectoryController extends BaseController {

    private final DirectoryService directoryService;

    @Autowired
    public AdminDirectoryController(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    @DeleteMapping(path = "/{ehr_id}/directory/{directory_id}")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Directory has been deleted successfully",
                        headers = {@Header(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC)}),
                @ApiResponse(responseCode = "401", description = "Client credentials are invalid or have expired."),
                @ApiResponse(
                        responseCode = "403",
                        description = "Client has no permission to access since admin role is missing."),
                @ApiResponse(responseCode = "404", description = "EHR or Directory could not be found.")
            })
    public ResponseEntity<AdminDeleteResponseData> deleteDirectory(
            @Parameter(description = "Target EHR ed to remove Directory from", required = true)
                    @PathVariable(value = "ehr_id")
                    String ehrId,
            @Parameter(description = "Target Directory id to delete", required = true)
                    @PathVariable(value = "directory_id")
                    String directoryId) {

        UUID ehrUuid = UUID.fromString(ehrId);

        UUID folderUid = UUID.fromString(directoryId);

        HttpRestContext.register(
                EHR_ID,
                ehrUuid,
                DIRECTORY_ID,
                folderUid.toString(),
                HttpRestContext.LOCATION,
                fromPath("")
                        .pathSegment(EHR, ehrId, DIRECTORY, folderUid.toString())
                        .build()
                        .toString());

        directoryService.adminDeleteFolder(ehrUuid, folderUid);

        return ResponseEntity.noContent().build();
    }
}
