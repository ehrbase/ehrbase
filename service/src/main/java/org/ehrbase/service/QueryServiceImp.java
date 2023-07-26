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
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.BadGatewayException;
import org.ehrbase.api.exception.GeneralRequestProcessingException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.QueryService;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.sql.AqlResult;
import org.ehrbase.dao.access.interfaces.I_StoredQueryAccess;
import org.ehrbase.dao.access.jooq.AqlQueryHandler;
import org.ehrbase.dao.access.jooq.StoredQueryAccess;
import org.ehrbase.dao.access.util.InvalidVersionFormatException;
import org.ehrbase.dao.access.util.SemVer;
import org.ehrbase.dao.access.util.SemVerUtil;
import org.ehrbase.dao.access.util.VersionConflictException;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredStringFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.query.ResultHolder;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());
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
            boolean explain,
            Map<String, Set<Object>> auditResultMap) {

        AqlQueryHandler handler = new AqlQueryHandler(getDataAccess(), tsAdapter);
        return queryAql(
                queryString,
                explain,
                () -> parameters == null ? handler.process(queryString) : handler.process(queryString, parameters),
                auditResultMap);
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
        } catch (RestClientException e) {
            throw new BadGatewayException(
                    "Bad gateway exception: " + e.getCause().getMessage());
        } catch (DataAccessException e) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error: " + e.getCause().getMessage());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Could not process query/stored-query, reason: " + e);
        }
    }

    // === DEFINITION: manage stored queries
    @Override
    public List<QueryDefinitionResultDto> retrieveStoredQueries(String fullyQualifiedName) {
        String name = StringUtils.defaultIfEmpty(fullyQualifiedName, null);
        try {
            List<StoredQueryAccess> storedQueries = StoredQueryAccess.retrieveQualifiedList(getDataAccess(), name);
            return storedQueries.stream()
                    .map(QueryServiceImp::mapToQueryDefinitionDto)
                    .toList();
        } catch (DataAccessException e) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error: " + e.getCause().getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Could not retrieve stored query, reason: " + e, e);
        }
    }

    @Override
    public QueryDefinitionResultDto retrieveStoredQuery(String qualifiedName, String version) {
        SemVer requestedVersion = parseRequestSemVer(version);

        Optional<StoredQueryAccess> storedQueryAccess;
        try {
            storedQueryAccess = StoredQueryAccess.retrieveQualified(getDataAccess(), qualifiedName, requestedVersion);
        } catch (DataAccessException e) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error: " + e.getCause().getMessage(), e);
        } catch (RuntimeException e) {
            throw new InternalServerException(e.getMessage());
        }

        return storedQueryAccess
                .map(QueryServiceImp::mapToQueryDefinitionDto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not retrieve stored query for qualified name: " + qualifiedName));
    }

    @Override
    public QueryDefinitionResultDto createStoredQuery(String qualifiedName, String version, String queryString) {

        SemVer requestedVersion = parseRequestSemVer(version);

        // validate the query syntax
        try {
            new AqlExpression().parse(queryString);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid query, reason:" + e, e);
        }

        // lookup version in db
        SemVer dbSemVer = StoredQueryAccess.retrieveQualified(getDataAccess(), qualifiedName, requestedVersion)
                .map(q -> SemVer.parse(q.getSemver()))
                .orElse(SemVer.NO_VERSION);

        checkVersionCombination(requestedVersion, dbSemVer);

        SemVer newVersion = SemVerUtil.determineVersion(requestedVersion, dbSemVer);

        Short sysTenant = tenantService.getCurrentSysTenant();
        I_StoredQueryAccess storedQueryAccess =
                new StoredQueryAccess(getDataAccess(), qualifiedName, newVersion, queryString, sysTenant);

        // if not final version and already existing: update
        boolean isUpdate = dbSemVer.isPreRelease();

        try {
            if (isUpdate) {
                storedQueryAccess.update(Timestamp.from(Instant.now()));
            } else {
                storedQueryAccess.commit();
            }
        } catch (DataAccessException e) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error: " + e.getCause().getMessage(), e);
        } catch (VersionConflictException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return mapToQueryDefinitionDto(storedQueryAccess);
    }

    private static void checkVersionCombination(SemVer requestSemVer, SemVer dbSemVer) {
        if (dbSemVer.isNoVersion()) {
            // Noop: no issue
        } else if (dbSemVer.isPartial()) {
            throw new IllegalStateException("The database contains stored queries with partial versions");

        } else if (dbSemVer.isPreRelease()) {
            if (!requestSemVer.isPreRelease()) {
                throw new RuntimeException(
                        "Pre-release " + dbSemVer + " was provided when " + requestSemVer + " was requested");
            }
        } else {
            // release
            if (requestSemVer.isPreRelease()) {
                throw new RuntimeException(
                        "Version " + dbSemVer + " was provided when pre-release " + requestSemVer + " was requested");

            } else if (requestSemVer.isRelease()) {
                throw new StateConflictException("Version already exists");
            }
        }
    }

    @Override
    public QueryDefinitionResultDto deleteStoredQuery(String qualifiedName, String version) {

        SemVer requestedVersion = parseRequestSemVer(version);
        if (requestedVersion.isNoVersion() || requestedVersion.isPartial()) {
            throw new InvalidApiParameterException("A qualified version has to be specified");
        }

        try {
            I_StoredQueryAccess storedQueryAccess = StoredQueryAccess.retrieveQualified(
                            getDataAccess(), qualifiedName, requestedVersion)
                    .orElseThrow(() -> new ObjectNotFoundException(
                            "stored query",
                            "Could not retrieve stored query for qualified name: " + qualifiedName + " version:"
                                    + version));

            storedQueryAccess.delete();
            return mapToQueryDefinitionDto(storedQueryAccess);
        } catch (ObjectNotFoundException e) {
            throw e;
        } catch (DataAccessException dae) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error:" + dae.getCause().getMessage());
        } catch (RuntimeException e) {
            throw new InternalServerException(e.getMessage());
        }
    }

    private static SemVer parseRequestSemVer(String version) {
        try {
            return SemVer.parse(version);
        } catch (InvalidVersionFormatException e) {
            throw new InvalidApiParameterException("Incorrect version. Use the SEMVER format.", e);
        }
    }

    private static QueryDefinitionResultDto mapToQueryDefinitionDto(I_StoredQueryAccess storedQueryAccess) {
        QueryDefinitionResultDto dto = new QueryDefinitionResultDto();
        dto.setSaved(storedQueryAccess.getCreationDate().toInstant().atZone(ZoneId.systemDefault()));
        dto.setQualifiedName(storedQueryAccess.getReverseDomainName() + "::" + storedQueryAccess.getSemanticId());
        dto.setVersion(storedQueryAccess.getSemver());
        dto.setQueryText(storedQueryAccess.getQueryText());
        dto.setType(storedQueryAccess.getQueryType());
        return dto;
    }
}
