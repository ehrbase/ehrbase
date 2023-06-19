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
package org.ehrbase.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.service.StatusService;
import org.ehrbase.openehr.sdk.response.dto.StatusResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API endpoint to get status of EHRbase and version information on used dependencies as archie or openEHR_sdk as well
 * as the current used JVM version or target PostgreSQL server version.
 */
@Tag(name = "Status", description = "Heartbeat, Version info, Status")
@RestController
@RequestMapping(
        path = "/rest",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
public class StatusController extends BaseController {

    private final StatusService statusService;

    @Autowired
    public StatusController(StatusService statusService) {
        this.statusService = Objects.requireNonNull(statusService);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_SYSTEM_STATUS)
    @GetMapping(path = "/status")
    @Operation(summary = "Get status information on running EHRbase server instance")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description =
                                "EHRbase is available. Basic information on runtime and build is returned in body.",
                        headers = {
                            @Header(
                                    name = CONTENT_TYPE,
                                    description = RESP_CONTENT_TYPE_DESC,
                                    schema = @Schema(implementation = MediaType.class))
                        },
                        content = @Content(schema = @Schema(implementation = StatusResponseData.class)))
            })
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<StatusResponseData> getEhrbaseStatus(
            @Parameter(description = "Client desired response data format")
                    @RequestHeader(
                            value = HttpHeaders.ACCEPT,
                            required = false,
                            defaultValue = MediaType.APPLICATION_JSON_VALUE)
                    String accept) {
        StatusResponseData responseData = new StatusResponseData();
        // Java VM version
        responseData.setJvmVersion(this.statusService.getJavaVMInformation());
        // OS Identifier and version
        responseData.setOsVersion(this.statusService.getOperatingSystemInformation());
        // Database server version
        responseData.setPostgresVersion(this.statusService.getDatabaseInformation());
        // EHRbase version
        responseData.setEhrbaseVersion(this.statusService.getEhrbaseVersion());
        // Client SDK Version
        responseData.setOpenEhrSdkVersion(this.statusService.getOpenEHR_SDK_Version());
        // Archie version
        responseData.setArchieVersion(this.statusService.getArchieVersion());

        return ResponseEntity.ok(responseData);
    }
}
