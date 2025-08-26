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
package org.ehrbase.rest.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ehrbase.api.exception.InvalidApiParameterException;

public final class StoredQueryRequestUtils {

    private StoredQueryRequestUtils() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getQueryParametersFromBody(String key, Map<String, Object> requestBody) {
        if (requestBody == null || requestBody.isEmpty()) {
            return Map.of();
        }
        return Optional.ofNullable(requestBody.get(key))
                .map(p -> (Map<String, Object>) p)
                .orElseGet(Map::of);
    }

    // rewrite is needed for explicit XML params
    public static Map<String, Object> rewriteExplicitParameterTypes(Map<String, Object> parameters) {
        if (parameters == null) {
            return Map.of();
        }
        return parameters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> handleExplicitParameterTypes(entry.getValue())));
    }

    public static Optional<Long> optionalLong(String name, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(name).map(params::get).map(o -> switch (o) {
            case Integer i -> i.longValue();
            case Long l -> l;
            case String s -> {
                try {
                    yield Long.valueOf(s);
                } catch (NumberFormatException e) {
                    throw new InvalidApiParameterException("invalid '%s' value '%s'".formatted(name, s));
                }
            }
            default -> throw new InvalidApiParameterException("invalid '%s' value '%s'".formatted(name, o));
        });
    }

    /**
     * Allows for explicit types via xml: <param type="int">1</param> in query parameters.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object handleExplicitParameterTypes(Object paramValue) {
        return switch (paramValue) {
            case Map<?, ?> map -> {
                if (map.get("type") instanceof String type) {
                    yield switch (type) {
                        case "int" -> intValue(map, "").orElse(paramValue);
                        case "num" -> numValue(map, "").orElse(paramValue);
                        default -> handleExplicitParameterTypes(map.get(""));
                    };
                } else if (map.get("") instanceof List children && !children.isEmpty()) {
                    yield children.stream()
                            .map(StoredQueryRequestUtils::handleExplicitParameterTypes)
                            .toList();
                } else {
                    yield intValue(map, "int")
                            .orElseGet(() -> numValue(map, "num").orElse(paramValue));
                }
            }
            case List list -> {
                for (int i = 0, s = list.size(); i < s; i++) {
                    var value = list.get(i);
                    var normalized = handleExplicitParameterTypes(value);
                    if (value != normalized) {
                        list.set(i, normalized);
                    }
                }
                yield paramValue;
            }
            default -> paramValue;
        };
    }

    private static Optional<Object> intValue(Map<?, ?> paramValues, String key) {
        return Optional.of(key).map(paramValues::get).map(Object::toString).map(Integer::parseInt);
    }

    private static Optional<Object> numValue(Map<?, ?> paramValues, String key) {
        return Optional.of(key).map(paramValues::get).map(Object::toString).map(Double::parseDouble);
    }
}
