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
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.rest.EHRbaseHeader;
import org.ehrbase.api.service.StatusService;
import org.ehrbase.openehr.sdk.response.dto.MetaData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Holds the metadata for the response to an AQL request.
 * <p></p>
 * Note: it is expected that per request no more than one AQL query is executed
 */
@RequestScope
@Component(AqlQueryContext.BEAN_NAME)
public class RequestScopedAqlQueryContext implements AqlQueryContext {

    @Value("${ehrbase.rest.aql.response.generator-details-enabled:false}")
    boolean generatorDetailsEnabled = false;

    @Value("${ehrbase.rest.aql.response.executed-aql-enabled:true}")
    boolean executedAqlEnabled = true;

    @Value("${ehrbase.rest.aql.debugging-enabled:false}")
    boolean debuggingEnabled = false;

    @Value("${ehrbase.aql.path-node-skipping:false}")
    private boolean pathNodeSkipping = false;

    @Value("${ehrbase.aql.archetype-local-node-predicates:false}")
    private boolean archetypeLocalNodePredicates = true;

    private final StatusService statusService;
    private final HttpServletRequest request;

    private String executedAql;

    private final Map<String, Object> metaProperties = new LinkedHashMap<>();

    public RequestScopedAqlQueryContext(StatusService statusService, HttpServletRequest request) {
        this.statusService = statusService;
        this.request = request;
    }

    @Override
    public MetaData createMetaData(URI location) {
        MetaData metaData = new MetaData();
        metaData.setCreated(OffsetDateTime.now());
        metaData.setSchemaVersion(OPENEHR_REST_API_VERSION);
        metaData.setType(MetaData.RESULTSET);

        metaData.setHref(Optional.ofNullable(location).map(URI::toASCIIString).orElse(null));

        if (generatorDetailsEnabled) {
            metaData.setGenerator("EHRBase/%s".formatted(statusService.getEhrbaseVersion()));
        }

        metaData.setExecutedAql(this.executedAql);

        if (isDryRun()) {
            setMetaProperty(EhrbaseMetaProperty.DRY_RUN, true);
        }

        metaProperties.forEach(metaData::setAdditionalProperty);

        return metaData;
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

    @Override
    public boolean isPathSkipping() {
        return debuggingEnabled
                ? Optional.of(EHRbaseHeader.AQL_PATH_SKIPPING)
                        .map(request::getHeader)
                        .map(Boolean::valueOf)
                        .orElse(pathNodeSkipping)
                : pathNodeSkipping;
    }

    @Override
    public boolean isArchetypeLocalNodePredicates() {
        return debuggingEnabled
                ? Optional.of(EHRbaseHeader.AQL_ARCHETYPE_LOCAL_NODE_PREDICATES)
                        .map(request::getHeader)
                        .map(Boolean::valueOf)
                        .orElse(archetypeLocalNodePredicates)
                : archetypeLocalNodePredicates;
    }

    private boolean isHeaderTrue(String header) {
        return Optional.of(header).map(request::getHeader).map(Boolean::valueOf).orElse(false);
    }

    @Override
    public void setExecutedAql(String executedAql) {
        this.executedAql = executedAql;
    }

    @Override
    public void setMetaProperty(MetaProperty property, Object value) {
        String name = property.propertyName();
        if (value == null) {
            metaProperties.remove(name);
        } else {
            metaProperties.put(name, value);
        }
    }
}
