/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.rest.openehr;

import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.dto.SystemOptionsDto;
import org.ehrbase.api.service.StatusService;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.SystemApiSpecification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the System API of the openEHR REST API: options and conformance of the running solution.
 */
@ConditionalOnMissingBean(name = "primaryopenehrsystemcontroller")
@RestController
@RequestMapping(path = BaseController.API_CONTEXT_PATH_WITH_VERSION)
public class OpenehrSystemController extends BaseController implements SystemApiSpecification {

    private static final String RESTAPI_SPECS_VERSION = "1.0.3";
    private static final String CONFORMANCE_PROFILE = "STANDARD";
    private static final List<String> ENDPOINTS = List.of("/ehr", "/definition", "/query", "/admin");

    private final StatusService statusService;
    private final SystemOptionsProperties properties;

    public OpenehrSystemController(StatusService statusService, SystemOptionsProperties properties) {
        this.statusService = Objects.requireNonNull(statusService);
        this.properties = Objects.requireNonNull(properties);
    }

    @RequestMapping(method = RequestMethod.OPTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ResponseEntity<SystemOptionsDto> options() {
        SystemOptionsDto systemOptions = new SystemOptionsDto(
                properties.solution(),
                solutionVersion(statusService.getEhrbaseVersion()),
                properties.vendor(),
                RESTAPI_SPECS_VERSION,
                CONFORMANCE_PROFILE,
                ENDPOINTS);

        return ResponseEntity.ok()
                .allow(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.OPTIONS)
                .body(systemOptions);
    }

    /**
     * The build version gets a {@code v} prefix when it starts with a digit, anything else (e.g. unknown) is kept.
     */
    private static String solutionVersion(String buildVersion) {
        if (StringUtils.isNotEmpty(buildVersion) && Character.isDigit(buildVersion.charAt(0))) {
            return "v" + buildVersion;
        }
        return buildVersion;
    }
}
