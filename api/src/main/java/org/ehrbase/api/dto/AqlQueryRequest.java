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
package org.ehrbase.api.dto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.ehrbase.api.definitions.QueryType;
import org.ehrbase.api.service.AqlQueryService;

/**
 * The requested AQL to be executed by {@link AqlQueryService#query(AqlQueryRequest)}.
 *
 * @param queryString          the actual aql query string
 * @param parameters           additional query parameters
 * @param fetch                query limit to apply
 * @param offset               query offset to apply
 */
public record AqlQueryRequest(
        @Nonnull QueryType type,
        @Nonnull String queryString,
        @Nullable Map<String, Object> parameters,
        @Nullable Long fetch,
        @Nullable Long offset) {

    public AqlQueryRequest(
            @Nonnull String queryString,
            @Nullable Map<String, Object> parameters,
            @Nullable Long fetch,
            @Nullable Long offset) {
        this(QueryType.AQL, queryString, parameters, fetch, offset);
    }

    public AqlQueryRequest(
            @Nonnull QueryType type,
            @Nonnull String queryString,
            @Nullable Map<String, Object> parameters,
            @Nullable Long fetch,
            @Nullable Long offset) {
        this.type = type;
        this.queryString = queryString;
        this.parameters = rewriteExplicitParameterTypes(parameters);
        this.fetch = fetch;
        this.offset = offset;
    }

    public static Map<String, Object> rewriteExplicitParameterTypes(Map<String, Object> parameters) {
        if (parameters == null) {
            return Map.of();
        }
        parameters.entrySet().forEach(e -> {
            Object ov = e.getValue();
            Object nv = handleExplicitParameterTypes(ov);
            if (ov != nv) {
                e.setValue(nv);
            }
        });
        return parameters;
    }

    /**
     * Allows for explicit types via xml: <param type="int">1</param> in query parameters.
     */
    private static Object handleExplicitParameterTypes(Object paramValue) {
        final Object result;
        if (paramValue instanceof Map<?, ?> m) {
            if (m.get("type") instanceof String type) {
                result = switch (type) {
                    case "int" -> intValue(m, "").orElse(paramValue);
                    case "num" -> numValue(m, "").orElse(paramValue);
                    default -> handleExplicitParameterTypes(m.get(""));};
            } else if (m.get("") instanceof List children && !children.isEmpty()) {
                result = children.stream()
                        .map(AqlQueryRequest::handleExplicitParameterTypes)
                        .toList();
            } else {
                result = intValue(m, "int").orElseGet(() -> numValue(m, "num").orElse(paramValue));
            }
        } else if (paramValue instanceof List l) {
            for (int i = 0, s = l.size(); i < s; i++) {
                var v = l.get(i);
                var n = handleExplicitParameterTypes(v);
                if (v != n) {
                    l.set(i, n);
                }
            }
            result = paramValue;
        } else {
            result = paramValue;
        }
        return result;
    }

    private static Optional<Object> intValue(Map<?, ?> paramValues, String key) {
        return Optional.of(key).map(paramValues::get).map(Object::toString).map(Integer::parseInt);
    }

    private static Optional<Object> numValue(Map<?, ?> paramValues, String key) {
        return Optional.of(key).map(paramValues::get).map(Object::toString).map(Double::parseDouble);
    }
}
