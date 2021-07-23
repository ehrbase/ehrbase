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

import org.jooq.Condition;
import org.jooq.SelectQuery;
import org.jooq.Table;

import java.util.List;

/**
 * Created by christian on 2/17/2017.
 */
@SuppressWarnings({"java:S3740"})
public class QuerySteps {
    private final SelectQuery selectQuery;
    private Condition whereCondition; //can be force to a NULL condition (f.e. 1 = 0)
    private final List<Table<?>> lateralJoins;
    private final String templateId;

    public QuerySteps(SelectQuery selectQuery, Condition whereCondition, List<Table<?>> lateralJoins, String templateId) {
        this.selectQuery = selectQuery;
        this.whereCondition = whereCondition;
        this.lateralJoins = lateralJoins;
        this.templateId = templateId;
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

    public List<Table<?>> getLateralJoins() {
        return lateralJoins;
    }

    public void setWhereCondition(Condition whereCondition){
        this.whereCondition = whereCondition;
    }
}
