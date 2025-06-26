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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.AqlQueryService;
import org.ehrbase.api.service.StoredQueryService;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.QueryApiSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
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
            @RequestParam(name = Q_PARAM) String queryString,
            @RequestParam(name = OFFSET_PARAM, required = false) Integer offset,
            @RequestParam(name = FETCH_PARAM, required = false) Integer fetch,
            @RequestParam(name = QUERY_PARAMETERS, required = false) Map<String, Object> queryParameters,
            @RequestHeader(name = ACCEPT, required = false) String accept) {

        // Enriches request attributes with aql for later audit processing
        HttpRestContext.register(QUERY_EXECUTE_ENDPOINT, Boolean.TRUE);

        // get the query and pass it to the service
        AqlQueryRequest aqlQueryRequest = createRequest(
                queryString,
                queryParameters,
                Optional.ofNullable(fetch).map(Integer::longValue),
                Optional.ofNullable(offset).map(Integer::longValue));
        QueryResultDto aqlQueryResult = aqlQueryService.query(aqlQueryRequest);

        // create and return response
        QueryResponseData queryResponseData =
                createQueryResponse(aqlQueryResult, queryString, createLocationUri("query", "aql"));

        return ResponseEntity.ok(queryResponseData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping(
            path = "/aql",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<QueryResponseData> executeAdHocQuery(
            @RequestBody Map<String, Object> queryRequest,
            @RequestHeader(name = ACCEPT, required = false) String accept,
            @RequestHeader(name = CONTENT_TYPE) String contentType) {

        logger.debug("Got following input: {}", queryRequest);

        // sanity check
        Object rawQuery = queryRequest.get(Q_PARAM);
        String queryString =
                switch (rawQuery) {
                    case null -> throw new InvalidApiParameterException("No aql query provided");
                    case ArrayList<?> __ -> throw new InvalidApiParameterException("Multiple aql queries provided");
                    case String s -> s;
                    default -> throw new InvalidApiParameterException("Data type of aql query not supported");
                };

        // Enriches request attributes with aql for later audit processing
        HttpRestContext.register(QUERY_EXECUTE_ENDPOINT, Boolean.TRUE);

        // get the query and pass it to the service
        AqlQueryRequest aqlQueryRequest = createRequest(queryString, queryRequest);
        QueryResultDto aqlQueryResult = aqlQueryService.query(aqlQueryRequest);

        // create and return response
        QueryResponseData queryResponseData = createQueryResponse(aqlQueryResult, queryString, null);

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

        String queryString = queryDefinition.getQueryText();

        // get the query and pass it to the service
        AqlQueryRequest aqlQueryRequest = createRequest(
                queryString,
                queryParameters,
                Optional.ofNullable(fetch).map(Integer::longValue),
                Optional.ofNullable(offset).map(Integer::longValue));
        QueryResultDto aqlQueryResult = aqlQueryService.query(aqlQueryRequest);

        // use the fully qualified metadata location
        Stream<String> pathSegments =
                Stream.of("query", qualifiedQueryName, version).filter(Objects::nonNull);
        URI locationUri = createLocationUri(pathSegments.toArray(String[]::new));

        // create and return response
        QueryResponseData queryResponseData = createQueryResponse(aqlQueryResult, queryString, locationUri);
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
            @RequestBody(required = false) Map<String, Object> queryRequest) {

        logger.trace("postStoredQuery with the following input: {}, {}, {}", qualifiedQueryName, version, queryRequest);

        // Enriches request attributes with aql for later audit processing
        createRestContext(qualifiedQueryName, version);

        QueryDefinitionResultDto queryDefinition = storedQueryService.retrieveStoredQuery(qualifiedQueryName, version);

        String queryString = queryDefinition.getQueryText();

        if (queryString == null) {
            var message = MessageFormat.format("Could not retrieve AQL {0}/{1}", qualifiedQueryName, version);
            throw new ObjectNotFoundException("AQL", message);
        }

        // get the query and pass it to the service
        AqlQueryRequest aqlQueryRequest = createRequest(queryString, queryRequest);
        QueryResultDto aqlQueryResult = aqlQueryService.query(aqlQueryRequest);

        // create and return response
        QueryResponseData queryResponseData = createQueryResponse(aqlQueryResult, queryString, null);
        setQueryName(queryDefinition, queryResponseData);

        HttpRestContext.register(QUERY_ID, queryDefinition.getQualifiedName());

        return ResponseEntity.ok(queryResponseData);
    }

    private void createRestContext(String qualifiedName, @Nullable String version) {
        HttpRestContext.register(
                QUERY_EXECUTE_ENDPOINT,
                Boolean.TRUE,
                HttpRestContext.LOCATION,
                fromPath("")
                        .pathSegment(Q_PARAM, qualifiedName, version)
                        .build()
                        .toString());
    }

    @SuppressWarnings("unchecked")
    private AqlQueryRequest createRequest(@NonNull String queryString, Map<String, Object> requestBody) {

        requestBody = Optional.ofNullable(requestBody).orElseGet(Map::of);
        Map<String, Object> queryParameters = Optional.ofNullable(requestBody.get(QUERY_PARAMETERS))
                .map(p -> (Map<String, Object>) p)
                .orElseGet(Map::of);
        Optional<Long> fetch = optionalLong(FETCH_PARAM, requestBody);
        Optional<Long> offset = optionalLong(OFFSET_PARAM, requestBody);

        return createRequest(queryString, queryParameters, fetch, offset);
    }

    private AqlQueryRequest createRequest(
            @NonNull String queryString, Map<String, Object> parameters, Optional<Long> fetch, Optional<Long> offset) {

        return AqlQueryRequest.parse(queryString, parameters, fetch.orElse(null), offset.orElse(null));
    }

    protected QueryResponseData createQueryResponse(
            QueryResultDto aqlQueryResult, String queryString, @Nullable URI location) {
        final QueryResponseData queryResponseData = new QueryResponseData(aqlQueryResult);
        queryResponseData.setQuery(queryString);
        queryResponseData.setMeta(aqlQueryContext.createMetaData(location));
        return queryResponseData;
    }

    // --- Helper ---

    private static void setQueryName(
            QueryDefinitionResultDto queryDefinitionResultDto, QueryResponseData queryResponseData) {
        queryResponseData.setName(
                queryDefinitionResultDto.getQualifiedName() + "/" + queryDefinitionResultDto.getVersion());
    }

    private static Optional<Long> optionalLong(String name, Map<String, Object> params) {
        return Optional.of(name).map(params::get).map(o -> switch (o) {
            case Integer i -> i.longValue();
            case Long l -> l;
            case String s -> {
                try {
                    yield Long.valueOf(s);
                } catch (NumberFormatException e) {
                    throw new InvalidApiParameterException("invalid '%s' value '%s'".formatted(name, s));
                }
            }
            default -> throw new InvalidApiParameterException("invalid '%s' value '%s'".formatted(name, o));
        });
    }
}
