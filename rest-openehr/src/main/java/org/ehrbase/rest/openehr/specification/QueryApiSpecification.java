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
package org.ehrbase.rest.openehr.specification;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.springframework.http.ResponseEntity;

/**
 * OpenAPI specification for openEHR REST API QUERY resource.
 */
@Tag(name = "QUERY")
@SuppressWarnings({"unused", "java:S107"})
public interface QueryApiSpecification {

    /**
     * Execute ad-hoc (non-stored) AQL query.
     */
    @Operation(
            summary = "Execute ad-hoc (non-stored) AQL query",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/query.html#query-execute-query-get"))
    ResponseEntity<QueryResponseData> executeAdHocQuery(
            String accept,
            Integer offset,
            Integer fetch,
            Map<String, Object> queryParameters,
            String query,
            @ApiParameter.PrettyPrint String pretty);

    /**
     * Execute ad-hoc (non-stored) AQL query.
     */
    @Operation(
            summary = "Execute ad-hoc (non-stored) AQL query",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/query.html#query-execute-query-post"))
    ResponseEntity<QueryResponseData> executeAdHocQuery(
            String contentType,
            String accept,
            @ApiParameter.PrettyPrint String pretty,
            Map<String, Object> queryRequest);

    /**
     * Execute stored query.
     */
    @Operation(
            summary = "Execute stored query",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/query.html#query-execute-query-get-1"))
    ResponseEntity<QueryResponseData> executeStoredQuery(
            String accept,
            String version,
            Integer offset,
            Integer fetch,
            Map<String, Object> queryParameter,
            @ApiParameter.PrettyPrint String pretty,
            String qualifiedQueryName);

    /**
     * Execute stored query.
     */
    @Operation(
            summary = "Execute stored query",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/query.html#query-execute-query-post-1"))
    ResponseEntity<QueryResponseData> executeStoredQuery(
            String accept,
            String contentType,
            String version,
            String qualifiedQueryName,
            @ApiParameter.PrettyPrint String pretty,
            Map<String, Object> queryRequest);
}
