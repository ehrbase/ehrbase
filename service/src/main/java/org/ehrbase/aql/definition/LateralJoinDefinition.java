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

package org.ehrbase.aql.definition;

import org.jooq.*;

public class LateralJoinDefinition {

    private final Table<?> table;
    private final JoinType joinType;
    private final Condition condition;

    public LateralJoinDefinition(Table<?> table, JoinType joinType, Condition condition) {
        this.table = table;
        this.joinType = joinType;
        this.condition = condition;
    }

    public Table<?> getTable(){
        return table;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public Condition getCondition(){
        return condition;
    }
}
