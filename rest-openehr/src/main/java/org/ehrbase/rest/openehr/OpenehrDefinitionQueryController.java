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
package org.ehrbase.rest.openehr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.service.QueryService;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.response.openehr.ErrorBodyPayload;
import org.ehrbase.response.openehr.QueryDefinitionListResponseData;
import org.ehrbase.response.openehr.QueryDefinitionResponseData;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.DefinitionQueryApiSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@TenantAware
@RestController
@RequestMapping(
        path = "${openehr-api.context-path:/rest/openehr}/v1/definition/query",
        produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
public class OpenehrDefinitionQueryController extends BaseController implements DefinitionQueryApiSpecification {

    static final Logger log = LoggerFactory.getLogger(OpenehrDefinitionQueryController.class);

    private final QueryService queryService;

    @Autowired
    public OpenehrDefinitionQueryController(QueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService);
    }

    // ----- DEFINITION: Manage Stored Query, From definition package
    // https://openehr.github.io/specifications-ITS-REST/definitions.html#definitions-stored-query

    /**
     * Get a stored query
     *
     * @param accept
     * @param qualifiedQueryName
     * @return
     */
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_READ)
    @RequestMapping(
            value = {"/{qualified_query_name}", ""},
            method = RequestMethod.GET)
    @Override
    public ResponseEntity<QueryDefinitionListResponseData> getStoredQueryList(
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "qualified_query_name", required = false) String qualifiedQueryName) {

        log.debug("getStoredQueryList invoked with the following input: " + qualifiedQueryName);

        QueryDefinitionListResponseData responseData =
                new QueryDefinitionListResponseData(queryService.retrieveStoredQueries(qualifiedQueryName));
        return ResponseEntity.ok(responseData);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_READ)
    @RequestMapping(
            value = {"/{qualified_query_name}/{version}"},
            method = RequestMethod.GET) //
    @Override
    public ResponseEntity<QueryDefinitionResponseData> getStoredQueryVersion(
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
            @PathVariable(value = "version") Optional<String> version) {

        log.debug("getStoredQueryVersion invoked with the following input: " + qualifiedQueryName + ", version:"
                + version);

        QueryDefinitionResponseData queryDefinitionResponseData = new QueryDefinitionResponseData(
                queryService.retrieveStoredQuery(qualifiedQueryName, version.isPresent() ? version.get() : null));

        return ResponseEntity.ok(queryDefinitionResponseData);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_CREATE)
    @PutMapping( value = {"/{qualified_query_name}/{version}", "/{qualified_query_name}"})
    @Override
    public ResponseEntity<QueryDefinitionResponseData> putStoreQuery(
            @RequestHeader(value = CONTENT_TYPE, required = false) String contentType,
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
            @PathVariable(value = "version") Optional<String> version,
            @RequestParam(value = "type", required = false, defaultValue = "AQL") String type,
            @RequestBody String queryPayload) {

        log.debug("putStoreQuery invoked with the following input: " + qualifiedQueryName + ", version:" + version
                + ", query:" + queryPayload + ", type=" + type);

        var format = extractCompositionFormat(contentType);
        String aql;
        switch (format) {
            case JSON: {
                // use the payload from adhoc POST:
                // get the query and parameters if any
                Gson gson = new GsonBuilder().create();
                Map<String, Object> mapped = gson.fromJson(queryPayload, Map.class);
                aql = (String) mapped.get("q");
                break;
            }
            case TEXT: {
                aql = queryPayload;
                break;
            }
            default:
                throw new UnexpectedSwitchCaseException(format);
        }

        if (aql == null || aql.isEmpty())
            return new ResponseEntity(
                    new ErrorBodyPayload("Invalid query", "no aql query provided in payload").toString(),
                    HttpStatus.BAD_REQUEST);

        return internalPutDefinitionProcessing(qualifiedQueryName, version, format, aql);
    }

    private ResponseEntity<QueryDefinitionResponseData> internalPutDefinitionProcessing(String qualifiedQueryName,
                              Optional<String> version, CompositionFormat format, String aql) {
        QueryDefinitionResultDto storedQuery =
                queryService.createStoredQuery(qualifiedQueryName, version.orElse(null), aql);

        switch (format) {
            case JSON: {
                return ResponseEntity.ok(new QueryDefinitionResponseData(storedQuery));
            }
            case TEXT: {
                HttpHeaders respHeaders = new HttpHeaders();
                respHeaders.setContentType(resolveContentType(APPLICATION_JSON_VALUE));
                respHeaders.setLocation(URI.create(this.encodePath(getBaseEnvLinkURL())));
                return ResponseEntity.ok()
                        .headers(respHeaders)
                        .build();
            }
            default:
                throw new UnexpectedSwitchCaseException(format);
        }
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_DELETE)
    @RequestMapping(
            value = {"/{qualified_query_name}/{version}"},
            method = RequestMethod.DELETE)
    @Override
    public ResponseEntity<QueryDefinitionResponseData> deleteStoredQuery(
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
            @PathVariable(value = "version") String version) {

        log.debug("deleteStoredQuery for the following input: {} , version: {}", qualifiedQueryName, version);

        QueryDefinitionResponseData queryDefinitionResponseData =
                new QueryDefinitionResponseData(queryService.deleteStoredQuery(qualifiedQueryName, version));

        return ResponseEntity.ok(queryDefinitionResponseData);
    }
}
