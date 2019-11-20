/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.aql.sql;

import org.ehrbase.aql.sql.binding.JsonbBlockDef;
import org.ehrbase.aql.sql.queryImpl.CompositionAttributeQuery;
import org.jooq.Condition;
import org.jooq.SelectQuery;

import java.util.List;

/**
 * Created by christian on 2/17/2017.
 */
public class QuerySteps {
    private final SelectQuery selectQuery;
    private final Condition whereCondition;
    private final String templateId;
    private final CompositionAttributeQuery compositionAttributeQuery;
    private final List<JsonbBlockDef> jsonColumns;
    private final boolean containsJQueryPath;

    public QuerySteps(SelectQuery selectQuery, Condition whereCondition, String templateId, CompositionAttributeQuery compositionAttributeQuery, List<JsonbBlockDef> jsonColumns, boolean containsJQueryPath) {
        this.selectQuery = selectQuery;
        this.whereCondition = whereCondition;
        this.templateId = templateId;
        this.compositionAttributeQuery = compositionAttributeQuery;
        this.jsonColumns = jsonColumns;
        this.containsJQueryPath = containsJQueryPath;
    }

    public SelectQuery getSelectQuery() {
        return selectQuery;
    }

    public Condition getWhereCondition() {
        return whereCondition;
    }

    public String getTemplateId() {
        return templateId;
    }

    public CompositionAttributeQuery getCompositionAttributeQuery() {
        return compositionAttributeQuery;
    }

    public List<JsonbBlockDef> getJsonColumns() {
        return jsonColumns;
    }

    public Integer jsonColumnsSize() {
        if (jsonColumns == null)
            return 0;
        return jsonColumns.size();
    }

    public boolean isContainsJQueryPath() {
        return containsJQueryPath;
    }

    public boolean isContainsJson(){
        return jsonColumnsSize() > 0 || isContainsJQueryPath();
    }
}
