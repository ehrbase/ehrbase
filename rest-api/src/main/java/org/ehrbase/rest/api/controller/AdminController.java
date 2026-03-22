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
import java.util.Map;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.service.MaterializedViewRefreshService;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v2 admin endpoints for system management.
 * Behind {@code ehrbase.features.admin-api} feature flag.
 */
@RestController
@RequestMapping("/api/v2/admin")
@ConditionalOnProperty(name = "ehrbase.features.admin-api", havingValue = "true")
@Tag(name = "Admin", description = "Administrative operations: health, schema refresh, migration")
public class AdminController extends BaseApiController {

    private final DSLContext dsl;
    private final SystemService systemService;
    private final MaterializedViewRefreshService matViewRefreshService;

    public AdminController(
            DSLContext dsl, SystemService systemService, MaterializedViewRefreshService matViewRefreshService) {
        this.dsl = dsl;
        this.systemService = systemService;
        this.matViewRefreshService = matViewRefreshService;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Detailed system health check")
    public ResponseEntity<Map<String, Object>> health() {
        String pgVersion = dsl.resultQuery("SELECT version()").fetchOne(0, String.class);

        long templateCount = dsl.fetchCount(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("ehr_system", "template")));
        long ehrCount = dsl.fetchCount(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("ehr_system", "ehr")));
        long compositionCount =
                dsl.fetchCount(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("ehr_system", "composition")));

        return ResponseEntity.ok(Map.of(
                "status",
                "UP",
                "system_id",
                systemService.getSystemId(),
                "postgresql_version",
                pgVersion != null ? pgVersion : "unknown",
                "template_count",
                templateCount,
                "ehr_count",
                ehrCount,
                "composition_count",
                compositionCount));
    }

    @PostMapping(value = "/schema/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Refresh materialized views")
    public ResponseEntity<Map<String, Object>> refreshSchema() {
        matViewRefreshService.refreshAllMaterializedViews();
        return ResponseEntity.ok(Map.of("status", "refreshed"));
    }

    @PostMapping(value = "/migrate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger data migration")
    public ResponseEntity<Map<String, Object>> triggerMigration() {
        throw new UnsupportedOperationException("Data migration not yet implemented");
    }

    @GetMapping(value = "/migrate/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get migration progress")
    public ResponseEntity<Map<String, Object>> migrationStatus() {
        throw new UnsupportedOperationException("Data migration not yet implemented");
    }
}
