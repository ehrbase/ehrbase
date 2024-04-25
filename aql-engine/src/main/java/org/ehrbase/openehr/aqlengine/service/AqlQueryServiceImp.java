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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.re2j.Pattern;
import java.lang.constant.Constable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.api.dto.AqlQueryResult;
import org.ehrbase.api.exception.AqlFeatureNotImplementedException;
import org.ehrbase.api.exception.BadGatewayException;
import org.ehrbase.api.exception.IllegalAqlException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.AqlQueryService;
import org.ehrbase.openehr.aqlengine.AqlQueryUtils;
import org.ehrbase.openehr.aqlengine.asl.AqlSqlLayer;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.featurecheck.AqlQueryFeatureCheck;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper.SelectType;
import org.ehrbase.openehr.aqlengine.repository.AqlQueryRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class AqlQueryServiceImp implements AqlQueryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AqlQueryRepository aqlQueryRepository;
    private final ExternalTerminologyValidation tsAdapter;
    private final AqlSqlLayer aqlSqlLayer;
    private final AqlQueryFeatureCheck aqlQueryFeatureCheck;

    @Autowired
    public AqlQueryServiceImp(
            AqlQueryRepository aqlQueryRepository,
            ExternalTerminologyValidation tsAdapter,
            AqlSqlLayer aqlSqlLayer,
            AqlQueryFeatureCheck aqlQueryFeatureCheck) {
        this.aqlQueryRepository = aqlQueryRepository;
        this.tsAdapter = tsAdapter;
        this.aqlSqlLayer = aqlSqlLayer;
        this.aqlQueryFeatureCheck = aqlQueryFeatureCheck;
    }

    @Override
    public AqlQueryResult query(AqlQueryRequest aqlQuery) {
        return queryAql(aqlQuery);
    }

    private static void raiseInvalidApiParameterIf(boolean condition, Supplier<String> messageSupplier) {
        if (condition) {
            throw new InvalidApiParameterException(messageSupplier.get());
        }
    }

    private AqlQueryResult queryAql(AqlQueryRequest aqlQueryRequest) {
        // TODO: check that select aliases are not duplicated
        try {
            AqlQuery aqlQuery = buildAqlQuery(aqlQueryRequest);

            aqlQueryFeatureCheck.ensureQuerySupported(aqlQuery);

            try {
                AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

                AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);
                List<SelectWrapper> nonPrimitiveSelects =
                        queryWrapper.nonPrimitiveSelects().toList();

                AqlQueryRequest.ExecutionInstruction executionInstruction = aqlQueryRequest.executionInstruction();
                AqlQueryRepository.QueryResult result = aqlQueryRepository.executeQuery(
                        aslQuery, nonPrimitiveSelects, executionInstruction.returnExecutedSQL());
                List<List<Object>> resultData = result.data();

                if (nonPrimitiveSelects.isEmpty()) {
                    // only primitives selected: only a count() was performed, so the list must be constructed
                    resultData = LongStream.range(
                                    0, (long) resultData.getFirst().getFirst())
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

                if (logger.isTraceEnabled()) {
                    try {
                        logger.trace(new ObjectMapper().writeValueAsString(aqlQuery));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }

                String understoodByAqlParser = AqlRenderer.render(aqlQuery);
                QueryResultDto queryResultDto = formatResult(queryWrapper, resultData, understoodByAqlParser);

                AqlQueryResult.ExecutionInfo executionInfo = AqlQueryResult.ExecutionInfo.Empty;
                if (executionInstruction.isPresent()) {
                    executionInfo = new AqlQueryResult.ExecutionInfo(result.executedSQL(), false);
                }
                return new AqlQueryResult(queryResultDto, executionInfo);

            } catch (IllegalArgumentException e) {
                // regular IllegalArgumentException, not due to illegal query parameters
                throw new InternalServerException(e.getMessage(), e);
            }

        } catch (RestClientException e) {
            throw new BadGatewayException(
                    "Bad gateway: %s"
                            .formatted(Optional.of(e)
                                    .map(Throwable::getCause)
                                    .orElse(e)
                                    .getMessage()),
                    e);
        } catch (DataAccessException e) {
            throw new InternalServerException(
                    "Data Access Error: %s"
                            .formatted(Optional.of(e)
                                    .map(Throwable::getCause)
                                    .orElse(e)
                                    .getMessage()),
                    e);
        } catch (AqlParseException e) {
            throw new IllegalAqlException(
                    "Could not parse AQL query: %s"
                            .formatted(Optional.of(e)
                                    .map(Throwable::getCause)
                                    .orElse(e)
                                    .getMessage()),
                    e);
        }
    }

    private static AqlQuery buildAqlQuery(AqlQueryRequest aqlQueryRequest) {

        AqlQuery aqlQuery = AqlQueryParser.parse(aqlQueryRequest.queryString());

        // apply limit and offset - where the definitions from the aql are the precedence
        Optional.ofNullable(aqlQueryRequest.fetch()).ifPresent(fetch -> {
            raiseInvalidApiParameterIf(
                    aqlQuery.getLimit() != null,
                    () -> "Invalid AQL query: fetch is defined on query %s and as parameter %s"
                            .formatted(aqlQuery.getLimit(), fetch));
            aqlQuery.setLimit(fetch);
        });
        Optional.ofNullable(aqlQueryRequest.offset()).ifPresent(offset -> {
            raiseInvalidApiParameterIf(
                    aqlQuery.getOffset() != null,
                    () -> "Invalid AQL query: fetch is defined on query %s and as parameter %s"
                            .formatted(aqlQuery.getOffset(), offset));
            aqlQuery.setOffset(offset);
        });

        // sanity check - In AQL there is no offset without limit.
        raiseInvalidApiParameterIf(
                aqlQuery.getOffset() != null && aqlQuery.getLimit() == null,
                () -> "Invalid AQL query: provided offset %s without a limit".formatted(aqlQuery.getOffset()));

        // postprocess
        replaceParameters(aqlQuery, aqlQueryRequest.parameters());
        replaceEhrPaths(aqlQuery);

        return aqlQuery;
    }

    private QueryResultDto formatResult(AqlQueryWrapper query, List<List<Object>> resultData, String queryString) {

        List<SelectWrapper> selectFields = query.selects();

        QueryResultDto dto = new QueryResultDto();
        dto.setExecutedAQL(queryString);

        Optional.ofNullable(query.limit()).ifPresent(v -> dto.setLimit(v.intValue()));
        Optional.ofNullable(query.offset()).ifPresent(v -> dto.setOffset(v.intValue()));

        Map<String, String> columns = new LinkedHashMap<>();
        for (int i = 0; i < selectFields.size(); i++) {
            SelectWrapper namePath = selectFields.get(i);
            columns.put(
                    Optional.of(namePath).map(SelectWrapper::getSelectAlias).orElse("#" + i),
                    namePath.getSelectPath().orElse(null));
        }
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
     *
     * @param aqlQuery
     */
    static void replaceEhrPaths(AqlQuery aqlQuery) {
        replaceEhrPath(aqlQuery, "compositions", "COMPOSITION", "c");
        replaceEhrPath(aqlQuery, "ehr_status", "EHR_STATUS", "s");
    }

    /**
     * Rephrases a path from EHR to EHR_STATUS as CONTAINS statement so that it can be handled regularly by the aql engine.
     * E.g. <code>SELECT e/status FROM EHR</code> is rewritten as <code>SELECT s FROM EHR e CONTAINS EHR_STATUS s</code>.
     *
     * @param aqlQuery
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
}
