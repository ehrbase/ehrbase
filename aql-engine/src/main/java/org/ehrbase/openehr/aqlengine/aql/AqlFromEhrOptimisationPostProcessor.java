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

import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.springframework.stereotype.Component;

/**
 * Removes unused EHR at the start of the FROM statement of an {@link AqlQuery} for simple cases.
 */
@Component
public class AqlFromEhrOptimisationPostProcessor implements AqlQueryParsingPostProcessor {
    @Override
    public int getOrder() {
        return FROM_EHR_OPTIMISATION_PRECEDENCE;
    }

    @Override
    public void afterParseAql(final AqlQuery aqlQuery, final AqlQueryRequest request, final AqlQueryContext ctx) {
        // remove unused FROM EHR
        if (aqlQuery.getFrom() instanceof ContainmentClassExpression containment
                && RmConstants.EHR.equals(containment.getType())
                && containment.getContains() instanceof ContainmentClassExpression childContainment
                && !isReferenced(containment, aqlQuery)) {
            aqlQuery.setFrom(childContainment);
        }
    }

    protected static boolean isReferenced(ContainmentClassExpression containment, AqlQuery aqlQuery) {
        if (containment.getPredicates() != null) {
            return true;
        }
        String identifier = containment.getIdentifier();
        if (identifier == null) {
            return false;
        } else {
            return AqlQueryUtils.allIdentifiedPaths(aqlQuery)
                    .anyMatch(p -> p.getRoot().equals(containment));
        }
    }
}
