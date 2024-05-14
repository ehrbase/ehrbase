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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
        @Nonnull String queryString,
        @Nullable Map<String, Object> parameters,
        @Nullable Long fetch,
        @Nullable Long offset) {

    public AqlQueryRequest(
            @Nonnull String queryString,
            @Nullable Map<String, Object> parameters,
            @Nullable Long fetch,
            @Nullable Long offset) {
        this.queryString = queryString;
        this.parameters = Optional.ofNullable(parameters).map(Map::entrySet).stream()
                .flatMap(Set::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> handleExplicitParameterTypes(e.getValue())));
        this.fetch = fetch;
        this.offset = offset;
    }

    /**
     * Allows for explicit types via xml: <param type="int">1</param> in query parameters.
     */
    private static Object handleExplicitParameterTypes(Object paramValue) {
        if (paramValue instanceof Map<?, ?> m) {
            Object typeVal = m.get("type");
            if (typeVal instanceof String type)
                paramValue = switch (type) {
                    case "int" -> Optional.of("")
                            .map(m::get)
                            .map(Object::toString)
                            .<Object>map(Integer::parseInt)
                            .orElse(paramValue);
                    case "num" -> Optional.of("")
                            .map(m::get)
                            .map(Object::toString)
                            .<Object>map(Double::parseDouble)
                            .orElse(paramValue);
                    default -> paramValue;};
        }
        return paramValue;
    }
}
