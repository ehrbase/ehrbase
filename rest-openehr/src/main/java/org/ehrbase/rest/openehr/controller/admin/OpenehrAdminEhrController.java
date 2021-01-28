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

import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.response.openehr.admin.AdminDeleteResponseData;
import org.ehrbase.response.openehr.admin.AdminUpdateResponseData;
import org.ehrbase.rest.openehr.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin API controller for EHR related endpoints. Provides methods to update and delete EHRs physically in the DB.
 */
@ConditionalOnProperty(prefix = "admin-api", name = "active")
@RestController
@RequestMapping(path = "/rest/openehr/v1/admin/ehr", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrAdminEhrController extends BaseController {

    private final EhrService ehrService;

    @Autowired
    public OpenehrAdminEhrController(EhrService ehrService) {
        this.ehrService = ehrService;
    }

    @PutMapping(path = "/{ehr_id}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<AdminUpdateResponseData> updateEhr(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrId) {

        // Check if EHR with id exists
        UUID ehrUuid = UUID.fromString(ehrId);
        if (!ehrService.hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException("Admin EHR", String.format("EHR with id %s does not exist.", ehrId));
        }

        // TODO: Implement endpoint functionality

        return ResponseEntity.ok().body(new AdminUpdateResponseData(0));
    }

    @DeleteMapping(path = "/{ehr_id}")
    public ResponseEntity<AdminDeleteResponseData> deleteEhr(
            @PathVariable(value = "ehr_id") String ehrId) {

        UUID ehrUuid = UUID.fromString(ehrId);
        // Check if EHR with id exists
        if (!ehrService.hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException("Admin EHR", String.format("EHR with id %s does not exist.", ehrId));
        }

        ehrService.adminDeleteEhr(ehrUuid);

        return ResponseEntity.noContent().build();
    }
}
