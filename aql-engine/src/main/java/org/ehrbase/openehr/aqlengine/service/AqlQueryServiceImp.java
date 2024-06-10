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

import static org.ehrbase.openehr.aqlengine.AqlParameterReplacement.replaceParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.re2j.Pattern;
import java.lang.constant.Constable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.api.exception.AqlFeatureNotImplementedException;
import org.ehrbase.api.exception.BadGatewayException;
import org.ehrbase.api.exception.IllegalAqlException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.service.AqlQueryService;
import org.ehrbase.openehr.aqlengine.AqlQueryUtils;
import org.ehrbase.openehr.aqlengine.asl.AqlSqlLayer;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.featurecheck.AqlQueryFeatureCheck;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper.SelectType;
import org.ehrbase.openehr.aqlengine.repository.AqlQueryRepository;
import org.ehrbase.openehr.aqlengine.repository.PreparedQuery;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.AbstractContainmentExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentSetOperator;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentSetOperatorSymbol;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;
import org.ehrbase.openehr.sdk.aql.parser.AqlParseException;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;
import org.ehrbase.openehr.sdk.aql.util.AqlUtil;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.query.ResultHolder;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class AqlQueryServiceImp implements AqlQueryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AqlQueryRepository aqlQueryRepository;
    private final ExternalTerminologyValidation tsAdapter;
    private final AqlSqlLayer aqlSqlLayer;
    private final AqlQueryFeatureCheck aqlQueryFeatureCheck;
    private final ObjectMapper objectMapper;
    private final AqlQueryContext aqlQueryContext;

    @Value("${ehrbase.rest.aql.default-limit:}")
    private Long defaultLimit;

    @Value("${ehrbase.rest.aql.max-limit:}")
    private Long maxLimit;

    @Value("${ehrbase.rest.aql.max-fetch:}")
    private Long maxFetch;

    enum FetchPrecedence {
        /**
         * Fail if both fetch and limit are present
         */
        REJECT,
        /**
         * Take minimum of fetch and limit for limit;
         * fail if query has offset
         */
        MIN_FETCH;
    }

    @Value("${ehrbase.rest.aql.fetch-precedence:REJECT}")
    private FetchPrecedence fetchPrecedence = FetchPrecedence.REJECT;

    private static Long applyFetchPrecedence(FetchPrecedence fetchPrecedence, Long queryLimit, Long queryOffset, Long fetchParam, Long offsetParam) {
        if (fetchParam == null) {
            if (offsetParam != null) {
                throw new UnprocessableEntityException(
                        "Query parameter for offset provided, but no fetch parameter");
            }
            return queryLimit;
        } else if (queryLimit == null) {
            assert queryOffset == null;
            return fetchParam;
        }

        return switch (fetchPrecedence) {
            case REJECT -> {
                    throw new UnprocessableEntityException(
                            "Query contains a LIMIT clause, fetch and offset parameters must not be used (with fetch precedence %s)".formatted(fetchPrecedence));
            }
            case MIN_FETCH -> {
                if (queryOffset != null) {
                    throw new UnprocessableEntityException(
                            "Query contains a OFFSET clause, fetch parameter must not be used (with fetch precedence %s)".formatted(fetchPrecedence));
                }
                yield Math.min(queryLimit, fetchParam);
            }
        };
    }

    @Autowired
    public AqlQueryServiceImp(
            AqlQueryRepository aqlQueryRepository,
            ExternalTerminologyValidation tsAdapter,
            AqlSqlLayer aqlSqlLayer,
            AqlQueryFeatureCheck aqlQueryFeatureCheck,
            ObjectMapper objectMapper,
            AqlQueryContext aqlQueryContext) {
        this.aqlQueryRepository = aqlQueryRepository;
        this.tsAdapter = tsAdapter;
        this.aqlSqlLayer = aqlSqlLayer;
        this.aqlQueryFeatureCheck = aqlQueryFeatureCheck;
        this.objectMapper = objectMapper;
        this.aqlQueryContext = aqlQueryContext;
    }

    @Override
    public QueryResultDto query(AqlQueryRequest aqlQuery) {
        return queryAql(aqlQuery);
    }

    private QueryResultDto queryAql(AqlQueryRequest aqlQueryRequest) {

        if (defaultLimit != null) {
            aqlQueryContext.setMetaProperty(
                    AqlQueryContext.EhrbaseMetaProperty.DEFAULT_LIMIT, defaultLimit);
        }
        if (maxLimit != null) {
            aqlQueryContext.setMetaProperty(
                    AqlQueryContext.EhrbaseMetaProperty.MAX_LIMIT, maxLimit);
        }
        if (maxFetch != null) {
            aqlQueryContext.setMetaProperty(
                    AqlQueryContext.EhrbaseMetaProperty.MAX_FETCH, maxFetch);
        }

        // TODO: check that select aliases are not duplicated
        try {
            AqlQuery aqlQuery = buildAqlQuery(aqlQueryRequest, fetchPrecedence, defaultLimit, maxLimit, maxFetch);

            aqlQueryFeatureCheck.ensureQuerySupported(aqlQuery);

            try {
                if (logger.isTraceEnabled()) {
                    logger.trace(objectMapper.writeValueAsString(aqlQuery));
                }

                AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

                AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);
                List<SelectWrapper> nonPrimitiveSelects =
                        queryWrapper.nonPrimitiveSelects().toList();

                PreparedQuery preparedQuery = aqlQueryRepository.prepareQuery(aslQuery, nonPrimitiveSelects);

                // aql debug options
                if (aqlQueryContext.showExecutedSql()) {
                    aqlQueryContext.setMetaProperty(
                            AqlQueryContext.EhrbaseMetaProperty.EXECUTED_SQL,
                            AqlQueryRepository.getQuerySql(preparedQuery));
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
        } catch (AqlParseException e) {
            throw new IllegalAqlException(errorMessage("Could not parse AQL query", e), e);
        }
    }

    static AqlQuery buildAqlQuery(
            AqlQueryRequest aqlQueryRequest,
            FetchPrecedence fetchPrecedence,
            Long defaultLimit,
            Long maxLimit,
            Long maxFetch) {

        AqlQuery aqlQuery = AqlQueryParser.parse(aqlQueryRequest.queryString());

        // apply limit and offset - where the definitions from the aql are the precedence
        Optional<AqlQueryRequest> qr = Optional.of(aqlQueryRequest);
        Long fetchParam = aqlQueryRequest.fetch();
        Long offsetParam = aqlQueryRequest.offset();

        Long queryLimit = aqlQuery.getLimit();
        Long queryOffset = aqlQuery.getOffset();

        if (queryLimit != null && maxLimit != null && queryLimit > maxLimit) {
            throw  new UnprocessableEntityException(
                    "Query LIMIT %d exceeds maximum limit %d".formatted(queryLimit, maxLimit));
        }

        if (fetchParam != null && maxFetch != null && fetchParam > maxFetch) {
            throw  new UnprocessableEntityException(
                    "Fetch parameter %d exceeds maximum fetch %d".formatted(fetchParam, maxFetch));
        }

        Long limit = applyFetchPrecedence(fetchPrecedence, queryLimit, queryOffset, fetchParam, offsetParam);

        aqlQuery.setLimit(ObjectUtils.firstNonNull(limit, defaultLimit));
        aqlQuery.setOffset(ObjectUtils.firstNonNull(offsetParam, queryOffset));

        // postprocess
        replaceParameters(aqlQuery, aqlQueryRequest.parameters());
        replaceEhrPaths(aqlQuery);

        return aqlQuery;
    }

    private List<List<Object>> executeQuery(
            PreparedQuery preparedQuery, AqlQueryWrapper queryWrapper, List<SelectWrapper> nonPrimitiveSelects) {

        List<List<Object>> resultData = aqlQueryRepository.executeQuery(preparedQuery);

        if (nonPrimitiveSelects.isEmpty()) {
            // only primitives selected: only a count() was performed, so the list must be constructed
            resultData = LongStream.range(0, (long) resultData.getFirst().getFirst())
                    .<List<Object>>mapToObj(i -> new ArrayList<>())
                    .toList();
        }

        List<SelectWrapper> selects = queryWrapper.selects();
        // Since we do not add primitive value selects to the SQL query, we add them after the query was
        // executed
        for (int i = 0; i < selects.size(); i++) {
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

    private QueryResultDto formatResult(List<SelectWrapper> selectFields, List<List<Object>> resultData) {

        Map<String, String> columns = new LinkedHashMap<>();
        for (int i = 0; i < selectFields.size(); i++) {
            SelectWrapper namePath = selectFields.get(i);
            columns.put(
                    Optional.of(namePath).map(SelectWrapper::getSelectAlias).orElse("#" + i),
                    namePath.getSelectPath().orElse(null));
        }

        QueryResultDto dto = new QueryResultDto();
        dto.setVariables(columns);

        List<ResultHolder> resultList = resultData.stream()
                .map(r -> {
                    ResultHolder fieldMap = new ResultHolder();
                    for (int i = 0; i < r.size(); i++) {
                        Object c = r.get(i);
                        fieldMap.putResult(
                                Optional.ofNullable(selectFields.get(i).getSelectAlias())
                                        .orElse("#" + i),
                                c);
                    }

                    return fieldMap;
                })
                .toList();

        dto.setResultSet(resultList);
        return dto;
    }

    /**
     * Rephrases EHR.composition and EHR.status CONTAINS statements so that they can be handled regularly by the aql engine.
     * E.g. <code>SELECT e/ehr_status FROM EHR</code> is rewritten as <code>SELECT s FROM EHR e CONTAINS EHR_STATUS s</code>,
     * <code>SELECT e/composition FROM EHR</code> is rewritten as <code>SELECT c FROM EHR e CONTAINS COMPOSITION c</code>.
     */
    static void replaceEhrPaths(AqlQuery aqlQuery) {
        replaceEhrPath(aqlQuery, "compositions", "COMPOSITION", "c");
        replaceEhrPath(aqlQuery, "ehr_status", "EHR_STATUS", "s");
    }

    /**
     * Rephrases a path from EHR to EHR_STATUS as CONTAINS statement so that it can be handled regularly by the aql engine.
     * E.g. <code>SELECT e/status FROM EHR</code> is rewritten as <code>SELECT s FROM EHR e CONTAINS EHR_STATUS s</code>.
     */
    static void replaceEhrPath(AqlQuery aqlQuery, String ehrPath, String type, String aliasPrefix) {

        // gather paths that contain EHR/status.
        List<IdentifiedPath> ehrPaths = AqlQueryUtils.allIdentifiedPaths(aqlQuery)
                // EHR
                .filter(ip -> ip.getRoot() instanceof ContainmentClassExpression cce
                        && cce.getType().equals(RmConstants.EHR))
                // EHR.ehrPath...
                .filter(ip -> Optional.of(ip)
                        .map(IdentifiedPath::getPath)
                        .map(AqlObjectPath::getPathNodes)
                        .map(List::getFirst)
                        .map(PathNode::getAttribute)
                        .filter(ehrPath::equals)
                        .isPresent())
                .toList();

        if (ehrPaths.isEmpty()) {
            return;
        }

        if (ehrPaths.stream()
                        .map(IdentifiedPath::getRoot)
                        .map(AbstractContainmentExpression::getIdentifier)
                        .distinct()
                        .count()
                > 1) {
            throw new AqlFeatureNotImplementedException("Multiple EHR in FROM are not supported");
        }

        if (ehrPaths.stream().map(IdentifiedPath::getRootPredicate).anyMatch(CollectionUtils::isNotEmpty)) {
            throw new AqlFeatureNotImplementedException(
                    "Root predicates for EHR/%s are not supported".formatted(ehrPath));
        }

        if (ehrPaths.stream()
                        .map(IdentifiedPath::getPath)
                        .map(p -> p.getPathNodes().getFirst().getPredicateOrOperands())
                        .distinct()
                        .count()
                > 1) {
            // could result in multiple containments
            throw new AqlFeatureNotImplementedException(
                    "Specifying different predicates for EHR/%s is not supported".formatted(ehrPath));
        }
        // determine unused alias
        String alias = AqlUtil.streamContainments(aqlQuery.getFrom())
                .map(AbstractContainmentExpression::getIdentifier)
                .filter(Objects::nonNull)
                .filter(s -> s.matches(Pattern.quote(aliasPrefix) + "\\d*"))
                .map(s -> aliasPrefix.equals(s) ? 0 : Long.parseLong(s.substring(1)))
                .max(Comparator.naturalOrder())
                .map(i -> aliasPrefix + (i + 1))
                .orElse(aliasPrefix);

        // insert CONTAINS [type] (AND if needed)
        // what about "SELECT e[ehr_id=…]/status from EHR e"?
        ContainmentClassExpression ehrContainment =
                (ContainmentClassExpression) ehrPaths.getFirst().getRoot();

        ContainmentClassExpression ehrStatusContainment = new ContainmentClassExpression();
        ehrStatusContainment.setType(type);
        ehrStatusContainment.setIdentifier(alias);

        // copy first predicate (all all are the same)
        ehrPaths.stream()
                .findFirst()
                .map(IdentifiedPath::getPath)
                .map(p -> p.getPathNodes().getFirst().getPredicateOrOperands())
                .ifPresent(ehrStatusContainment::setPredicates);

        // add containment
        if (ehrContainment.getContains() == null) {
            ehrContainment.setContains(ehrStatusContainment);
        } else if (ehrContainment.getContains() instanceof ContainmentSetOperator cse
                && cse.getSymbol() == ContainmentSetOperatorSymbol.AND) {
            cse.setValues(Stream.concat(Stream.of(ehrStatusContainment), cse.getValues().stream())
                    .toList());
        } else {
            ContainmentSetOperator and = new ContainmentSetOperator();
            and.setSymbol(ContainmentSetOperatorSymbol.AND);
            and.setValues(List.of(ehrStatusContainment, ehrContainment.getContains()));
            ehrContainment.setContains(and);
        }

        // rewrite paths
        ehrPaths.forEach(ip -> {
            ip.setRoot(ehrStatusContainment);
            List<PathNode> pathNodes = ip.getPath().getPathNodes();
            ip.setPath(pathNodes.size() == 1 ? null : new AqlObjectPath(pathNodes.subList(1, pathNodes.size())));
        });
    }

    private static String errorMessage(String prefix, Exception e) {
        return prefix + ": " + Optional.of(e).map(Throwable::getCause).orElse(e).getMessage();
    }
}
