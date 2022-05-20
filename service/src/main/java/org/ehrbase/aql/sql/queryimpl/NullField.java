/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.queryimpl;

import static org.ehrbase.aql.sql.queryimpl.JsonbEntryQuery.MAGNITUDE;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class NullField {

    private final I_VariableDefinition variableDefinition;
    private final String alias;

    public NullField(I_VariableDefinition variableDefinition, String alias) {
        this.variableDefinition = variableDefinition;
        this.alias = alias;
    }

    public Field<?> instance() {
        // return a null field
        String cast = "";
        // force explicit type cast for DvQuantity
        if (variableDefinition != null
                && variableDefinition.getPath() != null
                && variableDefinition.getPath().endsWith(MAGNITUDE)) cast = "::numeric";

        if (variableDefinition != null && alias != null)
            return DSL.field(DSL.val((String) null) + cast).as(alias);
        else return DSL.field(DSL.val((String) null) + cast);
    }
}
