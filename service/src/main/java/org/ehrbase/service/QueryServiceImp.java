/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.service;

import static java.lang.String.format;

import com.google.gson.JsonElement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.ehrbase.api.definitions.QueryMode;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.BadGatewayException;
import org.ehrbase.api.exception.GeneralRequestProcessingException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.service.QueryService;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.sql.AqlResult;
import org.ehrbase.dao.access.interfaces.I_EntryAccess;
import org.ehrbase.dao.access.interfaces.I_StoredQueryAccess;
import org.ehrbase.dao.access.jooq.AqlQueryHandler;
import org.ehrbase.dao.access.jooq.StoredQueryAccess;
import org.ehrbase.response.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.response.ehrscape.QueryResultDto;
import org.ehrbase.response.ehrscape.StructuredString;
import org.ehrbase.response.ehrscape.StructuredStringFormat;
import org.ehrbase.response.ehrscape.query.ResultHolder;
import org.ehrbase.validation.terminology.ExternalTerminologyValidation;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@SuppressWarnings("unchecked")
public class QueryServiceImp extends BaseServiceImp implements QueryService {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private final ExternalTerminologyValidation tsAdapter;
    private final TenantService tenantService;

    @Autowired
    public QueryServiceImp(
            KnowledgeCacheService knowledgeCacheService,
            DSLContext context,
            ServerConfig serverConfig,
            ExternalTerminologyValidation tsAdapter,
            TenantService tenantService) {

        super(knowledgeCacheService, context, serverConfig);
        this.tsAdapter = tsAdapter;
        this.tenantService = tenantService;
    }

    private static BiConsumer<Map<?, ?>, String> checkNonNull = (map, errMsg) -> {
        if (map == null) throw new IllegalArgumentException(errMsg);
    };

    @Override
    public QueryResultDto query(
            String queryString,
            Map<String, Object> parameters,
            QueryMode queryMode,
            boolean explain,
            Map<String, Set<Object>> auditResultMap) {

        switch (queryMode) {
            case SQL:
                return querySql(queryString);

            case AQL:
                return queryAql(
                        queryString,
                        explain,
                        () -> parameters == null
                                ? new AqlQueryHandler(getDataAccess(), tsAdapter).process(queryString)
                                : new AqlQueryHandler(getDataAccess(), tsAdapter).process(queryString, parameters),
                        auditResultMap);

            default:
                throw new IllegalArgumentException("Invalid query mode:" + queryMode);
        }
    }

    private QueryResultDto formatResult(AqlResult aqlResult, String queryString, boolean explain) {
        QueryResultDto dto = new QueryResultDto();
        dto.setExecutedAQL(queryString);
        dto.setVariables(aqlResult.getVariables());

        List<ResultHolder> resultList = new ArrayList<>();
        for (Record record : aqlResult.getRecords()) {
            ResultHolder fieldMap = new ResultHolder();
            for (Field field : record.fields()) {
                // process non-hidden variables
                if (aqlResult.variablesContains(field.getName())) {
                    // check whether to use field name or alias
                    if (record.getValue(field) instanceof JsonElement) {
                        fieldMap.putResult(
                                field.getName(),
                                new StructuredString((record.getValue(field)).toString(), StructuredStringFormat.JSON));
                    } else fieldMap.putResult(field.getName(), record.getValue(field));
                }
            }

            resultList.add(fieldMap);
        }

        dto.setResultSet(resultList);
        if (explain) {
            dto.setExplain(aqlResult.getExplain());
        }

        return dto;
    }

    private static final String ERR_MAP_NON_NULL = "Arg[%s] must not be null";

    private QueryResultDto queryAql(
            String queryString,
            boolean explain,
            Supplier<AqlResult> resultSupplier,
            Map<String, Set<Object>> auditResultMap) {
        checkNonNull.accept(auditResultMap, format(ERR_MAP_NON_NULL, "auditResultMap"));
        try {
            AqlResult aqlResult = resultSupplier.get();
            auditResultMap.putAll(aqlResult.getAuditResultMap());
            return formatResult(aqlResult, queryString, explain);
        } catch (RestClientException rce) {
            throw new BadGatewayException(
                    "Bad gateway exception: " + rce.getCause().getMessage());
        } catch (DataAccessException dae) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error: " + dae.getCause().getMessage());
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(iae.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not process query/stored-query, reason: " + e);
        }
    }

    private QueryResultDto querySql(String queryString) {
        Map<String, Object> result;
        try {
            result = I_EntryAccess.queryJSON(getDataAccess(), queryString);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new InternalServerException(e);
        }

        QueryResultDto dto = new QueryResultDto();
        dto.setExecutedAQL((String) result.get("executedAQL"));
        dto.setResultSet((List<ResultHolder>) result.get("resultSet"));
        dto.setExplain((List<List<String>>) result.get("explain"));
        return dto;
    }

    // === DEFINITION: manage stored queries
    @Override
    public List<QueryDefinitionResultDto> retrieveStoredQueries(String fullyQualifiedName) {

        List<QueryDefinitionResultDto> resultDtos = new ArrayList<>();
        try {
            if (fullyQualifiedName == null || fullyQualifiedName.isEmpty()) {
                for (I_StoredQueryAccess storedQueryAccess : StoredQueryAccess.retrieveQualifiedList(getDataAccess())) {
                    resultDtos.add(mapToQueryDefinitionDto(storedQueryAccess));
                }
            } else {
                for (I_StoredQueryAccess storedQueryAccess :
                        StoredQueryAccess.retrieveQualifiedList(getDataAccess(), fullyQualifiedName)) {
                    resultDtos.add(mapToQueryDefinitionDto(storedQueryAccess));
                }
            }
        } catch (DataAccessException dae) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error:" + dae.getCause().getMessage());
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(iae.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not retrieve stored query, reason:" + e);
        }

        return resultDtos;
    }

    @Override
    public QueryDefinitionResultDto retrieveStoredQuery(String qualifiedName, String version) {
        String queryQualifiedName = qualifiedName + ((version != null && !version.isEmpty()) ? "/" + version : "");

        I_StoredQueryAccess storedQueryAccess;
        try {
            storedQueryAccess = StoredQueryAccess.retrieveQualified(getDataAccess(), queryQualifiedName);
        } catch (DataAccessException dae) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error:" + dae.getCause().getMessage());
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(iae.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage());
        }

        return mapToQueryDefinitionDto(storedQueryAccess);
    }

    @Override
    public QueryDefinitionResultDto createStoredQuery(String qualifiedName, String version, String queryString) {

        // validate the query syntax
        try {
            new AqlExpression().parse(queryString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid query, reason:" + e);
        }

        String tenantIdentifier = tenantService.getCurrentTenantIdentifier();

        try {
            String queryQualifiedName = qualifiedName + ((version != null && !version.isEmpty()) ? "/" + version : "");
            I_StoredQueryAccess storedQueryAccess =
                    new StoredQueryAccess(getDataAccess(), queryQualifiedName, queryString, tenantIdentifier);
            storedQueryAccess.commit();
            return mapToQueryDefinitionDto(storedQueryAccess);
        } catch (DataAccessException dae) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error:" + dae.getCause().getMessage());
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(iae.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage());
        }
    }

    @Override
    public QueryDefinitionResultDto updateStoredQuery(String qualifiedName, String version, String queryString) {

        try {
            I_StoredQueryAccess storedQueryAccess = StoredQueryAccess.retrieveQualified(
                    getDataAccess(), qualifiedName + ((version != null && !version.isEmpty()) ? "/" + version : ""));

            storedQueryAccess.setQueryText(queryString);

            storedQueryAccess.update(Timestamp.from(Instant.now()));
            return mapToQueryDefinitionDto(storedQueryAccess);
        } catch (DataAccessException dae) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error:" + dae.getCause().getMessage());
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(iae.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage());
        }
    }

    @Override
    public QueryDefinitionResultDto deleteStoredQuery(String qualifiedName, String version) {

        try {
            I_StoredQueryAccess storedQueryAccess = StoredQueryAccess.retrieveQualified(
                    getDataAccess(), qualifiedName + ((version != null && !version.isEmpty()) ? "/" + version : ""));

            storedQueryAccess.delete();
            return mapToQueryDefinitionDto(storedQueryAccess);
        } catch (DataAccessException dae) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error:" + dae.getCause().getMessage());
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(iae.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage());
        }
    }

    private QueryDefinitionResultDto mapToQueryDefinitionDto(I_StoredQueryAccess storedQueryAccess) {
        QueryDefinitionResultDto dto = new QueryDefinitionResultDto();
        dto.setSaved(storedQueryAccess.getCreationDate().toInstant().atZone(ZoneId.systemDefault()));
        dto.setQualifiedName(storedQueryAccess.getReverseDomainName() + "::" + storedQueryAccess.getSemanticId());
        dto.setVersion(storedQueryAccess.getSemver());
        dto.setQueryText(storedQueryAccess.getQueryText());
        dto.setType(storedQueryAccess.getQueryType());
        return dto;
    }
}
