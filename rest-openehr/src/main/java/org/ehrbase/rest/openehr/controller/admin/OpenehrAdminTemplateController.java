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
import org.ehrbase.api.definitions.OperationalTemplateFormat;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.response.openehr.admin.AdminDeleteResponseData;
import org.ehrbase.response.openehr.admin.AdminStatusResponseData;
import org.ehrbase.rest.openehr.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin API controller for Templates. Provides endpoints to update (replace) and delete templates.
 */
@Api(tags = {"Admin", "Template"})
@ConditionalOnProperty(prefix = "admin-api", name = "active")
@RestController
@RequestMapping(path = "/rest/openehr/v1/admin/template", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrAdminTemplateController extends BaseController {

    TemplateService templateService;

    @Autowired
    OpenehrAdminTemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @Autowired
    AdminApiConfiguration adminApiConfiguration;


    @PutMapping(
            path = "/{template_id}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = 200,
                    message = "Template has been updated successfully.",
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
                    message = "Client has no access permission since admin role is missing."
            ),
            @ApiResponse(
                    code = 404,
                    message = "Template could not be found."
            ),
            @ApiResponse(
                    code = 422,
                    message = "Template could not be replaced since it is used in at least one Composition."
            )
    })
    public ResponseEntity<String> updateTemplate(
            @ApiParam(value = REQ_ACCEPT)
            @RequestHeader(value = ACCEPT, required = false, defaultValue = MediaType.APPLICATION_XML_VALUE)
                    String accept,
            @ApiParam(value = REQ_CONTENT_TYPE)
            @RequestHeader(value = CONTENT_TYPE)
                    String contentType,
            @ApiParam(value = "Target template id to update. The value comes from the 'template_id' property.")
            @PathVariable(value = "template_id")
                    String templateId,
            @ApiParam(value = "New template content to replace old one with")
            @RequestBody() String content
    ) {

        // Currently only 'application/xml' is supported. So skip if Accept or Content-Type headers specify another type
        if (
                !(MediaType.parseMediaType(accept).equalsTypeAndSubtype(MediaType.APPLICATION_XML)
                && MediaType.parseMediaType(contentType).equalsTypeAndSubtype(MediaType.APPLICATION_XML))
        ) {
            throw new UnprocessableEntityException(
                    String.format(
                            "Only %s format currently supported.", MediaType.APPLICATION_XML_VALUE
                    )
            );
        }

        String newId = this.templateService.adminUpdateTemplate(templateId, content);

        // TODO: Change after implementation of JSON parsing of templates
        String updatedTemplate = this.templateService.findOperationalTemplate(newId, OperationalTemplateFormat.XML);

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_XML_VALUE);

        return ResponseEntity.ok().headers(headers).body(updatedTemplate);
    }

    @DeleteMapping(path = "/{template_id}")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 202,
                    message = "Template has been deleted successfully."
            ),
            @ApiResponse(
                    code = 401,
                    message = "Client credentials are invalid or have expired."
            ),
            @ApiResponse(
                    code = 403,
                    message = "Client has no access permission since admin role is missing."
            ),
            @ApiResponse(
                    code = 404,
                    message = "Template could not be found."
            ),
            @ApiResponse(
                    code = 422,
                    message = "The template is still used by compositions and cannot be deleted."
            )
    })
    public ResponseEntity<AdminDeleteResponseData> deleteTemplate(
            @ApiParam(value = "Target template id to delete. The value comes from the 'template_id' property.")
            @PathVariable(value = "template_id")
                    String templateId
    ) {

        int deleted = this.templateService.adminDeleteTemplate(templateId) ? 1 : 0;

        return ResponseEntity.ok().body(new AdminDeleteResponseData(deleted));
    }

    @DeleteMapping(path = "/all")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 200,
                    message = "All templates have been removed successfully",
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
                    message = "Client has no access permission since admin role is missing."
            ),
            @ApiResponse(
                    code = 404,
                    message = "Template could not be found."
            ),
            @ApiResponse(
                    code = 422,
                    message = "There are templates that are used by compositions and cannot be removed."
            )
    })
    public ResponseEntity<?> deleteAllTemplates() {

        if (!this.adminApiConfiguration.getAllowDeleteAll()) {
            return ResponseEntity
                    .status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(new AdminStatusResponseData(
                            "Delete all resources not allowed."
                    ));
        }

        // TODO: Implement endpoint functionality

        return ResponseEntity.ok().body(new AdminDeleteResponseData(0));
    }
}
