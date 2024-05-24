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
package org.ehrbase.openehr.test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.openehr.sdk.response.dto.MetaData;

public class TestAqlQueryContext implements AqlQueryContext {

    public String executedAql;
    public boolean dryRun;
    public boolean showExecutedAql;
    public boolean showExecutedSql;
    public boolean showQueryPlan;

    public Map<MetaProperty, Object> metaProperties = new HashMap<>();

    public static TestAqlQueryContext create(Consumer<TestAqlQueryContext> block) {
        TestAqlQueryContext queryContext = new TestAqlQueryContext();
        block.accept(queryContext);
        return queryContext;
    }

    @Override
    public MetaData createMetaData(URI location) {
        return null;
    }

    @Override
    public boolean showExecutedAql() {
        return showExecutedAql;
    }

    @Override
    public boolean isDryRun() {
        return dryRun;
    }

    @Override
    public boolean showExecutedSql() {
        return showExecutedSql;
    }

    @Override
    public boolean showQueryPlan() {
        return showQueryPlan;
    }

    @Override
    public void setExecutedAql(String executedAql) {
        this.executedAql = executedAql;
    }

    @Override
    public void setMetaProperty(MetaProperty property, Object value) {
        metaProperties.put(property, value);
    }
}
