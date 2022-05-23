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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.aql.compiler.OrderAttribute;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.jooq.SortField;
import org.jooq.impl.DSL;

/**
 * Created by christian on 9/23/2016.
 */
@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class OrderByBinder {

    private List<OrderAttribute> orderAttributes;
    private final SelectQuery<Record> select;
    private final VariableDefinitions variableDefinitions;

    OrderByBinder(
            VariableDefinitions variableDefinitions, List<OrderAttribute> orderAttributes, SelectQuery<Record> select) {
        this.orderAttributes = orderAttributes;
        this.select = select;
        this.variableDefinitions = variableDefinitions;
    }

    List<SortField<Object>> getOrderByFields() {
        if (orderAttributes.isEmpty()) return Collections.emptyList();

        List<SortField<Object>> orderFields = new ArrayList<>();

        for (OrderAttribute orderAttribute : orderAttributes) {
            SortField<Object> field;
            String fieldIdentifier = null;

            // get the corresponding variable definition
            if (variableDefinitions != null) {
                Iterator<I_VariableDefinition> localIterator = variableDefinitions.iterator();

                while (localIterator.hasNext()) {
                    I_VariableDefinition variableDefinition = localIterator.next();
                    if (orderAttribute.getVariableDefinition().getPath() != null
                            && orderAttribute.getVariableDefinition().getPath().equals(variableDefinition.getPath())) {
                        if (variableDefinition.getAlias() != null) fieldIdentifier = variableDefinition.getAlias();
                        else
                            fieldIdentifier =
                                    "/" + orderAttribute.getVariableDefinition().getPath();
                        break;
                    } else if (orderAttribute.getVariableDefinition().getAlias() != null)
                        fieldIdentifier = orderAttribute.getVariableDefinition().getAlias();
                }
            }

            if (fieldIdentifier == null) { // hidden field
                fieldIdentifier = orderAttribute.getVariableDefinition().getAlias() == null
                        ? "/" + orderAttribute.getVariableDefinition().getPath()
                        : orderAttribute.getVariableDefinition().getAlias();
                orderAttribute.getVariableDefinition().setHidden(true);
            }

            if (!fieldIdentifier.startsWith("\""))
                fieldIdentifier = "\"" + fieldIdentifier + "\""; // by postgresql convention

            if (orderAttribute.getDirection() != null
                    && orderAttribute.getDirection().equals(OrderAttribute.OrderDirection.DESC)) {
                field = DSL.field(fieldIdentifier).desc();
            } else // default to ASCENDING
            field = DSL.field(fieldIdentifier).asc();

            orderFields.add(field);
        }
        return orderFields;
    }

    public SelectQuery<Record> bind() {

        if (CollectionUtils.isNotEmpty(orderAttributes)) {
            select.addOrderBy(getOrderByFields());
        }
        return select;
    }
}
