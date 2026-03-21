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
import java.util.stream.Collectors;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.rest.api.dto.QueryRequestDto;
import org.ehrbase.rest.api.dto.QueryResponseDto;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.ViewCatalogService;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v1 controller for SQL query execution against {@code ehr_views} schema.
 * Read-only, parameterized queries only. RLS enforced automatically.
 */
@RestController
@RequestMapping("/api/v1/query")
@Tag(name = "Query", description = "SQL query execution against ehr_views and view catalog listing")
public class QueryController extends BaseApiController {

    private final DSLContext dsl;
    private final ViewCatalogService viewCatalogService;
    private final RequestContext requestContext;

    public QueryController(DSLContext dsl, ViewCatalogService viewCatalogService, RequestContext requestContext) {
        this.dsl = dsl;
        this.viewCatalogService = viewCatalogService;
        this.requestContext = requestContext;
    }

    @PostMapping(
            value = "/sql",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Execute read-only SQL against ehr_views",
            description = "Parameterized queries only. RLS enforced automatically via tenant session variables.")
    public ResponseEntity<QueryResponseDto> executeSql(@RequestBody QueryRequestDto request) {
        if (request.sql() == null || request.sql().isBlank()) {
            throw new InvalidApiParameterException("SQL query must not be empty");
        }

        String sql = request.sql().strip();
        if (!sql.toUpperCase().startsWith("SELECT")) {
            throw new InvalidApiParameterException("Only SELECT queries are allowed");
        }

        if (sql.toUpperCase().contains("EHR_SYSTEM.") || sql.toUpperCase().contains("EHR_DATA.")) {
            throw new InvalidApiParameterException("Queries must target ehr_views schema only");
        }

        int timeout = request.timeout() != null ? request.timeout() : 30;
        dsl.execute("SET LOCAL statement_timeout = '%ds'".formatted(timeout));

        long start = System.currentTimeMillis();
        Result<Record> result = dsl.resultQuery(sql).fetch();
        long elapsed = System.currentTimeMillis() - start;

        List<QueryResponseDto.ColumnMeta> columns = result.fields().length > 0
                ? java.util.Arrays.stream(result.fields())
                        .map(f -> new QueryResponseDto.ColumnMeta(
                                f.getName(), f.getDataType().getTypeName()))
                        .toList()
                : List.of();

        List<java.util.Map<String, Object>> rows =
                result.stream().map(Record::intoMap).collect(Collectors.toList());

        var meta = new QueryResponseDto.QueryMeta(rows.size(), elapsed);
        return ResponseEntity.ok(new QueryResponseDto(columns, rows, meta));
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

    @PostMapping(
            value = "/sql/explain",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "EXPLAIN ANALYZE a query (admin only)")
    public ResponseEntity<String> explainQuery(@RequestBody QueryRequestDto request) {
        if (request.sql() == null || request.sql().isBlank()) {
            throw new InvalidApiParameterException("SQL query must not be empty");
        }

        String sql = "EXPLAIN ANALYZE " + request.sql().strip();
        Result<Record> result = dsl.resultQuery(sql).fetch();

        String plan = result.stream().map(r -> r.get(0, String.class)).collect(Collectors.joining("\n"));

        return ResponseEntity.ok(plan);
    }
}
