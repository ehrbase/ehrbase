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
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.audit.msg.AuditMsgBuilder;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.openehr.sdk.response.dto.admin.AdminDeleteResponseData;
import org.ehrbase.openehr.sdk.response.dto.admin.AdminUpdateResponseData;
import org.ehrbase.rest.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API controller for EHR related endpoints. Provides methods to update and delete EHRs physically in the DB.
 */
@TenantAware
@Tag(name = "Admin - EHR")
@ConditionalOnProperty(prefix = "admin-api", name = "active")
@RestController
@RequestMapping(
        path = BaseController.ADMIN_API_CONTEXT_PATH + "/ehr",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class AdminEhrController extends BaseController {

    private final EhrService ehrService;

    @Autowired
    public AdminEhrController(EhrService ehrService) {
        this.ehrService = ehrService;
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_ADMIN_ACCESS)
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_UPDATE)
    @PutMapping(
            path = "/{ehr_id}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "EHR has been updated and number of updated items will be returned in the body.",
                        headers = {
                            @Header(
                                    name = CONTENT_TYPE,
                                    description = RESP_CONTENT_TYPE_DESC,
                                    schema = @Schema(implementation = MediaType.class))
                        }),
                @ApiResponse(
                        responseCode = "401",
                        description = "Client credentials are invalid or have been expired."),
                @ApiResponse(
                        responseCode = "403",
                        description =
                                "Client is not permitted to access this resource since the admin role is missing."),
                @ApiResponse(responseCode = "404", description = "EHR with id could not be found.")
            })
    public ResponseEntity<AdminUpdateResponseData> updateEhr(
            @Parameter(description = "Client requested response content type")
                    @RequestHeader(value = HttpHeaders.ACCEPT, required = false)
                    String accept,
            @Parameter(description = "Target EHR id to update", required = true) @PathVariable(value = "ehr_id")
                    String ehrId) {

        // Check if EHR with id exists
        UUID ehrUuid = UUID.fromString(ehrId);
        if (!ehrService.hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException("Admin EHR", String.format("EHR with id %s does not exist.", ehrId));
        }

        AuditMsgBuilder.getInstance().setEhrIds(ehrUuid);

        // TODO: Implement endpoint functionality

        return ResponseEntity.ok().body(new AdminUpdateResponseData(0));
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_ADMIN_ACCESS)
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_DELETE)
    @DeleteMapping(path = "/{ehr_id}")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "EHR has been deleted successfully"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Client credentials are invalid or have been expired."),
                @ApiResponse(
                        responseCode = "403",
                        description =
                                "Client is not permitted to access this resource since the admin role is missing."),
                @ApiResponse(responseCode = "404", description = "EHR with id could not be found.")
            })
    public ResponseEntity<AdminDeleteResponseData> deleteEhr(
            @Parameter(description = "Target EHR id to delete", required = true) @PathVariable(value = "ehr_id")
                    String ehrId) {

        UUID ehrUuid = UUID.fromString(ehrId);
        // Check if EHR with id exists
        if (!ehrService.hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException("Admin EHR", String.format("EHR with id %s does not exist.", ehrId));
        }

        AuditMsgBuilder.getInstance().setEhrIds(ehrUuid).setRemovedPatients(getPatientNumbers(ehrUuid));

        ehrService.adminDeleteEhr(ehrUuid);

        return ResponseEntity.noContent().build();
    }

    private Set<String> getPatientNumbers(Object... ehrs) {
        return Arrays.stream(ehrs)
                .map(ehrId -> ehrService.getSubjectExtRef(ehrId.toString()))
                .collect(Collectors.toSet());
    }
}
