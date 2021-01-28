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
            consumes = {MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE}
    )
    public ResponseEntity<String> updateTemplate(
            @RequestHeader(value = ACCEPT, required = false, defaultValue = MediaType.APPLICATION_XML_VALUE) String accept,
            @RequestHeader(value = CONTENT_TYPE) String contentType,
            @PathVariable(value = "template_id") String templateId,
            @RequestBody() String content) {

        String updatedTemplate = this.templateService.adminUpdateTemplate(templateId, content);

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_XML_VALUE);

        return ResponseEntity.ok().headers(headers).body(updatedTemplate);
    }

    @DeleteMapping(path = "/{template_id}")
    public ResponseEntity<AdminDeleteResponseData> deleteTemplate(
            @PathVariable(value = "template_id") String templateId) {

        int deleted = this.templateService.adminDeleteTemplate(templateId) ? 1 : 0;

        return ResponseEntity.ok().body(new AdminDeleteResponseData(deleted));
    }

    @DeleteMapping(path = "/all")
    public ResponseEntity<?> deleteAllTemplates() {

        if (!this.adminApiConfiguration.getAllowDeleteAll()) {
            return ResponseEntity
                    .status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(new AdminStatusResponseData(
                            "Delete all resources not allowed."
                    ));
        }

        int deleted = this.templateService.adminDeleteAllTemplates();

        return ResponseEntity.ok().body(new AdminDeleteResponseData(deleted));
    }
}
