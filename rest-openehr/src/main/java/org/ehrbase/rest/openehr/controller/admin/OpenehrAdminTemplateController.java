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
import org.ehrbase.response.openehr.admin.AdminDeleteResponseData;
import org.ehrbase.response.openehr.admin.AdminStatusResponseData;
import org.ehrbase.response.openehr.admin.AdminUpdateResponseData;
import org.ehrbase.rest.openehr.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Autowired
    AdminApiConfiguration adminApiConfiguration;

    @PutMapping(path = "/{template_id}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
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
            )
    })
    public ResponseEntity<AdminUpdateResponseData> updateTemplate(
            @ApiParam(value = "Target template id to update")
            @PathVariable(value = "template_id")
                    String templateId
    ) {

        // TODO: Implement endpoint functionality

        return ResponseEntity.ok().body(new AdminUpdateResponseData(0));
    }

    @DeleteMapping(path = "/{template_id}")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 200,
                    message = "Template has been deleted successfully.",
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
            )
    })
    public ResponseEntity<AdminDeleteResponseData> deleteTemplate(
            @ApiParam(value = "Target template id to update")
            @PathVariable(value = "template_id")
                    String templateId
    ) {

        // TODO: Implement endpoint functionality

        return ResponseEntity.ok().body(new AdminDeleteResponseData(0));
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
