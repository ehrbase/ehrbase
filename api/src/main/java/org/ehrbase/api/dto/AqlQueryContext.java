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

import java.net.URI;
import org.ehrbase.openehr.sdk.response.dto.MetaData;

public interface AqlQueryContext {

    interface MetaProperty {
        String propertyName();
    }

    enum EhrbaseMetaProperty implements MetaProperty {
        OFFSET("offset"),
        FETCH("fetch"),
        RESULT_SIZE("resultsize"),
        DRY_RUN("dry_run"),
        EXECUTED_SQL("executed_sql"),
        QUERY_PLAN("query_plan");

        private final String propertyName;

        EhrbaseMetaProperty(String propertyName) {
            this.propertyName = propertyName;
        }

        @Override
        public String propertyName() {
            return propertyName;
        }
    }

    String OPENEHR_REST_API_VERSION = "1.0.3";

    MetaData createMetaData(URI location);

    boolean showExecutedAql();

    boolean isDryRun();

    boolean showExecutedSql();

    boolean showQueryPlan();

    void setExecutedAql(String executedAql);

    void setMetaProperty(MetaProperty property, Object value);
}
