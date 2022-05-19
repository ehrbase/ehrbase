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
package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.ConstantDefinition;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.queryimpl.DefaultColumnId;
import org.jooq.Field;
import org.jooq.impl.DSL;

@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class ConstantField {

    private final I_VariableDefinition variableDefinition;

    public ConstantField(I_VariableDefinition variableDefinition) {
        this.variableDefinition = variableDefinition;
    }

    Field<?> toSql() {
        Field<?> field;

        ConstantDefinition constantDefinition = (ConstantDefinition) variableDefinition;
        if (constantDefinition.getValue() == null) // assume NULL
        field = DSL.field("NULL");
        else field = DSL.field(DSL.val(constantDefinition.getValue()));

        if (constantDefinition.getAlias() != null) field = field.as(constantDefinition.getAlias());
        else {
            String defaultAlias = DefaultColumnId.value(constantDefinition);
            field = field.as("/" + defaultAlias);
            constantDefinition.setPath(defaultAlias);
        }
        return field;
    }
}
