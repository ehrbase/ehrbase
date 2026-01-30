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
package org.ehrbase.rest.admin;

import static org.ehrbase.api.rest.HttpRestContext.QUERY_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.StoredQueryService;
import org.ehrbase.rest.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnMissingBean(name = "primaryadmindefinitioncontroller")
@ConditionalOnProperty(prefix = "admin-api", name = "active")
@Tag(name = "Admin - Stored-Query")
@RestController
@RequestMapping(
        path = BaseController.ADMIN_API_CONTEXT_PATH + "/query",
        produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
public class AdminQueryController extends BaseController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final StoredQueryService storedQueryService;

    @Autowired
    public AdminQueryController(StoredQueryService storedQueryService) {
        this.storedQueryService = Objects.requireNonNull(storedQueryService);
    }

    @DeleteMapping(value = {"/{qualified_query_name}/{version}"})
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Stored-Query has been deleted successfully"),
                @ApiResponse(responseCode = "401", description = "Client credentials are invalid or have expired."),
                @ApiResponse(
                        responseCode = "403",
                        description = "Client has no permission to access since admin role is missing."),
                @ApiResponse(responseCode = "404", description = "Stored-Query could not be found.")
            })
    public ResponseEntity<Void> deleteStoredQuery(
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
            @PathVariable(value = "version") String version) {

        logger.debug("deleteStoredQuery for the following input: {} , version: {}", qualifiedQueryName, version);

        HttpRestContext.register(
                HttpRestContext.LOCATION,
                fromPath("")
                        .pathSegment(QUERY, qualifiedQueryName, version)
                        .build()
                        .toString());
        storedQueryService.deleteStoredQuery(qualifiedQueryName, version);
        HttpRestContext.register(QUERY_ID, qualifiedQueryName);

        return ResponseEntity.ok().build();
    }
}
