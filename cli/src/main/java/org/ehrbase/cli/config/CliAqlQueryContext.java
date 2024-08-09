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
package org.ehrbase.cli.config;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.service.StatusService;
import org.ehrbase.openehr.sdk.response.dto.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AQL query context implementation used for EHRbase CLI that can be configured using
 * <code>-aql-mode=default|dry_run|show_executed_aql|show_executed_sql|show_query_plane</code> params.
 */
@Component(AqlQueryContext.BEAN_NAME)
public class CliAqlQueryContext implements AqlQueryContext {

    private static final Logger logger = LoggerFactory.getLogger(CliAqlQueryContext.class);

    private enum AqlMode {
        DEFAULT,
        DRY_RUN,
        SHOW_EXECUTED_SQL,
        SHOW_EXECUTED_AQL,
        SHOW_QUERY_PLAN
    }

    private final AqlMode mode;

    private final StatusService statusService;

    private String executedAql;

    private final Map<String, Object> metaProperties = new LinkedHashMap<>();

    public CliAqlQueryContext(StatusService statusService, @Value("${aql-mode:default}") String aqlMode) {
        this.statusService = statusService;

        this.mode = Arrays.stream(AqlMode.values())
                .filter(it -> it.name().equalsIgnoreCase(aqlMode))
                .findFirst()
                .orElseGet(() -> {
                    logger.warn(
                            "Unknown --aql-mode={} not in supported {} using fallback {}",
                            aqlMode,
                            Arrays.stream(AqlMode.values())
                                    .map(it -> it.name().toLowerCase(Locale.ROOT))
                                    .toList(),
                            AqlMode.DEFAULT.name().toLowerCase(Locale.ROOT));
                    return AqlMode.DEFAULT;
                });
    }

    @Override
    public MetaData createMetaData(URI location) {
        MetaData metaData = new MetaData();
        metaData.setCreated(OffsetDateTime.now());
        metaData.setSchemaVersion(AqlQueryContext.OPENEHR_REST_API_VERSION);
        metaData.setType(MetaData.RESULTSET);

        metaData.setHref(Optional.ofNullable(location).map(URI::toASCIIString).orElse(null));

        metaData.setGenerator("EHRBase-CLI/%s".formatted(statusService.getEhrbaseVersion()));

        metaData.setExecutedAql(this.executedAql);

        if (isDryRun()) {
            setMetaProperty(AqlQueryContext.EhrbaseMetaProperty.DRY_RUN, true);
        }

        metaProperties.forEach(metaData::setAdditionalProperty);

        return metaData;
    }

    @Override
    public boolean isDryRun() {
        return AqlMode.DRY_RUN == mode;
    }

    @Override
    public boolean showExecutedAql() {
        return AqlMode.SHOW_EXECUTED_SQL == mode;
    }

    @Override
    public boolean showExecutedSql() {
        return AqlMode.SHOW_EXECUTED_SQL == mode;
    }

    @Override
    public boolean showQueryPlan() {
        return AqlMode.SHOW_QUERY_PLAN == mode;
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
