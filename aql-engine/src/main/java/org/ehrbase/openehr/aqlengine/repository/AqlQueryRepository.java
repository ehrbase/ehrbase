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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes ASL queries as SQL, and converts the results
 */
@Repository
@Transactional(readOnly = true)
public class AqlQueryRepository {

    /**
     * Wrapper object for AQL query result
     *
     * @param data        result data
     * @param executedSQL in case {@link #executeQuery(AslRootQuery, List, boolean)} is invoked with
     *                    <code>returnExecutedSQL</code> enabled
     */
    public record QueryResult(@Nonnull List<List<Object>> data, @Nullable String executedSQL) {}

    private static final AqlSqlResultPostprocessor NOOP_POSTPROCESSOR = v -> v;
    private final SystemService systemService;
    private final KnowledgeCacheService knowledgeCache;
    private final AqlSqlQueryBuilder queryBuilder;

    public AqlQueryRepository(
            SystemService systemService, KnowledgeCacheService knowledgeCache, AqlSqlQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
        this.systemService = systemService;
        this.knowledgeCache = knowledgeCache;
    }

    public QueryResult executeQuery(AslRootQuery aslQuery, List<SelectWrapper> selects, boolean returnExecutedSQL) {
        SelectQuery<Record> queryResults = queryBuilder.buildSqlQuery(aslQuery);

        final Map<Integer, AqlSqlResultPostprocessor> postProcessors;
        if (selects.isEmpty()) {
            // one column with COUNT: see AqlSqlLayer::addSyntheticSelect
            postProcessors = Map.of(0, NOOP_POSTPROCESSOR);
        } else {
            postProcessors = IntStream.range(0, selects.size())
                    .boxed()
                    .collect(Collectors.toMap(i -> i, i -> getPostProcessor(selects.get(i))));
        }
        List<List<Object>> result = queryResults.stream()
                .map(r -> postProcessDbRecord(r, postProcessors))
                .toList();

        String executedSQL = null;
        if (returnExecutedSQL) {
            executedSQL = queryResults.getSQL();
        }
        return new QueryResult(result, executedSQL);
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

    private static List<Object> postProcessDbRecord(Record r, Map<Integer, AqlSqlResultPostprocessor> postProcessors) {
        List<Object> resultRow = new ArrayList<>(r.size());
        for (int i = 0; i < r.size(); i++) {
            resultRow.add(postProcessors.get(i).postProcessColumn(r.get(i)));
        }
        return resultRow;
    }
}
