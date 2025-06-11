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

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.openehr.sdk.response.dto.MetaData;

public class TestAqlQueryContext implements AqlQueryContext {

    private final Map<String, Object> metaProperties = new LinkedHashMap<>();
    private final Map<String, String> header = new LinkedHashMap<>();

    public TestAqlQueryContext() {
        this(Map.of());
    }

    public TestAqlQueryContext(Map<String, String> header) {
        this.header.putAll(header);
    }

    @Override
    public MetaData createMetaData(final URI location) {
        return null;
    }

    @Override
    public boolean showExecutedAql() {
        return false;
    }

    @Override
    public boolean isDryRun() {
        return false;
    }

    @Override
    public boolean showExecutedSql() {
        return false;
    }

    @Override
    public boolean showQueryPlan() {
        return false;
    }

    @Override
    public void setExecutedAql(final String executedAql) {
        metaProperties.put("executedAql", executedAql);
    }

    @Override
    public Optional<String> getHeader(String header) {
        return Optional.ofNullable(this.header.get(header));
    }

    @Override
    public void setMetaProperty(final MetaProperty property, final Object value) {
        String name = property.propertyName();
        if (value == null) {
            metaProperties.remove(name);
        } else {
            metaProperties.put(name, value);
        }
    }
}
