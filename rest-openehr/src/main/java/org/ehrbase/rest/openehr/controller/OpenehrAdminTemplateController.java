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
package org.ehrbase.rest.openehr.controller;

import io.swagger.annotations.*;
import org.ehrbase.response.openehr.AdminDeleteResponseData;
import org.ehrbase.response.openehr.AdminUpdateResponseData;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin API controller for Templates. Provides endpoints to update (replace) and delete templates.
 */
@Api(tags = {"Admin", "Template"})
@RestController
@RequestMapping(path = "/rest/openehr/v1/admin/template")
public class OpenehrAdminTemplateController extends BaseController {

    @PutMapping(path = "/{template_id}")
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
            @ApiParam(value = "Taget template id to update")
            @PathVariable(value = "template_id")
                    String templateId
    ) {

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
            @ApiParam(value = "Taget template id to update")
            @PathVariable(value = "template_id")
                    String templateId
    ) {

        return ResponseEntity.ok().body(new AdminDeleteResponseData(0));
    }
}
