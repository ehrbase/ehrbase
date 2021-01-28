/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.rest.openehr.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ehrbase.api.definitions.QueryMode;
import org.ehrbase.api.service.QueryService;
import org.ehrbase.response.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.response.openehr.ErrorBodyPayload;
import org.ehrbase.response.openehr.QueryDefinitionResponseData;
import org.ehrbase.response.openehr.QueryResponseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping(path = "/rest/openehr/v1/query", produces = MediaType.APPLICATION_JSON_VALUE)
public class OpenehrQueryController extends BaseController {

    final static Logger log = LoggerFactory.getLogger(OpenehrQueryController.class);
    private QueryService queryService;

    private final String QUERY_PARAMETERS = "query_parameters";

    @Autowired
    public OpenehrQueryController(QueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService);
    }

    @GetMapping("/aql{?q, offset, fetch, query_parameter}")
    public ResponseEntity<QueryResponseData> getAdhocQuery(@RequestHeader(value = ACCEPT, required = false) String accept,
                                                           @RequestParam(value = "q") String query,
                                                           @RequestParam(value = "offset", required = false) Integer offset,
                                                           @RequestParam(value = "fetch", required = false) Integer fetch,
                                                           @RequestParam Map<String, Object> queryParameters) {

        //deal with offset and fetch
        if (fetch != null)
            query = withFetch(query, fetch);

        if (offset != null)
            query = withOffset(query, offset);

        if (query != null) {
            QueryResponseData queryResponseData;

            if (queryParameters != null && !queryParameters.isEmpty())
                queryResponseData = new QueryResponseData(queryService.query(query, queryParameters, QueryMode.AQL, false));
            else
                queryResponseData = new QueryResponseData(queryService.query(query, QueryMode.AQL, false));

            if (queryResponseData.getRows().size() > 0)
                return ResponseEntity.ok(queryResponseData);
            else
                return ResponseEntity.noContent().build();
        } else
            return missingRequestResponseEntity();
    }

    @PostMapping("/aql")
    public ResponseEntity<QueryResponseData> postAdhocQuery(@RequestHeader(value = ACCEPT, required = false) String accept,
                                                            @RequestHeader(value = CONTENT_TYPE) String contentType,
                                                            @RequestBody String query) {

        log.debug("Got following input: " + query);

        //get the query and parameters if any
        Gson gson = new GsonBuilder().create();

        Map<String, Object> mapped = gson.fromJson(query, Map.class);

        String aql = (String) mapped.get("q");
        Map<String, Object> parameters = (Map<String, Object>) mapped.get(QUERY_PARAMETERS);

        QueryResponseData queryResponseData = null;

        if (aql != null) {
            aql = withOffsetLimit(aql, mapped);

            //get the query and pass it to the service
            if (parameters != null && !parameters.isEmpty())
                queryResponseData = new QueryResponseData(queryService.query(aql, parameters, QueryMode.AQL, false));
            else
                queryResponseData = new QueryResponseData(queryService.query(aql, QueryMode.AQL, false));
        } else
            return missingRequestResponseEntity();

        if (queryResponseData == null)
            return ResponseEntity.noContent().build();
            //NB. Empty result -> HTTP 200 with empty columns and rows (EtherCIS previously returned 204, but I think it's wrong)
//        else if (queryResponseData.getRows().size() == 0)
//            return ResponseEntity.noContent().build();
        else
            return ResponseEntity.ok(queryResponseData);

    }

    private String withFetch(String query, String value) {
        return withFetch(query, double2int(value));
    }

    private String withFetch(String query, Integer value) {
        return query + " LIMIT " + value;
    }

    private String withOffset(String query, String value) {
        return withOffset(query, double2int(value));
    }

    private String withOffset(String query, Integer value) {
        return query + " OFFSET " + value;
    }

    private Integer double2int(String value) {
        return (Double.valueOf(value.toString())).intValue();
    }

    @GetMapping(value = {"/{qualified_query_name}/{version}{?offset,fetch,query_parameter}", "/{qualified_query_name}{?offset,fetch,query_parameter}"})
    public ResponseEntity<QueryResponseData> getStoredQuery(@RequestHeader(value = ACCEPT, required = false) String accept,
                                                            @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
                                                            @PathVariable(value = "version") Optional<String> version,
                                                            @RequestParam(value = "offset", required = false) Integer offset,
                                                            @RequestParam(value = "fetch", required = false) Integer fetch,
                                                            @RequestParam Map<String, Object> queryParameter) {

        log.debug("getStoredQuery not implemented but got following input: " + qualifiedQueryName + " - " + version + " - " + offset + " - " + fetch + " - " + queryParameter);

        //retrieve the stored query for execution
        QueryDefinitionResultDto queryDefinitionResultDto = queryService.retrieveStoredQuery(qualifiedQueryName, version.isPresent() ? version.get() : "LATEST");

        String query = queryDefinitionResultDto.getQueryText();

        if (fetch != null) {
            //append LIMIT clause to aql
            query = withFetch(query, fetch);
        }

        if (offset != null) {
            //append OFFSET clause to aql
            query = withOffset(query, offset);
        }

        QueryResponseData queryResponseData = invoke(query, queryParameter);

        if (queryResponseData == null) {
            return ResponseEntity.noContent().build();
        } else {
            queryResponseData.setName(queryDefinitionResultDto.getQualifiedName() + "/" + queryDefinitionResultDto.getVersion());
            return ResponseEntity.ok(queryResponseData);
        }

    }

    @PostMapping(value = {"/{qualified_query_name}/{version}", "/{qualified_query_name}"})
    public ResponseEntity<QueryResponseData> postStoredQuery(@RequestHeader(value = ACCEPT, required = false) String accept,
                                                             @RequestHeader(value = CONTENT_TYPE) String contentType,
                                                             // TODO: what is this header about? couldn't be clarified and will be discussed with openEHR REST API people
                                                             @RequestHeader(value = IF_NONE_MATCH, required = false) String ifNoneMatch,
                                                             @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
                                                             @PathVariable(value = "version") Optional<String> version,
                                                             @RequestBody(required = false) String parameterBody) {

        log.debug("postStoredQuery with the following input: " + qualifiedQueryName + " - " + version + " - " + parameterBody);

        //retrieve the stored query for execution

        QueryDefinitionResultDto queryDefinitionResultDto = queryService.retrieveStoredQuery(qualifiedQueryName, version.isPresent() ? version.get() : "LATEST");

        String query = queryDefinitionResultDto.getQueryText();

        if (query != null) {
            //retrieve the parameter from body
            //get the query and parameters if any
            Map<String, Object> queryParameter = null;

            if (parameterBody != null && !parameterBody.isEmpty()) {
                Gson gson = new GsonBuilder().create();
                Map<String, Object> mapped = gson.fromJson(parameterBody, Map.class);
                queryParameter = (Map<String, Object>) mapped.get(QUERY_PARAMETERS);

                query = withOffsetLimit(query, mapped);

            }
            QueryResponseData queryResponseData = invoke(query, queryParameter);

            if (queryResponseData == null) {
                return badRequestResponseEntity(qualifiedQueryName, version);
            } else {
                queryResponseData.setName(queryDefinitionResultDto.getQualifiedName() + "/" + queryDefinitionResultDto.getVersion());
                return ResponseEntity.ok(queryResponseData);
            }
        } else {
            return badRequestResponseEntity(qualifiedQueryName, version);
        }
    }

    public ResponseEntity badRequestResponseEntity(String qualifiedQueryName, Optional<String> version) {
        String errorBody = new ErrorBodyPayload("Invalid query", "could not retrieve query identified by:" + qualifiedQueryName + "/" + (version.isPresent() ? version.get() : "LATEST")).toString();
        return new ResponseEntity(errorBody, HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity missingRequestResponseEntity() {
        String errorBody = new ErrorBodyPayload("Invalid query", "no aql query provided").toString();
        return new ResponseEntity(errorBody, HttpStatus.BAD_REQUEST);
    }

    QueryResponseData invoke(String query, Map<String, Object> queryParameter) {
        QueryResponseData queryResponseData;

        if (queryParameter != null && !queryParameter.isEmpty()) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.putAll(queryParameter);
            queryResponseData = new QueryResponseData(queryService.query(query, parameters, QueryMode.AQL, false));
        } else
            queryResponseData = new QueryResponseData(queryService.query(query, QueryMode.AQL, false));

        return queryResponseData;
    }

    String withOffsetLimit(String query, Map<String, Object> mapped) {
        if (mapped.containsKey("fetch")) {
            //append LIMIT clause to aql
            query = withFetch(query, mapped.get("fetch").toString());
        }

        if (mapped.containsKey("offset")) {
            //append OFFSET clause to aql
            query = withOffset(query, mapped.get("offset").toString());
        }

        return query;
    }
}
