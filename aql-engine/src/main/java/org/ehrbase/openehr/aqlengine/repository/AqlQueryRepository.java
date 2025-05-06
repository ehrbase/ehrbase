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
package org.ehrbase.openehr.aqlengine.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper.SelectType;
import org.ehrbase.openehr.aqlengine.sql.AqlSqlQueryBuilder;
import org.ehrbase.openehr.aqlengine.sql.ParsedQuery;
import org.ehrbase.openehr.aqlengine.sql.QueryResultRowMapper;
import org.ehrbase.openehr.aqlengine.sql.postprocessor.AqlSqlResultPostprocessor;
import org.ehrbase.openehr.aqlengine.sql.postprocessor.DefaultResultPostprocessor;
import org.ehrbase.openehr.aqlengine.sql.postprocessor.ExtractedColumnResultPostprocessor;
import org.ehrbase.openehr.sdk.aql.dto.operand.AggregateFunction.AggregateFunctionName;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.query.ResultHolder;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectQuery;
import org.jooq.impl.QOM;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes ASL queries as SQL, and converts the results
 */
@Repository
@Transactional(readOnly = true)
public class AqlQueryRepository {

    private static final AqlSqlResultPostprocessor NOOP_POSTPROCESSOR = v -> v;
    private final SystemService systemService;
    private final KnowledgeCacheService knowledgeCache;
    private final AqlSqlQueryBuilder queryBuilder;
    private final DSLContext context;
    private final JdbcClient jdbcClient;

    public AqlQueryRepository(SystemService systemService, KnowledgeCacheService knowledgeCache,
                              AqlSqlQueryBuilder queryBuilder, DSLContext context, JdbcClient jdbcClient) {
        this.queryBuilder = queryBuilder;
        this.systemService = systemService;
        this.knowledgeCache = knowledgeCache;
        this.context = context;
        this.jdbcClient = jdbcClient;
    }

    /**
     * Prepares the full SQL query. Build the structure from AQL and selects postprocess based on the given
     * <code>selects</code>.
     *
     * @param aslQuery to create the actual SQL query from.
     * @param selects  to obtain {@link AqlSqlResultPostprocessor} for.
     *
     * @see #executeQuery(PreparedQuery)
     * @see #getQuerySql(PreparedQuery)
     * @see #explainQuery(boolean, PreparedQuery)
     */
    public PreparedQuery prepareQuery(AslRootQuery aslQuery, List<SelectWrapper> selects) {

        final SelectQuery<Record> selectQuery = queryBuilder.buildSqlQuery(aslQuery);

        AqlSqlResultPostprocessor[] postProcessors;
        if (selects.isEmpty()) {
            // one column with COUNT: see AqlSqlLayer::addSyntheticSelect
            postProcessors = new AqlSqlResultPostprocessor[] {NOOP_POSTPROCESSOR};
        } else {
            postProcessors = selects.stream().map(this::getPostProcessor).toArray(AqlSqlResultPostprocessor[]::new);
        }
        return new PreparedQuery(selectQuery, postProcessors);
    }

    public List<List<Object>> executeQuery(PreparedQuery preparedQuery) {

        try (Stream<Record> stream = preparedQuery.selectQuery.stream()) {
            return stream.map(r -> postProcessDbRecord(r, preparedQuery.postProcessors))
                    .toList();
        }
    }

    public static String getQuerySql(PreparedQuery preparedQuery) {
        return preparedQuery.selectQuery.getSQL();
    }

    public String explainQuery(boolean analyze, PreparedQuery preparedQuery) {
        return queryBuilder.explain(analyze, preparedQuery.selectQuery).formatJSON();
    }

    private AqlSqlResultPostprocessor getPostProcessor(SelectWrapper select) {
        // datatype must remain numeric for count, sum, avg
        if (select.type() == SelectType.AGGREGATE_FUNCTION
                && EnumSet.of(AggregateFunctionName.COUNT, AggregateFunctionName.SUM, AggregateFunctionName.AVG)
                        .contains(select.getAggregateFunctionName())) {
            return NOOP_POSTPROCESSOR;
        }

        Optional<AqlObjectPath> selectPath = select.getIdentifiedPath().map(IdentifiedPath::getPath);
        List<PathNode> nodes = selectPath.map(AqlObjectPath::getPathNodes).orElseGet(Collections::emptyList);
        // extracted column by full path
        return AslExtractedColumn.find(select.root(), selectPath.orElse(null))
                // OR extracted column by archetype_node_id suffix
                .or(() -> Optional.of(AslExtractedColumn.ARCHETYPE_NODE_ID)
                        .filter(e ->
                                selectPath.filter(p -> p.endsWith(e.getPath())).isPresent()))
                // OR extracted column ORIGINAL_VERSION.commit_audit
                .or(() -> AslExtractedColumn.find(
                                RmConstants.AUDIT_DETAILS,
                                new AqlObjectPath(nodes.stream().skip(1).toList()))
                        .filter(e -> RmConstants.ORIGINAL_VERSION.equals(
                                select.root().getRmType()))
                        .filter(e -> nodes.stream()
                                .limit(1)
                                .map(PathNode::getAttribute)
                                .allMatch("commit_audit"::equals)))
                .<AqlSqlResultPostprocessor>map(
                        ec -> new ExtractedColumnResultPostprocessor(ec, knowledgeCache, systemService.getSystemId()))
                .orElseGet(DefaultResultPostprocessor::new);
    }

    private static List<Object> postProcessDbRecord(Record r, AqlSqlResultPostprocessor[] postProcessors) {
        List<Object> resultRow = new ArrayList<>(r.size());
        for (int i = 0; i < r.size(); i++) {
            resultRow.add(postProcessors[i].postProcessColumn(r.get(i)));
        }
        return resultRow;
    }

    /**
     * Executes the SQL query and returns the result.
     * <p>
     * This method MUST only be used to execute the SQL query that has been stored first.
     * It is not intended to be used for executing arbitrary SQL queries.
     * </p>
     *
     * @param parsedQuery the parsed SQL query
     * @return the result of the query
     */
    @SuppressWarnings("SqlSourceToSinkFlow")
    public QueryResultDto executeSql(ParsedQuery parsedQuery) {
        StatementSpec queryStatement = jdbcClient.sql(parsedQuery.getSql());
        parsedQuery.getParameters().forEach(queryStatement::param);

        List<List<Object>> results = queryStatement
                .query(new QueryResultRowMapper())
                .list();

        Select<?> selectQuery = context.parser().parseSelect(parsedQuery.getSql());
        if (selectQuery == null) {
            throw new IllegalStateException("Failed to parse SQL query");
        }

        return buildResult(selectQuery.getSelect(), results, parsedQuery.getLimit(), parsedQuery.getOffset());
    }

    @SuppressWarnings("UnstableApiUsage")
    private QueryResultDto buildResult(List<Field<?>> selectFields, List<List<Object>> results,
                                       Integer limit, Integer offset) {
        QueryResultDto queryResult = new QueryResultDto();

        String[] columnNames = new String[selectFields.size()];
        Map<String, String> columns = new LinkedHashMap<>();
        for (int i = 0; i < selectFields.size(); i++) {
            Field<?> field = selectFields.get(i);
            columnNames[i] = field.getName();
            if (field instanceof QOM.FieldAlias<?> fieldAlias) {
                columns.put(fieldAlias.getName(), formatQualifiedName(fieldAlias.$field().getQualifiedName()));
            }
        }
        queryResult.setVariables(columns);

        var resultSet = results.stream()
                .map(row -> {
                    ResultHolder fieldMap = new ResultHolder();
                    for (int i = 0; i < row.size(); i++) {
                        fieldMap.putResult(columnNames[i], row.get(i));
                    }
                    return fieldMap;
                })
                .toList();
        queryResult.setResultSet(resultSet);
        queryResult.setLimit(limit);
        queryResult.setOffset(offset);
        return queryResult;
    }

    private String formatQualifiedName(Name qualifiedName) {
        return String.join(".", qualifiedName.getName());
    }
}
