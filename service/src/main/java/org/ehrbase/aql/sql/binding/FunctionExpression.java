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
import org.ehrbase.aql.definition.CalFunctionDefinition;
import org.ehrbase.aql.definition.FuncParameter;
import org.ehrbase.aql.definition.FuncParameterType;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * handles function expression and parameters.
 */
public class FunctionExpression {

    private final I_VariableDefinition functionDefinition;
    private final SelectQuery<?> query;
    private final VariableDefinitions variables;

    FunctionExpression(VariableDefinitions variables, I_VariableDefinition functionDefinition, SelectQuery<?> query) {
        this.variables = variables;
        this.functionDefinition = functionDefinition;
        this.query = query;
    }

    public Field<?> buildField() {

        if (functionDefinition instanceof CalFunctionDefinition calFunctionDefinition) {
            return to(calFunctionDefinition.getCast()).cast(getType(calFunctionDefinition.getAs()));
        }

        if (isAggregateDistinct()) {
            return DSL.aggregateDistinct(
                    functionDefinition.getIdentifier(),
                    Object.class,
                    functionDefinition.getFuncParameters().stream()
                            .filter(funcParameter -> !funcParameter.isIdentifier())
                            .map(this::to)
                            .toArray(i -> new Field<?>[i]));
        } else {
            return DSL.function(
                    functionDefinition.getIdentifier(),
                    Object.class,
                    functionDefinition.getFuncParameters().stream()
                            .filter(funcParameter -> !funcParameter.isIdentifier())
                            .map(this::to)
                            .toArray(i -> new Field<?>[i]));
        }
    }

    private DataType<?> getType(String as) {

        return switch (as.toUpperCase()) {
            case "DATE" -> SQLDataType.DATE;
            case "TIME" -> SQLDataType.TIME;
            case "INTERVAL" -> SQLDataType.INTERVAL;
            case "TIMESTAMP" -> SQLDataType.TIMESTAMP;
            case "NUMERIC" -> SQLDataType.NUMERIC;
            default -> SQLDataType.VARCHAR;
        };
    }

    private boolean isAggregateDistinct() {
        return functionDefinition.getFuncParameters().stream()
                .filter(FuncParameter::isVariable)
                .anyMatch(f -> variables.isDistinct(f.getValue().toString()));
    }

    private Field<?> to(FuncParameter funcParameter) {

        return switch (funcParameter.getType()) {
            case VARIABLE -> OrderByBinder.find(query, funcParameter.getValue().toString());
            case OPERAND -> DSL.inline(funcParameter.getValue());
            case IDENTIFIER -> throw new UnsupportedOperationException(
                    funcParameter.getType().toString());
            case FUNCTION -> new FunctionExpression(variables, (I_VariableDefinition) funcParameter.getValue(), query)
                    .buildField();
        };
    }

    List<String> arguments() {
        List<String> args = new ArrayList<>();

        for (FuncParameter parameter : functionDefinition.getFuncParameters()) {
            if (parameter.isVariable()) args.add(parameter.getValue().toString());
            if (parameter.getType().equals(FuncParameterType.FUNCTION)) {
                args.addAll(new FunctionExpression(variables, (I_VariableDefinition) parameter.getValue(), query)
                        .arguments());
            }
        }

        return args;
    }
}
