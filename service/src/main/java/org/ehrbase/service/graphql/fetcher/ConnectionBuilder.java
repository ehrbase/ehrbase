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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ehrbase.service.graphql.PgToGraphQlTypeMapper;
import org.jooq.Record;
import org.jooq.Result;

/**
 * Builds Relay-style connection objects from jOOQ query results.
 * Handles cursor encoding/decoding and pageInfo computation.
 */
public final class ConnectionBuilder {

    private ConnectionBuilder() {}

    /**
     * Builds a connection map from jOOQ results.
     *
     * @param rows      query results (fetched as first+1 to detect hasNextPage)
     * @param first     requested page size
     * @param offset    decoded cursor offset
     * @param totalCount total rows matching the filter
     * @return map with "edges", "pageInfo", "totalCount" keys
     */
    public static Map<String, Object> build(Result<?> rows, int first, int offset, int totalCount) {
        boolean hasNextPage = rows.size() > first;
        List<? extends Record> pageRows = hasNextPage ? rows.subList(0, first) : rows;

        List<Map<String, Object>> edges = pageRows.stream()
                .map(row -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    for (org.jooq.Field<?> field : row.fields()) {
                        String camelName = PgToGraphQlTypeMapper.toCamelCase(field.getName());
                        node.put(camelName, row.get(field));
                    }

                    int rowIndex = offset + pageRows.indexOf(row);
                    Map<String, Object> edge = new LinkedHashMap<>();
                    edge.put("node", node);
                    edge.put("cursor", encodeCursor(rowIndex));
                    return edge;
                })
                .toList();

        Map<String, Object> pageInfo = new LinkedHashMap<>();
        pageInfo.put("hasNextPage", hasNextPage);
        pageInfo.put("hasPreviousPage", offset > 0);
        pageInfo.put("startCursor", edges.isEmpty() ? null : edges.getFirst().get("cursor"));
        pageInfo.put("endCursor", edges.isEmpty() ? null : edges.getLast().get("cursor"));

        Map<String, Object> connection = new LinkedHashMap<>();
        connection.put("edges", edges);
        connection.put("pageInfo", pageInfo);
        connection.put("totalCount", totalCount);
        return connection;
    }

    /**
     * Decodes a cursor string to an integer offset.
     *
     * @param cursor Base64-encoded cursor, or null for offset 0
     * @return decoded offset
     */
    public static int decodeCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
        return Integer.parseInt(decoded.replace("cursor:", "")) + 1;
    }

    /**
     * Encodes an integer offset to a cursor string.
     */
    public static String encodeCursor(int offset) {
        return Base64.getEncoder().encodeToString(("cursor:" + offset).getBytes(StandardCharsets.UTF_8));
    }
}
