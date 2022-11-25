/*
 * Copyright (c) 2020-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.LateralJoinDefinition;
import org.ehrbase.aql.definition.LateralVariable;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.jooq.JoinType;
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.jooq.SelectSelectStep;
import org.jooq.Table;
import org.jooq.impl.DSL;

/**
 * @author Christian Chevalley
 * @since 1.0
 */
public class LateralJoins {

    private static int seed = 1;

    private LateralJoins() {
        // NOOP
    }

    public static void create(
            String templateId, TaggedStringBuilder encodedVar, I_VariableDefinition item, IQueryImpl.Clause clause) {
        var originalSqlExpression = encodedVar.toString();

        if (originalSqlExpression.isBlank()) return;

        // check for existing lateral join in the SAME variable definition
        if (item.getLateralJoinDefinitions(templateId) != null
                && !item.getLateralJoinDefinitions(templateId).isEmpty()) {
            for (LateralJoinDefinition lateralJoinDefinition : item.getLateralJoinDefinitions(templateId)) {
                if (lateralJoinDefinition.getSqlExpression().equals(originalSqlExpression)) {
                    // use this definition instead of creating a new redundant one
                    item.setAlias(new LateralVariable(
                                    lateralJoinDefinition.getTable().getName(),
                                    lateralJoinDefinition.getLateralVariable())
                            .alias());
                    return;
                }
            }
        }

        int hashValue = encodedVar.toString().hashCode(); // cf. SonarLint
        int abs;
        if (hashValue != 0) abs = Math.abs(hashValue);
        else abs = 0;
        String tableAlias = "array_" + abs + "_" + inc();
        String variableAlias = "var_" + abs + "_" + inc();
        // insert the variable alias used for the lateral join expression
        encodedVar.replaceLast(")", " AS " + variableAlias + ")");
        Table<Record> table = DSL.table(encodedVar.toString()).as(tableAlias);

        LateralJoinDefinition lateralJoinDefinition =
                new LateralJoinDefinition(originalSqlExpression, table, variableAlias, JoinType.JOIN, null, clause);
        reuse(lateralJoinDefinition, templateId, item);
    }

    public static void create(
            String templateId, SelectQuery selectSelectStep, I_VariableDefinition item, IQueryImpl.Clause clause) {
        if (selectSelectStep == null) return;
        int hashValue = selectSelectStep.hashCode(); // cf. SonarLint
        int abs;
        if (hashValue != 0) abs = Math.abs(hashValue);
        else abs = 0;
        String tableAlias = "array_" + abs + "_" + inc();
        String variableAlias = "var_" + abs + "_" + inc();

        SelectSelectStep wrappedSelectSelectStep =
                DSL.select(DSL.field(selectSelectStep).as(variableAlias));

        Table<Record> table = DSL.table(wrappedSelectSelectStep).as(tableAlias);
        item.setLateralJoinTable(
                templateId,
                new LateralJoinDefinition(
                        selectSelectStep.getSQL(),
                        table,
                        variableAlias,
                        JoinType.LEFT_OUTER_JOIN,
                        DSL.condition(true),
                        clause));
        item.setSubstituteFieldVariable(variableAlias);
    }

    public static void reuse(
            LateralJoinDefinition lateralJoinDefinition, String templateId, I_VariableDefinition item) {
        item.setLateralJoinTable(templateId, lateralJoinDefinition);
        item.setAlias(new LateralVariable(
                        lateralJoinDefinition.getTable().getName(), lateralJoinDefinition.getLateralVariable())
                .alias());
    }

    private static synchronized int inc() {
        return seed++;
    }
}
