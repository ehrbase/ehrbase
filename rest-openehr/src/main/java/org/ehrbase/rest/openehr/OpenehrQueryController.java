/*
 * Copyright (c) 2019-2022 vitasystems GmbH and Hannover Medical School.
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

import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.collections4.MapUtils;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.audit.msg.AuditMsgBuilder;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.QueryService;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.QueryApiSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.util.CollectionUtils;
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
 *
 * @author Jake Smolka
 * @author Renaud Subiger
 * @since 1.0
 */
@TenantAware
@RestController
@RequestMapping(path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/query")
public class OpenehrQueryController extends BaseController implements QueryApiSpecification {

    private static final String EHR_ID_VALUE = "ehr_id/value";
    private static final String QUERY_PARAMETERS = "query_parameters";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final QueryService queryService;

    @Autowired(required = true)
    @Qualifier("requestAwareAuditResultMapHolder")
    private RequestAwareAuditResultMapHolder auditResultMapHolder;

    public OpenehrQueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * {@inheritDoc}
     */
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_SEARCH_AD_HOC)
    @Override
    @GetMapping(path = "/aql")
    @PostAuthorize("checkAbacPostQuery(@requestAwareAuditResultMapHolder.getAuditResultMap())")
    public ResponseEntity<QueryResponseData> executeAdHocQuery(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "fetch", required = false) Integer fetch,
            @RequestParam(name = "query_parameters", required = false) Map<String, Object> queryParameters,
            @RequestHeader(name = ACCEPT, required = false) String accept) {

        // deal with offset and fetch
        if (fetch != null) {
            query = withFetch(query, fetch);
        }

        if (offset != null) {
            query = withOffset(query, offset);
        }

        // Enriches request attributes with aql for later audit processing
        createAdHocAuditLogsMsgBuilder();

        var body = executeQuery(query, queryParameters);

        if (!CollectionUtils.isEmpty(body.getRows())) {
            return ResponseEntity.ok(body);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * {@inheritDoc}
     */
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_SEARCH_AD_HOC)
    @Override
    @PostMapping(path = "/aql")
    @PostAuthorize("checkAbacPostQuery(@requestAwareAuditResultMapHolder.getAuditResultMap())")
    @SuppressWarnings("unchecked")
    public ResponseEntity<QueryResponseData> executeAdHocQuery(
            @RequestBody Map<String, Object> queryRequest,
            @RequestHeader(name = ACCEPT, required = false) String accept,
            @RequestHeader(name = CONTENT_TYPE) String contentType) {

        logger.debug("Got following input: {}", queryRequest);

        String aql = (String) queryRequest.get("q");
        if (aql == null) {
            throw new InvalidApiParameterException("No aql query provided");
        }

        aql = withOffsetLimit(aql, queryRequest);
        // Enriches request attributes with aql for later audit processing
        createAdHocAuditLogsMsgBuilder();

        Map<String, Object> parameters = (Map<String, Object>) queryRequest.get(QUERY_PARAMETERS);

        var body = executeQuery(aql, parameters);
        return ResponseEntity.ok(body);
    }

    /**
     * {@inheritDoc}
     */
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_SEARCH)
    @Override
    @GetMapping(path = {"/{qualified_query_name}", "/{qualified_query_name}/{version}"})
    @PostAuthorize("checkAbacPostQuery(@requestAwareAuditResultMapHolder.getAuditResultMap())")
    public ResponseEntity<QueryResponseData> executeStoredQuery(
            @PathVariable(name = "qualified_query_name") String qualifiedQueryName,
            @PathVariable(name = "version", required = false) String version,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "fetch", required = false) Integer fetch,
            @RequestParam(name = "query_parameters", required = false) Map<String, Object> queryParameter,
            @RequestHeader(name = ACCEPT, required = false) String accept) {

        logger.trace(
                "getStoredQuery not implemented but got following input: {} - {} - {} - {} - {}",
                qualifiedQueryName,
                version,
                offset,
                fetch,
                queryParameter);

        createAuditLogsMsgBuilder(qualifiedQueryName, version);
        // retrieve the stored query for execution
        QueryDefinitionResultDto queryResultDto = queryService.retrieveStoredQuery(qualifiedQueryName, version);

        String query = queryResultDto.getQueryText();

        // Enriches request attributes with query name for later audit processing

        if (fetch != null) {
            // append LIMIT clause to aql
            query = withFetch(query, fetch);
        }

        if (offset != null) {
            // append OFFSET clause to aql
            query = withOffset(query, offset);
        }

        QueryResponseData queryResponseData = invoke(query, queryParameter);
        setQueryName(queryResultDto, queryResponseData);
        AuditMsgBuilder.getInstance().setQueryId(queryResultDto.getQualifiedName());

        return ResponseEntity.ok(queryResponseData);
    }

    /**
     * {@inheritDoc}
     */
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_QUERY_SEARCH)
    @Override
    @PostMapping(path = {"/{qualified_query_name}", "/{qualified_query_name}/{version}"})
    @PostAuthorize("checkAbacPostQuery(@requestAwareAuditResultMapHolder.getAuditResultMap())")
    @SuppressWarnings("unchecked")
    public ResponseEntity<QueryResponseData> executeStoredQuery(
            @PathVariable(name = "qualified_query_name") String qualifiedQueryName,
            @PathVariable(name = "version", required = false) String version,
            @RequestHeader(name = ACCEPT, required = false) String accept,
            @RequestHeader(name = CONTENT_TYPE) String contentType,
            @RequestBody(required = false) Map<String, Object> queryRequest) {

        logger.trace("postStoredQuery with the following input: {}, {}, {}", qualifiedQueryName, version, queryRequest);

        // Enriches request attributes with aql for later audit processing
        createAuditLogsMsgBuilder(qualifiedQueryName, version);

        // retrieve the stored query for execution
        QueryDefinitionResultDto queryResultDto = queryService.retrieveStoredQuery(qualifiedQueryName, version);

        String query = queryResultDto.getQueryText();

        if (query == null) {
            var message = MessageFormat.format("Could not retrieve AQL {0}/{1}", qualifiedQueryName, version);
            throw new ObjectNotFoundException("AQL", message);
        }
        // retrieve the parameter from body
        // get the query and parameters if any
        Map<String, Object> queryParameter = null;

        if (queryRequest != null && !queryRequest.isEmpty()) {
            queryParameter = (Map<String, Object>) queryRequest.get(QUERY_PARAMETERS);

            query = withOffsetLimit(query, queryRequest);
        }
        QueryResponseData queryResponseData = invoke(query, queryParameter);

        setQueryName(queryResultDto, queryResponseData);
        AuditMsgBuilder.getInstance().setQueryId(queryResultDto.getQualifiedName());

        return ResponseEntity.ok(queryResponseData);
    }

    private void createAuditLogsMsgBuilder(String qualifiedName, @Nullable String version) {
        AuditMsgBuilder.getInstance()
                .setIsQueryExecuteEndpoint(true)
                .setLocation(fromPath("")
                        .pathSegment(QUERY, qualifiedName, version)
                        .build()
                        .toString());
    }

    private void createAdHocAuditLogsMsgBuilder() {
        AuditMsgBuilder.getInstance().setIsQueryExecuteEndpoint(true);
    }

    private static void setQueryName(
            QueryDefinitionResultDto queryDefinitionResultDto, QueryResponseData queryResponseData) {
        queryResponseData.setName(
                queryDefinitionResultDto.getQualifiedName() + "/" + queryDefinitionResultDto.getVersion());
    }

    private QueryResponseData executeQuery(String aql, Map<String, Object> parameters) {
        QueryResponseData queryResponseData;

        Map<String, Set<Object>> auditResultMap = auditResultMapHolder.getAuditResultMap();

        // get the query and pass it to the service
        queryResponseData = new QueryResponseData(queryService.query(aql, parameters, false, auditResultMap));

        // Enriches request attributes with EhrId(s) for later audit processing
        AuditMsgBuilder.getInstance().setEhrIds(auditResultMap.get(EHR_ID_VALUE));

        return queryResponseData;
    }

    private String withFetch(String query, String value) {
        return withFetch(query, double2int(value));
    }

    private String withFetch(String query, Integer value) {
        return orderedLimitOffset(query, "LIMIT", value);
    }

    private String orderedLimitOffset(String query, String keyword, Integer value) {
        String queryFormatted;

        if (query.replace(" ", "").toUpperCase().contains("ORDERBY")) {
            // insert LIMIT before ORDER BY clause!
            String[] strings = query.split("(?i)ORDER");
            // assemble
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append(strings[0]);
            queryBuilder.append(keyword.toUpperCase());
            queryBuilder.append(" ");
            queryBuilder.append(value);
            queryBuilder.append(" ORDER");
            queryBuilder.append(strings[1]);
            queryFormatted = queryBuilder.toString();
        } else {
            queryFormatted = query + " " + keyword + " " + value;
        }

        return queryFormatted;
    }

    private String withOffset(String query, String value) {
        return withOffset(query, double2int(value));
    }

    private String withOffset(String query, Integer value) {
        return orderedLimitOffset(query, "OFFSET", value);
    }

    private Integer double2int(String value) {
        return (Double.valueOf(value)).intValue();
    }

    private QueryResponseData invoke(String query, Map<String, Object> queryParameter) {
        Map<String, Set<Object>> auditResultMap = auditResultMapHolder.getAuditResultMap();

        Map<String, Object> parameters = Optional.ofNullable(queryParameter)
                .filter(MapUtils::isNotEmpty)
                .map(HashMap::new)
                .orElse(null);

        QueryResultDto resultDto = queryService.query(query, parameters, false, auditResultMap);

        // Enriches request attributes with EhrId(s) for later audit processing
        AuditMsgBuilder.getInstance().setEhrIds(auditResultMap.get(EHR_ID_VALUE));

        return new QueryResponseData(resultDto);
    }

    String withOffsetLimit(String query, Map<String, Object> mapped) {
        if (mapped.containsKey("fetch")) {
            // append LIMIT clause to aql
            query = withFetch(query, mapped.get("fetch").toString());
        }

        if (mapped.containsKey("offset")) {
            // append OFFSET clause to aql
            query = withOffset(query, mapped.get("offset").toString());
        }

        return query;
    }
}
