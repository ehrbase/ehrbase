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

import org.ehrbase.aql.compiler.OrderAttribute;
import org.ehrbase.aql.definition.*;
import org.ehrbase.aql.sql.queryImpl.DefaultColumnId;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by christian on 9/20/2017.
 */
public class SuperQuery {

    private VariableDefinitions variableDefinitions;
    private SelectQuery query;
    private DSLContext context;

    public SuperQuery(DSLContext context, VariableDefinitions variableDefinitions, SelectQuery query) {
        this.context = context;
        this.variableDefinitions = variableDefinitions;
        this.query = query;
    }

    @SuppressWarnings( "deprecation" )
    private List<Field> selectFields() {

        List<Field> fields = new ArrayList<>();
        Iterator<I_VariableDefinition> iterator = variableDefinitions.iterator();

        while (iterator.hasNext()) {
            I_VariableDefinition variableDefinition = iterator.next();
            if (variableDefinition instanceof FunctionDefinition){
                StringBuilder stringBuilder = new StringBuilder();
                for (FuncParameter funcParameter: ((FunctionDefinition) variableDefinition).getParameters()){
                    stringBuilder.append(funcParameter.getValue());
                }
                fields.add(DSL.fieldByName(stringBuilder.toString()));

            }
            else {
                if (variableDefinition.getAlias() == null || variableDefinition.getAlias().isEmpty())
                    fields.add(DSL.fieldByName(new DefaultColumnId().value(variableDefinition))); //CR #50
                else
                    fields.add(DSL.fieldByName(variableDefinition.getAlias()));
            }
        }

        return fields;


    }

    @SuppressWarnings("unchecked")
    private SelectQuery selectDistinct(SelectQuery selectQuery) {

        List<Field> fields = selectFields();

        selectQuery.addDistinctOn(fields);

        selectQuery.addFrom(query);

        return selectQuery;
    }

    @SuppressWarnings( {"deprecation", "unchecked"} )
    private SelectQuery selectAggregate(SelectQuery selectQuery) {

        List<Field> fields = new ArrayList<>();
        List<String> skipField = new ArrayList<>();
        Iterator<I_VariableDefinition> iterator = variableDefinitions.iterator();
        boolean distinctRequired = false;

        while (iterator.hasNext()) {
            I_VariableDefinition variableDefinition = iterator.next();
            String alias = variableDefinition.getAlias() == null || variableDefinition.getAlias().isEmpty() ? variableDefinition.getPath() : variableDefinition.getAlias();
            if (variableDefinition.isFunction()) {
                skipField.add(alias);

                FunctionExpression functionExpression = new FunctionExpression(variableDefinitions, variableDefinition);

                Field field = DSL.field(functionExpression.toString());

                skipField.addAll(functionExpression.arguments());

                if (variableDefinition.getAlias() != null) {
                    field = field.as(alias);
                }

                fields.add(field);
            } else if (variableDefinition.isExtension()) {
                //TODO:do nothing... for the time being
            } else {
                //check if this alias is serviced by a function
                //check if this alias requires distinct
                distinctRequired = distinctRequired || variableDefinition.isDistinct();

                if (skipField.contains(alias))
                    continue;

                if (variableDefinition.isDistinct())
                    fields.add(DSL.fieldByName("DISTINCT "+ alias));
                else
                    fields.add(DSL.fieldByName(alias));
            }
        }


        selectQuery.addSelect(fields);


        selectQuery.addFrom(query);

        return selectQuery;
    }

    @SuppressWarnings("unchecked")
    public SelectQuery selectOrderBy(List<OrderAttribute> orderAttributes) {

        SelectQuery selectQuery = context.selectQuery();

        selectQuery.addFrom(query);

        selectQuery = setOrderBy(orderAttributes, selectQuery);

        return selectQuery;
    }

    @SuppressWarnings("unchecked")
    public SelectQuery setOrderBy(List<OrderAttribute> orderAttributes, SelectQuery selectQuery){
        return new OrderByBinder(variableDefinitions, orderAttributes, selectQuery).bind();
    }

    public SelectQuery select() {

        SelectQuery selectQuery = context.selectQuery();

        if (new Variables(variableDefinitions).hasDefinedFunction()) {
            selectQuery = selectAggregate(selectQuery);
        }
        else if (new Variables(variableDefinitions).hasDefinedDistinct()) { //build a super select
            selectQuery = selectDistinct(selectQuery);
        }

        return selectQuery;
    }
}
