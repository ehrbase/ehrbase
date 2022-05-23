/*
 * Copyright (c) 2019-2022 vitasystems GmbH and Hannover Medical School.
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.jooq.DSLContext;
import org.jooq.SelectQuery;
import org.junit.Test;

/**
 * @author Stefan Spiska
 * @since 1.0
 */
public class LimitBindingTest {

    @Test
    public void testBind() {
        DSLContext context = DSLContextHelper.buildContext();

        // no offset and limit
        {
            LimitBinding cut = new LimitBinding(null, null, context.selectQuery());
            SelectQuery<?> actual = cut.bind();
            assertThat(actual.toString()).hasToString("select 1");
        }

        // only  limit
        {
            LimitBinding cut = new LimitBinding(1, null, context.selectQuery());
            SelectQuery<?> actual = cut.bind();
            assertThat(actual.toString()).hasToString("select 1\n" + "offset 0 rows\n" + "fetch next 1 rows only");
        }

        // only offset
        {
            LimitBinding cut = new LimitBinding(null, 1, context.selectQuery());
            SelectQuery<?> actual = cut.bind();
            assertThat(actual.toString()).hasToString("select 1\n" + "offset 1 rows\n" + "fetch next 0 rows only");
        }

        // offset and limit
        {
            LimitBinding cut = new LimitBinding(1, 1, context.selectQuery());
            SelectQuery<?> actual = cut.bind();
            assertThat(actual.toString()).hasToString("select 1\n" + "offset 1 rows\n" + "fetch next 1 rows only");
        }
    }
}
