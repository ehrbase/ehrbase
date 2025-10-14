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
package org.ehrbase.openehr.aqlengine.aql;

import org.apache.commons.lang3.ObjectUtils;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Applies upper bounds and/or defaults for limit/fetch to an {@link AqlQuery}.
 */
@Component
public class AqlLimitPostProcessor implements AqlQueryParsingPostProcessor {

    public enum FetchPrecedence {
        /**
         * Fail if both fetch and limit are present
         */
        REJECT,
        /**
         * Take minimum of fetch and limit for limit;
         * fail if query has offset
         */
        MIN_FETCH
    }

    private final Long defaultLimit;
    private final Long maxLimit;
    private final Long maxFetch;
    private final FetchPrecedence fetchPrecedence;

    public AqlLimitPostProcessor(
            @Value("${ehrbase.rest.aql.default-limit:}") Long defaultLimit,
            @Value("${ehrbase.rest.aql.max-limit:}") Long maxLimit,
            @Value("${ehrbase.rest.aql.max-fetch:}") Long maxFetch,
            @Value("${ehrbase.rest.aql.fetch-precedence:REJECT}") FetchPrecedence fetchPrecedence) {
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
        this.maxFetch = maxFetch;
        this.fetchPrecedence = fetchPrecedence;
    }

    @Override
    public int getOrder() {
        return LIMIT_CONSTRAINT_PRECEDENCE;
    }

    @Override
    public void afterParseAql(final AqlQuery aqlQuery, final AqlQueryRequest request, final AqlQueryContext ctx) {

        // add defaults and upper bounds to meta information
        if (defaultLimit != null) {
            ctx.setMetaProperty(AqlQueryContext.EhrbaseMetaProperty.DEFAULT_LIMIT, defaultLimit);
        }
        if (maxLimit != null) {
            ctx.setMetaProperty(AqlQueryContext.EhrbaseMetaProperty.MAX_LIMIT, maxLimit);
        }
        if (maxFetch != null) {
            ctx.setMetaProperty(AqlQueryContext.EhrbaseMetaProperty.MAX_FETCH, maxFetch);
        }

        // apply limit and offset - where the definitions from the aql are the precedence
        Long fetchParam = request.fetch();
        Long offsetParam = request.offset();

        Long queryLimit = aqlQuery.getLimit();
        Long queryOffset = aqlQuery.getOffset();

        if (queryLimit != null && maxLimit != null && queryLimit > maxLimit) {
            throw new UnprocessableEntityException(
                    "Query LIMIT %d exceeds maximum limit %d".formatted(queryLimit, maxLimit));
        }

        if (fetchParam != null && maxFetch != null && fetchParam > maxFetch) {
            throw new UnprocessableEntityException(
                    "Fetch parameter %d exceeds maximum fetch %d".formatted(fetchParam, maxFetch));
        }

        Long limit = applyFetchPrecedence(fetchPrecedence, queryLimit, queryOffset, fetchParam, offsetParam);

        aqlQuery.setLimit(ObjectUtils.firstNonNull(limit, defaultLimit));
        aqlQuery.setOffset(ObjectUtils.firstNonNull(offsetParam, queryOffset));
    }

    private static Long applyFetchPrecedence(
            FetchPrecedence fetchPrecedence, Long queryLimit, Long queryOffset, Long fetchParam, Long offsetParam) {

        if (fetchParam == null) {
            if (offsetParam != null) {
                throw new UnprocessableEntityException("Query parameter for offset provided, but no fetch parameter");
            }
            return queryLimit;
        } else if (queryLimit == null) {
            assert queryOffset == null;
            return fetchParam;
        }

        return switch (fetchPrecedence) {
            case REJECT -> {
                throw new UnprocessableEntityException(
                        "Query contains a LIMIT clause, fetch and offset parameters must not be used (with fetch precedence %s)"
                                .formatted(fetchPrecedence));
            }
            case MIN_FETCH -> {
                if (queryOffset != null) {
                    throw new UnprocessableEntityException(
                            "Query contains a OFFSET clause, fetch parameter must not be used (with fetch precedence %s)"
                                    .formatted(fetchPrecedence));
                }
                yield Math.min(queryLimit, fetchParam);
            }
        };
    }
}
