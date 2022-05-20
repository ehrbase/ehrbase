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

import com.google.common.base.Strings;
import java.util.Iterator;
import org.ehrbase.aql.compiler.OrderAttribute;
import org.ehrbase.aql.compiler.Statements;
import org.ehrbase.aql.definition.I_VariableDefinition;

public class OrderByField {

    private Statements statements;

    public OrderByField(Statements statements) {
        this.statements = statements;
    }

    private boolean containsOrderBy() {
        return (statements.getOrderAttributes() != null
                && !statements.getOrderAttributes().isEmpty());
    }

    public Statements merge() {
        // add order by fields as HIDDEN variables
        if (!containsOrderBy()) return statements;

        for (OrderAttribute orderAttribute : statements.getOrderAttributes()) {
            I_VariableDefinition orderAttributeVariable = orderAttribute.getVariableDefinition();

            if (!isSelectVariable(orderAttributeVariable)) {
                // add it to the statement as an hidden variable
                I_VariableDefinition orderVariable = orderAttributeVariable.duplicate();
                orderVariable.setHidden(true);
                statements.put(orderVariable);
            }
        }

        return statements;
    }

    private boolean isSelectVariable(I_VariableDefinition aVariable) {
        Iterator<I_VariableDefinition> variableDefinitions =
                statements.getVariables().iterator();

        while (variableDefinitions.hasNext()) {
            I_VariableDefinition variableDefinition = variableDefinitions.next();

            if (aVariable.getAlias() != null
                    && Strings.nullToEmpty(variableDefinition.getAlias())
                            .equals(Strings.nullToEmpty(aVariable.getAlias()))) return true;
            if (aVariable.getPath() != null
                    && Strings.nullToEmpty(variableDefinition.getPath())
                            .equals(Strings.nullToEmpty(aVariable.getPath()))) return true;
            // NB. order by variable don't have an identifier
        }
        return false;
    }
}
