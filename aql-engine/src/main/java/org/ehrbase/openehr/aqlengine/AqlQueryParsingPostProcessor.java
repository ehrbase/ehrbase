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
package org.ehrbase.openehr.aqlengine;

import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.springframework.core.Ordered;

/**
 * Used for modifications of the parsed AQL query directly after parsing.
 * All Spring beans implementing this interface will be picked up by AqlQueryService.
 * <p>
 * Specifying the Order is required since inplace modifications may affect each other.
 * Having multiple beans of this type with the same order value may produce inconsistent results.
 */
public interface AqlQueryParsingPostProcessor extends Ordered {

    /**
     * Invoked after parsing the raw AQL string and before running the {@link org.ehrbase.openehr.aqlengine.featurecheck.AqlQueryFeatureCheck}.
     * The given {@link AqlQuery} object can be modified inplace.
     * Meta information can be added through the {@link AqlQueryContext}.
     *
     * @param aqlQuery object representing the parsed AQL string
     * @param request  the request passed to the service layer
     * @param ctx the query context
     */
    default void afterParseAql(AqlQuery aqlQuery, AqlQueryRequest request, AqlQueryContext ctx) {}

    /**
     * Invoked after running the AqlQueryFeatureCheck and before building the AqlQueryWrapper.
     * The given AqlQuery object can be modified inplace.
     * All modifications made here must be supported by the engine!
     *
     * @param aqlQuery object representing the parsed AQL string
     * @param request the request passed to the service layer
     */
    default void afterFeatureCheck(AqlQuery aqlQuery, AqlQueryRequest request, AqlQueryContext ctx) {}
}
