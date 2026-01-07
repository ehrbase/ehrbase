/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.rest.openehr.aql;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.ehrbase.api.dto.AbstractAqlQueryContext;
import org.ehrbase.api.rest.EHRbaseHeader;
import org.ehrbase.api.service.StatusService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Holds the metadata for the response to an AQL request.
 * <p></p>
 * Note: it is expected that per request no more than one AQL query is executed
 */
@RequestScope
@Component(RequestScopedAqlQueryContext.BEAN_NAME)
public class RequestScopedAqlQueryContext extends AbstractAqlQueryContext {

    public static final String BEAN_NAME = "requestScopedAqlQueryContext";

    @Value("${ehrbase.rest.aql.response.generator-details-enabled:false}")
    boolean generatorDetailsEnabled = false;

    @Value("${ehrbase.rest.aql.response.executed-aql-enabled:true}")
    boolean executedAqlEnabled = true;

    @Value("${ehrbase.rest.aql.debugging-enabled:false}")
    boolean debuggingEnabled = false;

    private final HttpServletRequest request;

    public RequestScopedAqlQueryContext(
            StatusService statusService,
            HttpServletRequest request,
            @Value("${ehrbase.aql.path-node-skipping:false}") boolean pathNodeSkipping,
            @Value("${ehrbase.aql.archetype-local-node-predicates:true}") boolean archetypeLocalNodePredicates) {
        super(statusService, pathNodeSkipping, archetypeLocalNodePredicates);
        this.request = request;
    }

    @Override
    public boolean showExecutedAql() {
        return executedAqlEnabled;
    }

    @Override
    public boolean isDryRun() {
        return debuggingEnabled && isHeaderTrue(EHRbaseHeader.AQL_DRY_RUN);
    }

    @Override
    public boolean showExecutedSql() {
        return debuggingEnabled && isHeaderTrue(EHRbaseHeader.AQL_EXECUTED_SQL);
    }

    @Override
    public boolean showQueryPlan() {
        return debuggingEnabled && isHeaderTrue(EHRbaseHeader.AQL_QUERY_PLAN);
    }

    private boolean isHeaderTrue(String header) {
        return Optional.of(header).map(request::getHeader).map(Boolean::valueOf).orElse(false);
    }

    @Override
    public boolean isGeneratorDetailsEnabled() {
        return generatorDetailsEnabled;
    }

    @Override
    public boolean isPathSkipping() {
        return debuggingEnabled
                ? Optional.of(EHRbaseHeader.AQL_PATH_SKIPPING)
                        .map(request::getHeader)
                        .map(Boolean::valueOf)
                        .orElseGet(super::isPathSkipping)
                : super.isPathSkipping();
    }

    @Override
    public boolean isArchetypeLocalNodePredicates() {
        return debuggingEnabled
                ? Optional.of(EHRbaseHeader.AQL_ARCHETYPE_LOCAL_NODE_PREDICATES)
                        .map(request::getHeader)
                        .map(Boolean::valueOf)
                        .orElseGet(super::isArchetypeLocalNodePredicates)
                : super.isArchetypeLocalNodePredicates();
    }
}
