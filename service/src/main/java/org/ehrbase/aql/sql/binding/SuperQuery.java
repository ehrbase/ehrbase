/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) Ripple Foundation CIC Ltd, UK, 2017
 * Author: Christian Chevalley
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

package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.FuncParameter;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.Variables;
import org.ehrbase.aql.sql.queryImpl.DefaultColumnId;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by christian on 9/20/2017.
 */
public class SuperQuery {

    private List<I_VariableDefinition> variableDefinitions;
    private SelectQuery query;
    private DSLContext context;

    public SuperQuery(DSLContext context, List<I_VariableDefinition> variableDefinitions, SelectQuery query) {
        this.context = context;
        this.variableDefinitions = variableDefinitions;
        this.query = query;
    }

    public List<Field> selectFields() {

        List<Field> fields = new ArrayList<>();

        for (I_VariableDefinition variableDefinition : variableDefinitions) {
            if (variableDefinition.getAlias() == null || variableDefinition.getAlias().isEmpty())
                fields.add(DSL.fieldByName(new DefaultColumnId().value(variableDefinition))); //CR #50
            else
                fields.add(DSL.fieldByName(variableDefinition.getAlias()));
        }

        return fields;


    }

    public SelectQuery selectDistinct() {

        SelectQuery selectQuery = context.selectQuery();

        List<Field> fields = selectFields();

        selectQuery.addDistinctOn(fields);

        selectQuery.addFrom(query);

        return selectQuery;
    }

    public SelectQuery selectAggregate() {

        SelectQuery selectQuery = context.selectQuery();

        List<Field> fields = new ArrayList<>();
        List<String> skipField = new ArrayList<>();
        for (I_VariableDefinition variableDefinition : variableDefinitions) {
            String alias = variableDefinition.getAlias() == null || variableDefinition.getAlias().isEmpty() ? variableDefinition.getPath() : variableDefinition.getAlias();
            if (variableDefinition.isFunction()) {
                skipField.add(alias);
                Field field = DSL.field(functionExpression(variableDefinition));
                if (variableDefinition.getAlias() != null)
                    field = field.as(alias);
                fields.add(field);
            } else if (variableDefinition.isExtension()) {
                //do nothing... for the time being
            } else {
                //check if this alias is serviced by a function
                if (skipField.contains(alias))
                    continue;

                fields.add(DSL.fieldByName(alias));
            }
        }

        selectQuery.addSelect(fields);

        selectQuery.addFrom(query);

        return selectQuery;
    }

    private String functionExpression(I_VariableDefinition variableDefinition) {

        StringBuffer expression = new StringBuffer();

        for (FuncParameter parameter : variableDefinition.getFuncParameters()) {
            if (parameter.isVariable()) {
                expression.append("\"");
                expression.append(parameter.getValue());
                expression.append("\"");
            } else
                expression.append(parameter.getValue());
        }
        return expression.toString();
    }

    public SelectQuery select() {

        if (new Variables(variableDefinitions).hasDefinedDistinct()) { //build a super select
            return selectDistinct();
        } else if (new Variables(variableDefinitions).hasDefinedFunction()) {
            return selectAggregate();
        }

        throw new IllegalArgumentException("Don't know how to handle super query");
    }
}
