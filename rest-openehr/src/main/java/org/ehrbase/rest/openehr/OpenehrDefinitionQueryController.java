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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.Optional;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.audit.msg.AuditMsgBuilder;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.GeneralRequestProcessingException;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.exception.UnsupportedMediaTypeException;
import org.ehrbase.api.service.QueryService;
import org.ehrbase.openehr.sdk.response.dto.ErrorBodyPayload;
import org.ehrbase.openehr.sdk.response.dto.QueryDefinitionListResponseData;
import org.ehrbase.openehr.sdk.response.dto.QueryDefinitionResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.DefinitionQueryApiSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@TenantAware
@RestController
@RequestMapping(
        path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/definition/query",
        produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
public class OpenehrDefinitionQueryController extends BaseController implements DefinitionQueryApiSpecification {

    private static final String AQL = "AQL";

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
    @Override
    @GetMapping(value = {"/{qualified_query_name}", ""})
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_READ)
    public ResponseEntity<QueryDefinitionListResponseData> getStoredQueryList(
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "qualified_query_name", required = false) String qualifiedQueryName) {

        logger.debug("getStoredQueryList invoked with the following input: {}", qualifiedQueryName);

        createAuditLogsMsgBuilder(qualifiedQueryName, null);
        QueryDefinitionListResponseData responseData =
                new QueryDefinitionListResponseData(queryService.retrieveStoredQueries(qualifiedQueryName));
        AuditMsgBuilder.getInstance().setQueryId(qualifiedQueryName);

        return ResponseEntity.ok(responseData);
    }

    @Override
    @GetMapping(value = {"/{qualified_query_name}/{version}"})
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_READ)
    public ResponseEntity<QueryDefinitionResponseData> getStoredQueryVersion(
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
            @PathVariable(value = "version") Optional<String> version) {

        logger.debug(
                "getStoredQueryVersion invoked with the following input: {}, version:{}", qualifiedQueryName, version);

        createAuditLogsMsgBuilder(qualifiedQueryName, version.orElse(null));

        QueryDefinitionResponseData queryDefinitionResponseData = new QueryDefinitionResponseData(
                queryService.retrieveStoredQuery(qualifiedQueryName, version.orElse(null)));
        AuditMsgBuilder.getInstance().setQueryId(qualifiedQueryName);

        return ResponseEntity.ok(queryDefinitionResponseData);
    }

    @Override
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_CREATE)
    @PutMapping(
            value = {"/{qualified_query_name}/{version}", "/{qualified_query_name}"},
            consumes = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE},
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<QueryDefinitionResponseData> putStoredQuery(
            @RequestHeader(value = CONTENT_TYPE, required = false) String contentType,
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
            @PathVariable(value = "version") Optional<String> version,
            @RequestParam(value = "type", required = false, defaultValue = "AQL") String type,
            @RequestBody String queryPayload) {

        logger.debug(
                "putStoreQuery invoked with the following input: {}, version: {}, query: {}, type: {}",
                qualifiedQueryName,
                version,
                queryPayload,
                type);

        if (!AQL.equalsIgnoreCase(type)) {
            return new ResponseEntity(
                    new ErrorBodyPayload("Invalid query", String.format("Query type:%s not supported!", type))
                            .toString(),
                    HttpStatus.BAD_REQUEST);
        }

        MediaType mediaType = MediaType.parseMediaType(contentType);
        String aql;
        if (APPLICATION_JSON.isCompatibleWith(mediaType)) {
            // assume same format as adhoc POST
            aql = Optional.of(queryPayload)
                    .map(p -> {
                        try {
                            return new ObjectMapper().readTree(p);
                        } catch (JsonProcessingException e) {
                            throw new GeneralRequestProcessingException("Invalid content format", e);
                        }
                    })
                    .map(n -> n.get("q"))
                    .filter(JsonNode::isTextual)
                    .map(JsonNode::asText)
                    .orElse(null);

        } else if (TEXT_PLAIN.isCompatibleWith(mediaType)) {
            aql = queryPayload;
        } else {
            throw new UnsupportedMediaTypeException(mediaType.getType());
        }

        if (isBlank(aql)) {
            return new ResponseEntity(
                    new ErrorBodyPayload("Invalid query", "no aql query provided").toString(), HttpStatus.BAD_REQUEST);
        }

        createAuditLogsMsgBuilder(qualifiedQueryName, version.orElse(null));

        QueryDefinitionResultDto storedQuery =
                queryService.createStoredQuery(qualifiedQueryName, version.orElse(null), aql);
        AuditMsgBuilder.getInstance().setQueryId(qualifiedQueryName);

        return getPutDefenitionResponseEntity(mediaType, storedQuery);
    }

    @Override
    @DeleteMapping(value = {"/{qualified_query_name}/{version}"})
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_DELETE)
    public ResponseEntity<QueryDefinitionResponseData> deleteStoredQuery(
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
            @PathVariable(value = "version") String version) {

        logger.debug("deleteStoredQuery for the following input: {} , version: {}", qualifiedQueryName, version);

        createAuditLogsMsgBuilder(qualifiedQueryName, version);

        QueryDefinitionResponseData queryDefinitionResponseData =
                new QueryDefinitionResponseData(queryService.deleteStoredQuery(qualifiedQueryName, version));
        AuditMsgBuilder.getInstance().setQueryId(qualifiedQueryName);

        return ResponseEntity.ok(queryDefinitionResponseData);
    }

    private ResponseEntity<QueryDefinitionResponseData> getPutDefenitionResponseEntity(
            MediaType mediaType, QueryDefinitionResultDto storedQuery) {
        if (APPLICATION_JSON.isCompatibleWith(mediaType)) {
            return ResponseEntity.ok(new QueryDefinitionResponseData(storedQuery));
        } else if (TEXT_PLAIN.isCompatibleWith(mediaType)) {
            HttpHeaders respHeaders = new HttpHeaders();
            respHeaders.setContentType(APPLICATION_JSON);
            respHeaders.setLocation(
                    createLocationUri(DEFINITION, QUERY, storedQuery.getQualifiedName(), storedQuery.getVersion()));

            return ResponseEntity.ok().headers(respHeaders).build();
        } else {
            throw new UnexpectedSwitchCaseException(mediaType.getType());
        }
    }

    private void createAuditLogsMsgBuilder(String queryName, @Nullable String version) {
        AuditMsgBuilder.getInstance()
                .setLocation(fromPath("")
                        .pathSegment(DEFINITION, QUERY, queryName, version)
                        .build()
                        .toString());
    }
}
