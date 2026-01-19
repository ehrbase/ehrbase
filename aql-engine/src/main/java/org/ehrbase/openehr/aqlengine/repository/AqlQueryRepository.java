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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.api.service.AqlQueryService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper.SelectType;
import org.ehrbase.openehr.aqlengine.sql.AqlSqlQueryBuilder;
import org.ehrbase.openehr.aqlengine.sql.postprocessor.AqlSqlResultPostprocessor;
import org.ehrbase.openehr.aqlengine.sql.postprocessor.DefaultResultPostprocessor;
import org.ehrbase.openehr.aqlengine.sql.postprocessor.ExtractedColumnResultPostprocessor;
import org.ehrbase.openehr.sdk.aql.dto.operand.AggregateFunction.AggregateFunctionName;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.Select;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes ASL queries as SQL, and converts the results
 */
@Repository
public class AqlQueryRepository {

    private static final AqlSqlResultPostprocessor NOOP_POSTPROCESSOR = v -> v;
    private final SystemService systemService;
    private final KnowledgeCacheService knowledgeCache;
    private final AqlSqlQueryBuilder queryBuilder;
    private final AqlQueryContext queryContext;

    public AqlQueryRepository(
            SystemService systemService,
            KnowledgeCacheService knowledgeCache,
            AqlSqlQueryBuilder queryBuilder,
            AqlQueryContext queryContext) {
        this.systemService = systemService;
        this.knowledgeCache = knowledgeCache;
        this.queryBuilder = queryBuilder;
        this.queryContext = queryContext;
    }

    /**
     * Prepares the full SQL query. Build the structure from AQL and selects postprocess based on the given
     * <code>selects</code>.
     *
     * @param aslQuery to create the actual SQL query from.
     * @param selects  to obtain {@link AqlSqlResultPostprocessor} for.
     *
     * @see #executeQuery(PreparedQuery)
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

        ResultQuery<Record> resultQuery = prependSqlComments(selectQuery, queryContext);

        return new PreparedQuery(resultQuery, postProcessors);
    }

    public static <R extends Record> ResultQuery<R> prependSqlComments(
            Select<R> selectQuery, AqlQueryContext queryContext) {
        return Optional.of(AqlQueryService.SQL_COMMENTS_KEY)
                .map(queryContext::<List<String>>getProperty)
                .filter(CollectionUtils::isNotEmpty)
                .map(l -> l.stream()
                        .map(AqlQueryRepository::escapeSqlComment)
                        .collect(Collectors.joining("*/\n/*", "/*", "*/")))
                .map(comments -> {
                    ResultQuery<R> query = (ResultQuery<R>) DSL.resultQuery("{0}\n{1}", DSL.raw(comments), selectQuery);
                    query.coerce(selectQuery.fields());
                    // reattach the query, in case callers created the query attached
                    query.attach(selectQuery.configuration());
                    return query;
                })
                .orElse(selectQuery);
    }

    private static String escapeSqlComment(String s) {
        return s.replace("*/", "*|").replace("/*", "|*");
    }

    /**
     * Executes the given {@link PreparedQuery} in its own read only transaction.
     *
     * @param preparedQuery to execute
     * @return resultSet
     */
    @Transactional(readOnly = true)
    public List<List<Object>> executeQuery(PreparedQuery preparedQuery) {
        return preparedQuery.selectQuery.fetch(r -> postProcessDbRecord(r, preparedQuery.postProcessors));
    }

    /**
     * Explains the with optional analyse the given {@link PreparedQuery} in its own read only transaction.
     * @param analyze       also run analyse
     * @param preparedQuery to explain and analyse if needed
     * @return result in serialized Json format
     */
    @Transactional(readOnly = true)
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
                .or(() -> selectPath
                        .filter(p -> p.endsWith(AslExtractedColumn.ARCHETYPE_NODE_ID.getPath()))
                        .map(__ -> AslExtractedColumn.ARCHETYPE_NODE_ID))
                // OR extracted column ORIGINAL_VERSION.commit_audit
                .or(() -> AslExtractedColumn.find(
                                RmConstants.AUDIT_DETAILS,
                                new AqlObjectPath(nodes.stream().skip(1).toList()))
                        .filter(e -> RmConstants.ORIGINAL_VERSION.equals(
                                select.root().getRmType()))
                        .filter(e -> "commit_audit".equals(nodes.getFirst().getAttribute())))
                .<AqlSqlResultPostprocessor>map(
                        ec -> ExtractedColumnResultPostprocessor.get(ec, knowledgeCache, systemService.getSystemId()))
                .orElse(DefaultResultPostprocessor.INSTANCE);
    }

    private static List<Object> postProcessDbRecord(Record r, AqlSqlResultPostprocessor[] postProcessors) {
        List<Object> resultRow = new ArrayList<>(r.size());
        for (int i = 0; i < r.size(); i++) {
            resultRow.add(postProcessors[i].postProcessColumn(r.get(i)));
        }
        return resultRow;
    }
}
