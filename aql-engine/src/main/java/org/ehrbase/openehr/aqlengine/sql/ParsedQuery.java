/*
 * Copyright (c) 2025 vitasystems GmbH.
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

package org.ehrbase.openehr.aqlengine.sql;

import org.ehrbase.api.dto.AqlQueryRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses an SQL query string, validates the parameters, and adds pagination if needed.
 */
public class ParsedQuery {

    private final String sql;

    private final Map<String, Object> parameters = new HashMap<>();

    private Integer limit;

    private Integer offset;

    public ParsedQuery(AqlQueryRequest queryRequest) {
        Pattern pattern = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher matcher = pattern.matcher(queryRequest.queryString());

        while (matcher.find()) {
            String name = matcher.group(1);
            if (queryRequest.parameters() == null || !queryRequest.parameters().containsKey(name))
                throw new IllegalArgumentException("Missing value for query parameter: " + name);

        }

        this.sql = addPaginationIfNeeded(queryRequest.queryString(), queryRequest.offset(), queryRequest.fetch());
        if (queryRequest.parameters() != null) {
            this.parameters.putAll(queryRequest.parameters());
        }
    }

    public String getSql() {
        return sql;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getOffset() {
        return offset;
    }

    private String addPaginationIfNeeded(String sql, Long offset, Long fetch) {
        String upper = sql.toUpperCase();
        boolean hasLimit = upper.contains("LIMIT");
        boolean hasOffset = upper.contains("OFFSET");

        StringBuilder builder = new StringBuilder(sql);

        if (!hasLimit && fetch != null) {
            builder.append(" LIMIT ").append(fetch);
            this.limit = fetch.intValue();
        }

        if (!hasOffset && offset != null) {
            builder.append(" OFFSET ").append(offset);
            this.offset = offset.intValue();
        }

        return builder.toString();
    }
}
