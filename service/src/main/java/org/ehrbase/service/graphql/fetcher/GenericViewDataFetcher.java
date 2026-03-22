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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ehrbase.service.graphql.GraphQlSchemaRegistryService;
import org.ehrbase.service.graphql.PgToGraphQlTypeMapper;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.graphql.data.pagination.CursorStrategy;
import org.springframework.stereotype.Component;

/**
 * Generic jOOQ-backed data fetcher for all template-derived GraphQL query fields.
 * Maps GraphQL query arguments (filter, orderBy, first, after) to jOOQ SELECT on {@code ehr_views}.
 *
 * <p>Returns a {@link Window} of Maps, which Spring for GraphQL's auto-configured
 * {@link org.springframework.graphql.data.query.WindowConnectionAdapter} converts to
 * Relay-style Connection objects (edges, pageInfo, cursors) automatically.
 *
 * <p>Uses the auto-configured {@link CursorStrategy} for cursor encoding/decoding,
 * ensuring consistency between cursor values in responses and cursor parsing on input.
 *
 * <p>RLS is automatically enforced via {@code TenantAwareConnectionProvider} which sets
 * {@code SET LOCAL ehrbase.current_tenant} on every connection.
 */
@Component
public class GenericViewDataFetcher implements DataFetcher<Window<Map<String, Object>>> {

    private static final Logger log = LoggerFactory.getLogger(GenericViewDataFetcher.class);
    private static final String VIEW_SCHEMA = "ehr_views";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 1000;

    private final org.jooq.DSLContext dsl;
    private final GraphQlSchemaRegistryService schemaRegistry;
    private final CursorStrategy<ScrollPosition> cursorStrategy;

    @Value("${ehrbase.graphql.statement-timeout:10s}")
    private String statementTimeout;

    @SuppressWarnings("unchecked")
    public GenericViewDataFetcher(
            org.jooq.DSLContext dsl,
            GraphQlSchemaRegistryService schemaRegistry,
            CursorStrategy<?> cursorStrategy) {
        this.dsl = dsl;
        this.schemaRegistry = schemaRegistry;
        this.cursorStrategy = (CursorStrategy<ScrollPosition>) cursorStrategy;
    }

    @Override
    public Window<Map<String, Object>> get(DataFetchingEnvironment env) {
        String queryFieldName = env.getFieldDefinition().getName();
        String viewName = schemaRegistry.resolveViewName(queryFieldName);

        Map<String, Object> filter = env.getArgument("filter");
        String orderByArg = env.getArgument("orderBy");
        Integer first = env.getArgument("first");
        String after = env.getArgument("after");

        int pageSize = Math.min(first != null ? first : DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        int offset = decodeCursor(after);

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

        boolean hasNext = rows.size() > pageSize;
        List<Map<String, Object>> content = (hasNext ? rows.subList(0, pageSize) : rows).stream()
                .map(this::recordToMap)
                .toList();

        log.debug("GraphQL query {}: {} rows, offset {}, hasNext {}", queryFieldName, content.size(), offset, hasNext);

        return Window.from(content, index -> ScrollPosition.offset(offset + index), hasNext);
    }

    private Map<String, Object> recordToMap(Record row) {
        Map<String, Object> node = new LinkedHashMap<>();
        for (org.jooq.Field<?> field : row.fields()) {
            String camelName = PgToGraphQlTypeMapper.toCamelCase(field.getName());
            node.put(camelName, row.get(field));
        }
        return node;
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

    private int decodeCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        ScrollPosition position = cursorStrategy.fromCursor(cursor);
        if (position instanceof OffsetScrollPosition offsetPos) {
            return (int) offsetPos.getOffset() + 1;
        }
        return 0;
    }
}
