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
import java.util.stream.Stream;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
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
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectQuery;
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

    public AqlQueryRepository(
            SystemService systemService,
            KnowledgeCacheService knowledgeCache,
            AqlSqlQueryBuilder queryBuilder,
            DSLContext context) {
        this.queryBuilder = queryBuilder;
        this.systemService = systemService;
        this.knowledgeCache = knowledgeCache;
        this.context = context;
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
        SelectQuery<Record> selectQuery = preparedQuery.selectQuery;

        Result<Record> fetch;

        fetch = preparedQuery.selectQuery.fetch();

        try (Stream<Record> stream = fetch.stream()) {
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
}
