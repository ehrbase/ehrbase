/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Optional;
import org.ehrbase.openehr.sdk.response.dto.QueryDefinitionListResponseData;
import org.ehrbase.openehr.sdk.response.dto.QueryDefinitionResponseData;
import org.springframework.http.ResponseEntity;

@Tag(name = "STORED_QUERY")
@SuppressWarnings("java:S107")
public interface DefinitionQueryApiSpecification {

    @Operation(
            summary = "List stored queries",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-stored-query-get"))
    ResponseEntity<QueryDefinitionListResponseData> getStoredQueryList(String accept, String qualifiedQueryName);

    @Operation(
            summary = "Store a query",
            description =
                    "Content type application/json is still supported but it's deprecated, please use text/plain instead.",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-stored-query-put"))
    ResponseEntity<QueryDefinitionResponseData> putStoredQuery(
            String contentType,
            String accept,
            String qualifiedQueryName,
            Optional<String> version,
            String type,
            String queryPayload);

    @Operation(
            summary = "Get stored query and info/metadata",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-stored-query-get-1"))
    ResponseEntity<QueryDefinitionResponseData> getStoredQueryVersion(
            String accept, String qualifiedQueryName, Optional<String> version);

    @Operation(summary = "Delete a query", hidden = true)
    ResponseEntity<QueryDefinitionResponseData> deleteStoredQuery(
            String accept, String qualifiedQueryName, String version);
}
