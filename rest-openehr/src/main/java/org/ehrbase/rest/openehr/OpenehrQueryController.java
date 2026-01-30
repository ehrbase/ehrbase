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
package org.ehrbase.rest.openehr;

import static org.ehrbase.api.rest.HttpRestContext.QUERY_EXECUTE_ENDPOINT;
import static org.ehrbase.api.rest.HttpRestContext.QUERY_ID;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.AqlQueryService;
import org.ehrbase.api.service.StoredQueryService;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.QueryApiSpecification;
import org.ehrbase.rest.util.OpenEhrQueryRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for openEHR REST API QUERY resource.
 */
@ConditionalOnMissingBean(name = "primaryopenehrquerycontroller")
@RestController
@RequestMapping(
        path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/query",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrQueryController extends BaseController implements QueryApiSpecification {

    // request parameter
    private static final String QUERY_PARAMETERS = "query_parameters";
    private static final String FETCH_PARAM = "fetch";
    private static final String OFFSET_PARAM = "offset";
    private static final String Q_PARAM = "q";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AqlQueryService aqlQueryService;
    private final StoredQueryService storedQueryService;
    private final AqlQueryContext aqlQueryContext;

    public OpenehrQueryController(
            AqlQueryService aqlQueryService, StoredQueryService storedQueryService, AqlQueryContext aqlQueryContext) {
        this.aqlQueryService = aqlQueryService;
        this.storedQueryService = storedQueryService;
        this.aqlQueryContext = aqlQueryContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping(path = "/aql")
    public ResponseEntity<QueryResponseData> executeAdHocQuery(
            @RequestParam(name = Q_PARAM) String queryText,
            @RequestParam(name = OFFSET_PARAM, required = false) Integer offset,
            @RequestParam(name = FETCH_PARAM, required = false) Integer fetch,
            @RequestParam(name = QUERY_PARAMETERS, required = false) Map<String, Object> queryParameters,
            @RequestHeader(name = ACCEPT, required = false) String accept) {

        // Enriches request attributes with aql for later audit processing
        HttpRestContext.register(QUERY_EXECUTE_ENDPOINT, Boolean.TRUE);

        // validate received query
        if (StringUtils.isBlank(queryText)) {
            throw new InvalidApiParameterException("No query provided.");
        }

        // prepare query
        AqlQueryRequest queryRequest = AqlQueryRequest.prepare(
                queryText,
                OpenEhrQueryRequestUtils.rewriteExplicitParameterTypes(queryParameters),
                Optional.ofNullable(fetch).map(Integer::longValue).orElse(null),
                Optional.ofNullable(offset).map(Integer::longValue).orElse(null));

        // execute query
        QueryResultDto aqlQueryResult = aqlQueryService.query(queryRequest);

        // create and return response
        QueryResponseData response = createQueryResponse(aqlQueryResult, queryText, createLocationUri("query", "aql"));

        return ResponseEntity.ok(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping(
            path = "/aql",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<QueryResponseData> executeAdHocQuery(
            @RequestBody Map<String, Object> requestBody,
            @RequestHeader(name = ACCEPT, required = false) String accept,
            @RequestHeader(name = CONTENT_TYPE) String contentType) {

        logger.debug("Got following input: {}", requestBody);

        // validate received query
        Object rawQuery = requestBody.get(Q_PARAM);
        String queryText =
                switch (rawQuery) {
                    case null -> throw new InvalidApiParameterException("No query provided.");
                    case Collection<?> __ -> throw new InvalidApiParameterException("Multiple queries provided.");
                    case String s -> {
                        if (StringUtils.isBlank(s)) {
                            throw new InvalidApiParameterException("No query provided.");
                        } else yield s;
                    }
                    default -> throw new InvalidApiParameterException("Data type of query not supported.");
                };

        // Enriches request attributes with aql for later audit processing
        HttpRestContext.register(QUERY_EXECUTE_ENDPOINT, Boolean.TRUE);

        Map<String, Object> params = OpenEhrQueryRequestUtils.getSubMap(requestBody, QUERY_PARAMETERS);

        // prepare query
        AqlQueryRequest queryRequest = AqlQueryRequest.prepare(
                queryText,
                OpenEhrQueryRequestUtils.rewriteExplicitParameterTypes(params),
                OpenEhrQueryRequestUtils.getOptionalLong(requestBody, FETCH_PARAM)
                        .orElse(null),
                OpenEhrQueryRequestUtils.getOptionalLong(requestBody, OFFSET_PARAM)
                        .orElse(null));

        // execute query
        QueryResultDto aqlQueryResult = aqlQueryService.query(queryRequest);

        // create and return response
        QueryResponseData queryResponseData = createQueryResponse(aqlQueryResult, queryText, null);

        return ResponseEntity.ok(queryResponseData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping(path = {"/{qualified_query_name}", "/{qualified_query_name}/{version}"})
    public ResponseEntity<QueryResponseData> executeStoredQuery(
            @PathVariable(name = "qualified_query_name") String qualifiedQueryName,
            @PathVariable(name = "version", required = false) String version,
            @RequestParam(name = OFFSET_PARAM, required = false) Integer offset,
            @RequestParam(name = FETCH_PARAM, required = false) Integer fetch,
            @RequestParam(name = QUERY_PARAMETERS, required = false) Map<String, Object> queryParameters,
            @RequestHeader(name = ACCEPT, required = false) String accept) {

        logger.trace(
                "getStoredQuery with the following input: {} - {} - {} - {} - {}",
                qualifiedQueryName,
                version,
                offset,
                fetch,
                queryParameters);

        createRestContext(qualifiedQueryName, version);

        // retrieve the stored query for execution
        QueryDefinitionResultDto queryDefinition = storedQueryService.retrieveStoredQuery(qualifiedQueryName, version);

        // execute
        QueryResultDto aqlQueryResult =
                executeStoredQuery(queryDefinition, QueryExecutionMetadata.of(queryParameters, offset, fetch));

        // use the fully qualified metadata location
        Stream<String> pathSegments =
                Stream.of("query", qualifiedQueryName, version).filter(Objects::nonNull);
        URI locationUri = createLocationUri(pathSegments.toArray(String[]::new));

        // create and return response
        QueryResponseData queryResponseData = createQueryResponse(aqlQueryResult, null, locationUri);
        setQueryName(queryDefinition, queryResponseData);

        HttpRestContext.register(QUERY_ID, queryDefinition.getQualifiedName());

        return ResponseEntity.ok(queryResponseData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping(
            path = {"/{qualified_query_name}", "/{qualified_query_name}/{version}"},
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<QueryResponseData> executeStoredQuery(
            @PathVariable(name = "qualified_query_name") String qualifiedQueryName,
            @PathVariable(name = "version", required = false) String version,
            @RequestHeader(name = ACCEPT, required = false) String accept,
            @RequestHeader(name = CONTENT_TYPE) String contentType,
            @RequestBody(required = false) Map<String, Object> requestBody) {

        logger.trace("postStoredQuery with the following input: {}, {}, {}", qualifiedQueryName, version, requestBody);

        // Enriches request attributes with aql for later audit processing
        createRestContext(qualifiedQueryName, version);

        QueryDefinitionResultDto queryDefinition = storedQueryService.retrieveStoredQuery(qualifiedQueryName, version);
        QueryResultDto aqlQueryResult =
                executeStoredQuery(queryDefinition, QueryExecutionMetadata.fromRequestBody(requestBody));

        // create and return response
        QueryResponseData queryResponseData = createQueryResponse(aqlQueryResult, null, null);
        setQueryName(queryDefinition, queryResponseData);

        HttpRestContext.register(QUERY_ID, queryDefinition.getQualifiedName());

        return ResponseEntity.ok(queryResponseData);
    }

    protected QueryResultDto executeStoredQuery(
            QueryDefinitionResultDto queryDefinition, QueryExecutionMetadata executionMetadata) {

        AqlQueryRequest queryRequest = AqlQueryRequest.prepareNamed(
                queryDefinition.getQueryText(),
                queryDefinition.getQualifiedName() + "/" + queryDefinition.getVersion(),
                OpenEhrQueryRequestUtils.rewriteExplicitParameterTypes(executionMetadata.queryParameters()),
                executionMetadata.fetch(),
                executionMetadata.offset());

        return aqlQueryService.query(queryRequest);
    }

    protected QueryResponseData createQueryResponse(QueryResultDto aqlQueryResult, String queryString, URI location) {
        final QueryResponseData queryResponseData = new QueryResponseData(aqlQueryResult);
        queryResponseData.setQuery(queryString);
        queryResponseData.setMeta(aqlQueryContext.createMetaData(location));
        return queryResponseData;
    }

    protected record QueryExecutionMetadata(Map<String, Object> queryParameters, Long offset, Long fetch) {
        static QueryExecutionMetadata of(Map<String, Object> queryParameters, Integer offset, Integer fetch) {
            return new QueryExecutionMetadata(
                    queryParameters,
                    Optional.ofNullable(offset).map(Integer::longValue).orElse(null),
                    Optional.ofNullable(fetch).map(Integer::longValue).orElse(null));
        }

        static QueryExecutionMetadata fromRequestBody(Map<String, Object> requestBody) {
            Map<String, Object> queryParameters = OpenEhrQueryRequestUtils.getSubMap(requestBody, QUERY_PARAMETERS);

            return new QueryExecutionMetadata(
                    queryParameters,
                    OpenEhrQueryRequestUtils.getOptionalLong(requestBody, OFFSET_PARAM)
                            .orElse(null),
                    OpenEhrQueryRequestUtils.getOptionalLong(requestBody, FETCH_PARAM)
                            .orElse(null));
        }
    }

    private void createRestContext(String qualifiedName, String version) {
        HttpRestContext.register(
                QUERY_EXECUTE_ENDPOINT,
                Boolean.TRUE,
                HttpRestContext.LOCATION,
                fromPath("")
                        .pathSegment(Q_PARAM, qualifiedName, version)
                        .build()
                        .toString());
    }

    private static void setQueryName(
            QueryDefinitionResultDto queryDefinitionResultDto, QueryResponseData queryResponseData) {
        queryResponseData.setName(
                queryDefinitionResultDto.getQualifiedName() + "/" + queryDefinitionResultDto.getVersion());
    }
}
