/*
 * Copyright (c) 2017-2022 vitasystems GmbH and Hannover Medical School.
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

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.compiler.OrderAttribute;
import org.ehrbase.aql.definition.FuncParameter;
import org.ehrbase.aql.definition.FunctionDefinition;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.Variables;
import org.ehrbase.aql.sql.queryimpl.DefaultColumnId;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Chevalley
 * @author Renaud Subiger
 * @since 1.0
 */
public class SuperQuery {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DSLContext context;

    private final VariableDefinitions variableDefinitions;

    private final SelectQuery<?> query;

    private boolean outputWithJson;

    public SuperQuery(
            I_DomainAccess domainAccess,
            VariableDefinitions variableDefinitions,
            SelectQuery<?> query,
            boolean containsJson) {
        this.context = domainAccess.getContext();
        this.variableDefinitions = variableDefinitions;
        this.query = query;
        this.outputWithJson = containsJson;
    }

    private List<Field<?>> selectDistinctFields() {
        List<Field<?>> fields = new ArrayList<>();
        Iterator<I_VariableDefinition> iterator = variableDefinitions.iterator();

        // check if the list of variable contains at least ONE distinct statement
        if (!variableDefinitions.hasDistinctOperator()) {
            return fields;
        }

        while (iterator.hasNext()) {
            I_VariableDefinition variableDefinition = iterator.next();
            if (variableDefinition instanceof FunctionDefinition) {
                StringBuilder stringBuilder = new StringBuilder();
                for (FuncParameter funcParameter : variableDefinition.getFuncParameters()) {
                    stringBuilder.append(funcParameter.getValue());
                }
                fields.add(field(name(stringBuilder.toString())));

            } else {
                if (variableDefinition.getAlias() == null
                        || variableDefinition.getAlias().isEmpty()) {
                    fields.add(field(name(DefaultColumnId.value(variableDefinition)))); // CR #50
                } else {
                    fields.add(field(name(variableDefinition.getAlias())));
                }
            }
        }

        return fields;
    }

    private SelectQuery<Record> selectDistinct(SelectQuery<Record> selectQuery) {
        List<Field<?>> fields = selectDistinctFields();
        selectQuery.addDistinctOn(fields);
        selectQuery.addFrom(query);
        return selectQuery;
    }

    private SelectQuery<Record> selectAggregate(SelectQuery<Record> selectQuery) {
        List<Field<?>> fields = new ArrayList<>();
        List<String> skipField = new ArrayList<>();
        Iterator<I_VariableDefinition> iterator = variableDefinitions.iterator();
        boolean distinctRequired = false;

        while (iterator.hasNext()) {
            I_VariableDefinition variableDefinition = iterator.next();
            String alias = getAlias(variableDefinition);
            if (variableDefinition.isFunction()) {
                skipField.add(alias);

                FunctionExpression functionExpression = new FunctionExpression(variableDefinitions, variableDefinition);
                Field<?> field = field(functionExpression.toString());
                skipField.addAll(functionExpression.arguments());

                if (variableDefinition.getAlias() != null) {
                    field = field.as(alias);
                }

                fields.add(field);
            } else if (variableDefinition.isExtension()) {
                log.debug("Not yet implemented");
            } else {
                // check if this alias is serviced by a function
                // check if this alias requires distinct
                distinctRequired = distinctRequired || variableDefinition.isDistinct();

                if (skipField.contains(alias)) {
                    continue;
                }

                if (variableDefinition.isDistinct()) {
                    fields.add(field(name("DISTINCT " + alias)));
                } else {
                    fields.add(field(name(alias)));
                }
            }
        }

        selectQuery.addSelect(fields);
        selectQuery.addFrom(query);

        // aggregate return scalar values
        this.outputWithJson = false;

        return selectQuery;
    }

    public SelectQuery<Record> selectOrderBy(List<OrderAttribute> orderAttributes) {
        SelectQuery<Record> selectQuery = context.selectQuery();
        selectQuery.addFrom(query);
        selectQuery = setOrderBy(orderAttributes, selectQuery);
        return selectQuery;
    }

    public SelectQuery<Record> setOrderBy(List<OrderAttribute> orderAttributes, SelectQuery<Record> selectQuery) {
        return new OrderByBinder(variableDefinitions, orderAttributes, selectQuery).bind();
    }

    public SelectQuery<Record> select() {
        SelectQuery<Record> selectQuery = context.selectQuery();

        if (new Variables(variableDefinitions).hasDefinedFunction()) {
            return selectAggregate(selectQuery);
        } else if (new Variables(variableDefinitions).hasDefinedDistinct()) { // build a super select
            return selectDistinct(selectQuery);
        } else {
            return selectQuery;
        }
    }

    public boolean isOutputWithJson() {
        return outputWithJson;
    }

    private String getAlias(I_VariableDefinition variableDefinition) {
        if (StringUtils.isEmpty(variableDefinition.getAlias())) {
            return variableDefinition.getPath();
        }
        return variableDefinition.getAlias();
    }
}
