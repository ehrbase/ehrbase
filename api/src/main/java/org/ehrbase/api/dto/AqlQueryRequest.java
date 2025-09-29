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
import org.ehrbase.api.exception.IllegalAqlException;
import org.ehrbase.api.service.AqlQueryService;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlParseException;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;

/**
 * The requested AQL to be executed by {@link AqlQueryService#query(AqlQueryRequest)}.
 *
 * @param aqlQuery    the actual aql query
 * @param parameters  additional query parameters
 * @param fetch       query limit to apply
 * @param offset      query offset to apply
 */
public record AqlQueryRequest(String aqlString, AqlQuery aqlQuery, Map<String, Object> parameters, Long fetch, Long offset) {

    /**
     * Create a new {@link AqlQueryRequest} by parsing the given AQL <code>queryString</code>.
     *
     * @see AqlQueryRequest
     */
    public static AqlQueryRequest prepare(String queryString, Map<String, Object> parameters, Long fetch, Long offset) {
        try {
            AqlQuery aqlQuery = AqlQueryParser.parse(queryString);
            return new AqlQueryRequest(queryString, aqlQuery, parameters, fetch, offset);
        } catch (AqlParseException e) {
            throw new IllegalAqlException(
                    "Could not parse AQL query: "
                            + Optional.of(e).map(Throwable::getCause).orElse(e).getMessage(),
                    e);
        }
    }
}
