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
import java.util.Optional;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.openehr.sdk.response.dto.MetaData;
import org.springframework.stereotype.Component;

/**
 * AQL query context implementation used for EHRbase CLI that can be configured using
 * <code>-aql-mode=default|dry_run|show_executed_aql|show_executed_sql|show_query_plane</code> params.
 */
@Component(AqlQueryContext.BEAN_NAME)
public class CliAqlQueryContext implements AqlQueryContext {

    private static final String UNSUPPORTED_MSG = "AQL is not supported on CLI";

    @Override
    public MetaData createMetaData(URI location) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    @Override
    public boolean isDryRun() {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    @Override
    public boolean showExecutedAql() {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    @Override
    public boolean showExecutedSql() {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    @Override
    public boolean showQueryPlan() {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    @Override
    public void setExecutedAql(String executedAql) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    @Override
    public void setMetaProperty(MetaProperty property, Object value) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    @Override
    public Optional<String> getHeader(String header) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }
}
