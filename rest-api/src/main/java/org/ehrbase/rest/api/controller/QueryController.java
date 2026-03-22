/*
 * Copyright (c) 2024 vitasystems GmbH.
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
package org.ehrbase.rest.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.ViewCatalogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v2 controller for listing available views from the view catalog.
 */
@RestController
@RequestMapping("/api/v2/query")
@Tag(name = "View Catalog", description = "List available views from ehr_system.view_catalog")
public class QueryController extends BaseApiController {

    private final ViewCatalogService viewCatalogService;
    private final RequestContext requestContext;

    public QueryController(ViewCatalogService viewCatalogService, RequestContext requestContext) {
        this.viewCatalogService = viewCatalogService;
        this.requestContext = requestContext;
    }

    @GetMapping(value = "/views", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "List available views",
            description = "Returns all views from ehr_system.view_catalog with column metadata")
    public ResponseEntity<List<ViewCatalogService.ViewCatalogEntry>> listViews() {
        List<ViewCatalogService.ViewCatalogEntry> views =
                viewCatalogService.listViews(null, requestContext.getTenantId());
        return ResponseEntity.ok(views);
    }
}
