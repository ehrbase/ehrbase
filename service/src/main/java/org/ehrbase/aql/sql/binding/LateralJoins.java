/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.jooq.*;
import org.jooq.impl.DSL;

public class LateralJoins {

    private static int seed = 1;

    public void create(String templateId, TaggedStringBuilder encodedVar, I_VariableDefinition item){
        if (encodedVar == null)
            return;
        int hashValue = encodedVar.toString().hashCode(); //cf. SonarLint
        int abs;
        if (hashValue != 0)
            abs = Math.abs(hashValue);
        else
            abs = 0;
        String tableAlias = "array_" + abs + "_" + inc();
        String variableAlias = "var_" + abs + "_" + inc();
        //insert the variable alias used for the lateral join expression
        encodedVar.replaceLast(")", " AS " + variableAlias + ")");
        Table<Record> table = DSL.table(encodedVar.toString()).as(tableAlias);
        item.setLateralJoinTable(templateId, table, JoinType.JOIN, null);
        item.setAlias(tableAlias + "." + variableAlias + " ");
    }

    public void create(String templateId, SelectQuery selectSelectStep, I_VariableDefinition item){
        if (selectSelectStep == null)
            return;
        int hashValue = selectSelectStep.hashCode(); //cf. SonarLint
        int abs;
        if (hashValue != 0)
            abs = Math.abs(hashValue);
        else
            abs = 0;
        String tableAlias = "array_" + abs + "_" + inc();
        String variableAlias = "var_" + abs + "_" + inc();

        SelectSelectStep wrappedSelectSelectStep = DSL.select(DSL.field(selectSelectStep).as(variableAlias));

        Table<Record> table = DSL.table(wrappedSelectSelectStep).as(tableAlias);
        item.setLateralJoinTable(templateId, table, JoinType.LEFT_OUTER_JOIN, DSL.condition(true));
        item.setSubstituteFieldVariable(variableAlias);
    }

    private static synchronized int inc(){
        return seed++;
    }
}
