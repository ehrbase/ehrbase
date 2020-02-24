/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
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
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.VariableDefinition;
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.jooq.SortField;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by christian on 9/23/2016.
 */
public class OrderByBinder {


    private List<OrderAttribute> orderAttributes;
    private final SelectQuery<Record> select;

    OrderByBinder(List<OrderAttribute> orderAttributes, SelectQuery<Record> select) {
        this.orderAttributes = orderAttributes;
        this.select = select;
    }

    List<SortField<Object>> getOrderByFields() {
        if (orderAttributes.isEmpty())
            return Collections.emptyList();

        List<SortField<Object>> orderFields = new ArrayList<>();

        for (OrderAttribute orderAttribute : orderAttributes) {
            SortField<Object> field;
                String fieldIdentifier = orderAttribute.getVariableDefinition().getAlias() == null ?
                        "\"/"+orderAttribute.getVariableDefinition().getPath()+"\"" :
                        orderAttribute.getVariableDefinition().getAlias();

                if (orderAttribute.getDirection() != null && orderAttribute.getDirection().equals(OrderAttribute.OrderDirection.DESC)) {
                    field = DSL.field(fieldIdentifier).desc();
                } else //default to ASCENDING
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
