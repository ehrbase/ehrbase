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
package org.ehrbase.service.graphql.fetcher;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.ehrbase.service.graphql.GraphQlSchemaRegistryService;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generic jOOQ-backed data fetcher for all template-derived GraphQL query fields.
 * Maps GraphQL query arguments (filter, orderBy, first, after) to jOOQ SELECT on {@code ehr_views}.
 *
 * <p>RLS is automatically enforced via {@code TenantAwareConnectionProvider} which sets
 * {@code SET LOCAL ehrbase.current_tenant} on every connection.
 */
@Component
public class GenericViewDataFetcher implements DataFetcher<Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(GenericViewDataFetcher.class);
    private static final String VIEW_SCHEMA = "ehr_views";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 1000;

    private final DSLContext dsl;
    private final GraphQlSchemaRegistryService schemaRegistry;

    @Value("${ehrbase.graphql.statement-timeout:10s}")
    private String statementTimeout;

    public GenericViewDataFetcher(DSLContext dsl, GraphQlSchemaRegistryService schemaRegistry) {
        this.dsl = dsl;
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public Map<String, Object> get(DataFetchingEnvironment env) {
        String queryFieldName = env.getFieldDefinition().getName();
        String viewName = schemaRegistry.resolveViewName(queryFieldName);

        Map<String, Object> filter = env.getArgument("filter");
        String orderByArg = env.getArgument("orderBy");
        Integer first = env.getArgument("first");
        String after = env.getArgument("after");

        int pageSize = Math.min(first != null ? first : DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        int offset = ConnectionBuilder.decodeCursor(after);

        Condition whereCondition = FilterTranslator.translate(filter, VIEW_SCHEMA, viewName);
        SortField<?> orderBy = parseOrderBy(orderByArg, viewName);

        dsl.execute("SET LOCAL statement_timeout = '" + statementTimeout + "'");

        Result<Record> rows = dsl.select()
                .from(DSL.table(DSL.name(VIEW_SCHEMA, viewName)))
                .where(whereCondition)
                .orderBy(orderBy)
                .limit(pageSize + 1)
                .offset(offset)
                .fetch();

        int totalCount = dsl.fetchCount(
                DSL.selectOne().from(DSL.table(DSL.name(VIEW_SCHEMA, viewName))).where(whereCondition));

        log.debug("GraphQL query {}: {} rows (total {}), offset {}", queryFieldName, rows.size(), totalCount, offset);

        return ConnectionBuilder.build(rows, pageSize, offset, totalCount);
    }

    private SortField<?> parseOrderBy(String orderByArg, String viewName) {
        if (orderByArg == null || orderByArg.isEmpty()) {
            return DSL.field(DSL.name(VIEW_SCHEMA, viewName, "id")).asc();
        }

        boolean desc = orderByArg.endsWith("_DESC");
        String columnName = orderByArg.replace("_ASC", "").replace("_DESC", "").toLowerCase();

        org.jooq.Field<Object> field = DSL.field(DSL.name(VIEW_SCHEMA, viewName, columnName));
        return desc ? field.desc() : field.asc();
    }
}
