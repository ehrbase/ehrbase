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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Translates GraphQL filter input maps to jOOQ {@link Condition} chains.
 * All conditions are AND-combined.
 */
public final class FilterTranslator {

    private FilterTranslator() {}

    /**
     * Builds a jOOQ Condition from a GraphQL filter argument map.
     *
     * @param filter  the filter map from GraphQL arguments (e.g., {ehrId: {eq: "..."}, systolicMagnitude: {gt: 120}})
     * @param viewSchema the schema name (e.g., "ehr_views")
     * @param viewName   the view name (e.g., "v_blood_pressure")
     * @return combined AND condition, or {@link DSL#trueCondition()} if no filters
     */
    public static Condition translate(Map<String, Object> filter, String viewSchema, String viewName) {
        if (filter == null || filter.isEmpty()) {
            return DSL.trueCondition();
        }

        Condition condition = DSL.trueCondition();

        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String fieldName = entry.getKey();
            Object filterValue = entry.getValue();

            if ("textSearch".equals(fieldName) && filterValue instanceof Map<?, ?> searchMap) {
                Object query = searchMap.get("query");
                if (query != null) {
                    condition = condition.and(DSL.condition(
                            "search_vector @@ plainto_tsquery('simple', {0})", DSL.val(query.toString())));
                }
                continue;
            }

            if (!(filterValue instanceof Map<?, ?> filterMap)) {
                continue;
            }

            String columnName = camelToSnake(fieldName);
            Field<Object> field = DSL.field(DSL.name(viewSchema, viewName, columnName));

            for (Map.Entry<?, ?> op : filterMap.entrySet()) {
                String operator = op.getKey().toString();
                Object value = op.getValue();
                condition = condition.and(applyOperator(field, operator, value));
            }
        }

        return condition;
    }

    @SuppressWarnings("unchecked")
    private static Condition applyOperator(Field<Object> field, String operator, Object value) {
        return switch (operator) {
            case "eq" -> field.eq(coerce(value));
            case "in" -> {
                if (value instanceof Collection<?> coll) {
                    yield field.in(coll);
                }
                yield field.eq(coerce(value));
            }
            case "gt" -> field.gt(coerce(value));
            case "gte" -> field.ge(coerce(value));
            case "lt" -> field.lt(coerce(value));
            case "lte" -> field.le(coerce(value));
            case "contains" -> field.containsIgnoreCase(value.toString());
            case "startsWith" -> field.startsWith(value.toString());
            case "before" -> field.lt(coerce(value));
            case "after" -> field.gt(coerce(value));
            default -> DSL.trueCondition();
        };
    }

    private static Object coerce(Object value) {
        if (value instanceof String s) {
            if (s.length() == 36 && s.contains("-")) {
                try {
                    return UUID.fromString(s);
                } catch (IllegalArgumentException ignored) {
                    // not a UUID
                }
            }
            try {
                return OffsetDateTime.parse(s);
            } catch (Exception ignored) {
                // not a date
            }
        }
        return value;
    }

    private static String camelToSnake(String camelCase) {
        var sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
