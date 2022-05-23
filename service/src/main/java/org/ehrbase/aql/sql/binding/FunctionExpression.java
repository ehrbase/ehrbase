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

import java.util.ArrayList;
import java.util.List;
import org.ehrbase.aql.definition.FuncParameter;
import org.ehrbase.aql.definition.I_VariableDefinition;

/**
 * handles function expression and parameters.
 */
public class FunctionExpression {

    private final I_VariableDefinition functionDefinition;
    private final VariableDefinitions variables;

    FunctionExpression(VariableDefinitions variables, I_VariableDefinition functionDefinition) {
        this.variables = variables;
        this.functionDefinition = functionDefinition;
    }

    public String toString() {

        StringBuilder expression = new StringBuilder();

        for (FuncParameter parameter : functionDefinition.getFuncParameters()) {
            if (parameter.isVariable()) {
                if (variables.isDistinct(parameter.getValue())) expression.append("DISTINCT ");
                expression.append("\"");
                expression.append(parameter.getValue());
                expression.append("\"");
            } else expression.append(parameter.getValue());
        }
        return expression.toString();
    }

    List<String> arguments() {
        List<String> args = new ArrayList<>();

        for (FuncParameter parameter : functionDefinition.getFuncParameters()) {
            if (parameter.isVariable()) args.add(parameter.getValue());
        }

        return args;
    }
}
