/*
 * Copyright (c) 2026 vitasystems GmbH.
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

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.ehrbase.api.service.StatusService;
import org.ehrbase.openehr.sdk.response.dto.MetaData;

public abstract class AbstractAqlQueryContext implements AqlQueryContext {
    private final Map<String, Object> metaProperties = new LinkedHashMap<>();
    private final Map<String, Object> properties = new HashMap<>();

    private final StatusService statusService;

    private AqlQueryRequest aqlQueryRequest;
    private String executedAql;

    private final boolean pathNodeSkipping;
    private final boolean archetypeLocalNodePredicates;

    protected AbstractAqlQueryContext(
            StatusService statusService, boolean pathNodeSkipping, boolean archetypeLocalNodePredicates) {
        this.statusService = statusService;
        this.pathNodeSkipping = pathNodeSkipping;
        this.archetypeLocalNodePredicates = archetypeLocalNodePredicates;
    }

    @Override
    public MetaData createMetaData(URI location) {
        MetaData metaData = new MetaData();
        metaData.setCreated(OffsetDateTime.now());
        metaData.setSchemaVersion(OPENEHR_REST_API_VERSION);
        metaData.setType(MetaData.RESULTSET);

        metaData.setHref(Optional.ofNullable(location).map(URI::toASCIIString).orElse(null));

        if (isGeneratorDetailsEnabled()) {
            metaData.setGenerator("EHRBase/%s".formatted(statusService.getEhrbaseVersion()));
        }

        metaData.setExecutedAql(this.executedAql);

        if (isDryRun()) {
            setMetaProperty(EhrbaseMetaProperty.DRY_RUN, true);
        }

        metaProperties.forEach(metaData::setAdditionalProperty);

        return metaData;
    }

    protected abstract boolean isGeneratorDetailsEnabled();

    @Override
    public void setAqlQueryRequest(AqlQueryRequest aqlQueryRequest) {
        this.aqlQueryRequest = aqlQueryRequest;
    }

    @Override
    public AqlQueryRequest getAqlQueryRequest() {
        return aqlQueryRequest;
    }

    @Override
    public boolean isPathSkipping() {
        return pathNodeSkipping;
    }

    @Override
    public boolean isArchetypeLocalNodePredicates() {
        return archetypeLocalNodePredicates;
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

    @Override
    public void setProperty(String name, Object value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
    }

    @Override
    public <T> T getProperty(String key) {
        return (T) properties.get(key);
    }
}
