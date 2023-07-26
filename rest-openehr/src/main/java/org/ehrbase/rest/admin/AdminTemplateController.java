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
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.sdk.response.dto.admin.AdminDeleteResponseData;
import org.ehrbase.openehr.sdk.response.dto.admin.AdminStatusResponseData;
import org.ehrbase.rest.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API controller for Templates. Provides endpoints to update (replace) and delete templates.
 */
@TenantAware
@Tag(name = "Admin - Template")
@ConditionalOnProperty(prefix = "admin-api", name = "active")
@RestController
@RequestMapping(
        path = "${admin-api.context-path:/rest/admin}/template",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class AdminTemplateController extends BaseController {

    TemplateService templateService;

    @Autowired
    AdminTemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @Autowired
    AdminApiConfiguration adminApiConfiguration;

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_ADMIN_ACCESS)
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_UPDATE)
    @PutMapping(
            path = "/{template_id}",
            consumes = {MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE})
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Template has been updated successfully.",
                        headers = {
                            @Header(
                                    name = CONTENT_TYPE,
                                    description = RESP_CONTENT_TYPE_DESC,
                                    schema = @Schema(implementation = MediaType.class))
                        }),
                @ApiResponse(responseCode = "401", description = "Client credentials are invalid or have expired."),
                @ApiResponse(
                        responseCode = "403",
                        description = "Client has no access permission since admin role is missing."),
                @ApiResponse(responseCode = "404", description = "Template could not be found."),
                @ApiResponse(
                        responseCode = "422",
                        description = "Template could not be replaced since it is used in at least one Composition.")
            })
    public ResponseEntity<String> updateTemplate(
            @Parameter(description = REQ_ACCEPT)
                    @RequestHeader(value = ACCEPT, required = false, defaultValue = MediaType.APPLICATION_XML_VALUE)
                    String accept,
            @Parameter(description = REQ_CONTENT_TYPE) @RequestHeader(value = CONTENT_TYPE) String contentType,
            @Parameter(description = "Target template id to update. The value comes from the 'template_id' property.")
                    @PathVariable(value = "template_id")
                    String templateId,
            @Parameter(description = "New template content to replace old one with") @RequestBody() String content) {

        String updatedTemplate = this.templateService.adminUpdateTemplate(templateId, content);

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_XML_VALUE);

        return ResponseEntity.ok().headers(headers).body(updatedTemplate);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_ADMIN_ACCESS)
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_DELETE)
    @DeleteMapping(path = "/{template_id}")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "202", description = "Template has been deleted successfully."),
                @ApiResponse(responseCode = "401", description = "Client credentials are invalid or have expired."),
                @ApiResponse(
                        responseCode = "403",
                        description = "Client has no access permission since admin role is missing."),
                @ApiResponse(responseCode = "404", description = "Template could not be found."),
                @ApiResponse(
                        responseCode = "422",
                        description = "The template is still used by compositions and cannot be deleted.")
            })
    public ResponseEntity<AdminDeleteResponseData> deleteTemplate(
            @Parameter(description = "Target template id to delete. The value comes from the 'template_id' property.")
                    @PathVariable(value = "template_id")
                    String templateId) {

        int deleted = this.templateService.adminDeleteTemplate(templateId) ? 1 : 0;

        return ResponseEntity.ok().body(new AdminDeleteResponseData(deleted));
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_ADMIN_ACCESS)
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_DELETE)
    @DeleteMapping(path = "/all")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "All templates have been removed successfully",
                        headers = {
                            @Header(
                                    name = CONTENT_TYPE,
                                    description = RESP_CONTENT_TYPE_DESC,
                                    schema = @Schema(implementation = MediaType.class))
                        }),
                @ApiResponse(responseCode = "401", description = "Client credentials are invalid or have expired."),
                @ApiResponse(
                        responseCode = "403",
                        description = "Client has no access permission since admin role is missing."),
                @ApiResponse(responseCode = "404", description = "Template could not be found."),
                @ApiResponse(
                        responseCode = "422",
                        description = "There are templates that are used by compositions and cannot be removed.")
            })
    public ResponseEntity<?> deleteAllTemplates() {

        if (!this.adminApiConfiguration.getAllowDeleteAll()) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(new AdminStatusResponseData("Delete all resources not allowed."));
        }

        int deleted = this.templateService.adminDeleteAllTemplates();

        return ResponseEntity.ok().body(new AdminDeleteResponseData(deleted));
    }
}
