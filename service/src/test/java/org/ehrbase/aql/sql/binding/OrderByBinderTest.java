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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.ehrbase.aql.compiler.OrderAttribute;
import org.ehrbase.aql.definition.I_VariableDefinitionHelper;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.jooq.SortField;
import org.junit.Test;

public class OrderByBinderTest {

    @Test
    public void getOrderByFields() {

        // ascending
        {
            // represents a/context/start_time/value as date_created and order by date_created ASC
            OrderAttribute orderAttribute = new OrderAttribute(
                    I_VariableDefinitionHelper.build(null, "date_created", null, false, false, false));
            orderAttribute.setDirection(OrderAttribute.OrderDirection.ASC);

            OrderByBinder cut = new OrderByBinder(
                    null,
                    Collections.singletonList(orderAttribute),
                    DSLContextHelper.buildContext().selectQuery());
            List<SortField<Object>> actual = cut.getOrderByFields();

            assertThat(actual).size().isEqualTo(1);
            SortField<?> sortField = actual.get(0);
            assertThat(sortField.toString()).isEqualTo("\"date_created\" asc");
        }

        // descending
        {
            // represents a/context/start_time/value as date_created and order by date_created DESC
            OrderAttribute orderAttribute = new OrderAttribute(
                    I_VariableDefinitionHelper.build(null, "date_created", null, false, false, false));
            orderAttribute.setDirection(OrderAttribute.OrderDirection.DESC);

            OrderByBinder cut = new OrderByBinder(
                    null,
                    Collections.singletonList(orderAttribute),
                    DSLContextHelper.buildContext().selectQuery());
            List<SortField<Object>> actual = cut.getOrderByFields();

            assertThat(actual).size().isEqualTo(1);
            SortField<?> sortField = actual.get(0);
            assertThat(sortField.toString()).isEqualTo("\"date_created\" desc");
        }

        // no direction
        {
            // represents a/context/start_time/value as date_created and order by date_created
            OrderAttribute orderAttribute = new OrderAttribute(
                    I_VariableDefinitionHelper.build(null, "date_created", null, false, false, false));

            OrderByBinder cut = new OrderByBinder(
                    null,
                    Collections.singletonList(orderAttribute),
                    DSLContextHelper.buildContext().selectQuery());
            List<SortField<Object>> actual = cut.getOrderByFields();

            assertThat(actual).size().isEqualTo(1);
            SortField<?> sortField = actual.get(0);
            assertThat(sortField.toString()).isEqualTo("\"date_created\" asc");
        }
    }
}
