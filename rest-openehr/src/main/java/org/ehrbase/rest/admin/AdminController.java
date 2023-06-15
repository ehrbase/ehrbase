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

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.openehr.sdk.response.dto.admin.AdminStatusResponseData;
import org.ehrbase.rest.BaseController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@TenantAware
@Tag(name = "Admin - Heartbeat")
@ConditionalOnProperty(prefix = "admin-api", name = "active")
@RestController
@RequestMapping(
        path = "${admin-api.context-path:/rest/admin}",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class AdminController extends BaseController {

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_ADMIN_ACCESS)
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_SYSTEM_STATUS)
    @GetMapping(path = "/status")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Admin API resources available and user has permission to access.",
                        headers = {
                            @Header(
                                    name = CONTENT_TYPE,
                                    description = RESP_CONTENT_TYPE_DESC,
                                    schema = @Schema(implementation = MediaType.class))
                        }),
                @ApiResponse(responseCode = "401", description = "Client credentials are invalid or have expired."),
                @ApiResponse(
                        responseCode = "403",
                        description = "Client has no access permission since the admin role is missing.")
            })
    public ResponseEntity<AdminStatusResponseData> getStatus() {

        return ResponseEntity.ok()
                .body(new AdminStatusResponseData("EHRbase Admin API available and you have permission to access it"));
    }
}
