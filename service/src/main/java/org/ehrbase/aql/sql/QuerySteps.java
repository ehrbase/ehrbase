/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.sql;

import java.util.List;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.definition.LateralJoinDefinition;
import org.jooq.Condition;
import org.jooq.SelectQuery;

/**
 * Created by christian on 2/17/2017.
 */
@SuppressWarnings({"java:S3740"})
public class QuerySteps {
    private final SelectQuery selectQuery;
    private Condition whereCondition; // can be force to a NULL condition (f.e. 1 = 0)
    private final List<LateralJoinDefinition> lateralJoins;
    private final String templateId;

    public QuerySteps(
            SelectQuery selectQuery,
            Condition whereCondition,
            List<LateralJoinDefinition> lateralJoins,
            String templateId) {
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

    public List<LateralJoinDefinition> getLateralJoins() {
        return lateralJoins;
    }

    public void setWhereCondition(Condition whereCondition) {
        this.whereCondition = whereCondition;
    }

    public static boolean compare(QuerySteps querySteps1, QuerySteps querySteps2) {

        if (!querySteps1.getTemplateId().equals(querySteps2.getTemplateId())) return false;
        // compare select part
        if (!querySteps1
                .getSelectQuery()
                .getSQL()
                .equals(querySteps2.getSelectQuery().getSQL())) return false;
        if (querySteps1.getLateralJoins().size()
                != querySteps2.getLateralJoins().size()) return false;
        boolean isEquals = IntStream.range(0, querySteps1.getLateralJoins().size())
                .allMatch(j -> querySteps1
                        .getLateralJoins()
                        .get(j)
                        .getSqlExpression()
                        .equals(querySteps2.getLateralJoins().get(j).getSqlExpression()));
        if (!isEquals) return false;
        // check condition
        return StringUtils.equals(
                querySteps1.getWhereCondition() == null
                        ? null
                        : querySteps1.getWhereCondition().toString(),
                querySteps2.getWhereCondition() == null
                        ? null
                        : querySteps2.getWhereCondition().toString());
    }

    public static boolean isIncludedInList(QuerySteps querySteps, List<QuerySteps> queryStepsList) {
        for (QuerySteps queryStepsToCompare : queryStepsList) {
            if (compare(querySteps, queryStepsToCompare)) return true;
        }
        return false;
    }
}
