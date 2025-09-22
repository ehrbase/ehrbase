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
package org.ehrbase.openehr.aqlengine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.constant.Constable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.LongStream;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.api.exception.BadGatewayException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.service.AqlQueryService;
import org.ehrbase.openehr.aqlengine.aql.AqlQueryParsingPostProcessor;
import org.ehrbase.openehr.aqlengine.asl.AqlSqlLayer;
import org.ehrbase.openehr.aqlengine.asl.AslPostProcessor;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper.SelectType;
import org.ehrbase.openehr.aqlengine.repository.AqlQueryRepository;
import org.ehrbase.openehr.aqlengine.repository.PreparedQuery;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.query.ResultHolder;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service("aqlQueryService")
public class AqlQueryServiceImp implements AqlQueryService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final AqlQueryRepository aqlQueryRepository;
    protected final ExternalTerminologyValidation tsAdapter;
    protected final AqlSqlLayer aqlSqlLayer;
    protected final ObjectMapper objectMapper;
    protected final AqlQueryContext aqlQueryContext;
    protected final List<AqlQueryParsingPostProcessor> aqlPostProcessors;
    protected final List<AslPostProcessor> aslPostProcessors;

    @Autowired
    public AqlQueryServiceImp(
            AqlQueryRepository aqlQueryRepository,
            ExternalTerminologyValidation tsAdapter,
            AqlSqlLayer aqlSqlLayer,
            ObjectMapper objectMapper,
            AqlQueryContext aqlQueryContext,
            List<AqlQueryParsingPostProcessor> aqlPostProcessors,
            List<AslPostProcessor> aslPostProcessors) {
        this.aqlQueryRepository = aqlQueryRepository;
        this.tsAdapter = tsAdapter;
        this.aqlSqlLayer = aqlSqlLayer;
        this.objectMapper = objectMapper;
        this.aqlQueryContext = aqlQueryContext;
        this.aqlPostProcessors = aqlPostProcessors;
        this.aslPostProcessors = aslPostProcessors;
    }

    @Override
    public QueryResultDto query(AqlQueryRequest aqlQueryRequest) {

        // TODO: check that select aliases are not duplicated
        try {
            AqlQuery aqlQuery = aqlQueryRequest.aqlQuery();

            // apply AQL postprocessors
            aqlPostProcessors.forEach(p -> p.afterParseAql(aqlQuery, aqlQueryRequest, aqlQueryContext));

            try {
                if (logger.isTraceEnabled()) {
                    logger.trace(objectMapper.writeValueAsString(aqlQuery));
                }

                AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery, aqlQueryContext.isPathSkipping());

                AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);
                aslPostProcessors.forEach(p -> p.afterBuildAsl(aslQuery, aqlQuery, queryWrapper, aqlQueryRequest));
                List<SelectWrapper> nonPrimitiveSelects =
                        queryWrapper.nonPrimitiveSelects().toList();

                if (aqlQueryContext.showExecutedAql()) {
                    aqlQueryContext.setExecutedAql(AqlRenderer.render(aqlQuery));
                }

                Optional.of(queryWrapper)
                        .map(AqlQueryWrapper::limit)
                        .map(Long::intValue)
                        .ifPresent(limit -> {
                            aqlQueryContext.setMetaProperty(AqlQueryContext.EhrbaseMetaProperty.FETCH, limit);
                            // in case only a limit was used we define the default offset as 0
                            aqlQueryContext.setMetaProperty(
                                    AqlQueryContext.EhrbaseMetaProperty.OFFSET,
                                    Optional.of(queryWrapper)
                                            .map(AqlQueryWrapper::offset)
                                            .map(Long::intValue)
                                            .orElse(0));
                        });

                PreparedQuery preparedQuery = aqlQueryRepository.prepareQuery(aslQuery, nonPrimitiveSelects);

                // aql debug options
                if (aqlQueryContext.showExecutedSql()) {
                    aqlQueryContext.setMetaProperty(
                            AqlQueryContext.EhrbaseMetaProperty.EXECUTED_SQL, preparedQuery.getQuerySql());
                }
                if (aqlQueryContext.showQueryPlan()) {
                    // for dry-run omit analyze
                    boolean analyze = !aqlQueryContext.isDryRun();
                    String explainedQuery = aqlQueryRepository.explainQuery(analyze, preparedQuery);
                    TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};
                    aqlQueryContext.setMetaProperty(
                            AqlQueryContext.EhrbaseMetaProperty.QUERY_PLAN,
                            objectMapper.readValue(explainedQuery, typeRef));
                }

                List<List<Object>> resultData;
                if (aqlQueryContext.isDryRun()) {
                    resultData = List.of();
                } else {
                    resultData = executeQuery(preparedQuery, queryWrapper, nonPrimitiveSelects);
                    aqlQueryContext.setMetaProperty(AqlQueryContext.EhrbaseMetaProperty.RESULT_SIZE, resultData.size());
                }
                return formatResult(queryWrapper.selects(), resultData);

            } catch (IllegalArgumentException | JsonProcessingException e) {
                // regular IllegalArgumentException, not due to illegal query parameters
                throw new InternalServerException(e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new BadGatewayException(errorMessage("Bad gateway", e), e);
        } catch (DataAccessException e) {
            throw new InternalServerException(errorMessage("Data Access Error", e), e);
        }
    }

    private List<List<Object>> executeQuery(
            PreparedQuery preparedQuery, AqlQueryWrapper queryWrapper, List<SelectWrapper> nonPrimitiveSelects) {

        List<List<Object>> resultData = aqlQueryRepository.executeQuery(preparedQuery);
        List<SelectWrapper> selects = queryWrapper.selects();

        if (nonPrimitiveSelects.isEmpty()) {
            // only primitives selected: only a count() was performed, so the list must be constructed
            resultData = LongStream.range(0, (long) resultData.getFirst().getFirst())
                    .<List<Object>>mapToObj(i -> new ArrayList<>(selects.size()))
                    .toList();
        }

        // Since we do not add primitive value selects to the SQL query, we add them after the query was
        // executed
        for (int i = 0, s = selects.size(); i < s; i++) {
            SelectWrapper sd = selects.get(i);
            if (sd.type() == SelectType.PRIMITIVE) {
                Constable value = sd.getPrimitive().getValue();
                for (List<Object> row : resultData) {
                    row.add(i, value);
                }
            }
        }
        return resultData;
    }

    protected QueryResultDto formatResult(List<SelectWrapper> selectFields, List<List<Object>> resultData) {

        String[] columnNames = new String[selectFields.size()];
        Map<String, String> columns = new LinkedHashMap<>();
        for (int i = 0, s = selectFields.size(); i < s; i++) {
            SelectWrapper namePath = selectFields.get(i);
            columnNames[i] =
                    Optional.of(namePath).map(SelectWrapper::getSelectAlias).orElse("#" + i);
            columns.put(columnNames[i], namePath.getSelectPath().orElse(null));
        }

        QueryResultDto dto = new QueryResultDto();
        dto.setVariables(columns);

        dto.setResultSet(resultData.stream()
                .map(r -> {
                    ResultHolder fieldMap = new ResultHolder();
                    for (int i = 0, s = r.size(); i < s; i++) {
                        fieldMap.putResult(columnNames[i], r.get(i));
                    }
                    return fieldMap;
                })
                .toList());
        return dto;
    }

    protected static String errorMessage(String prefix, Exception e) {
        return prefix + ": " + Optional.of(e).map(Throwable::getCause).orElse(e).getMessage();
    }
}
