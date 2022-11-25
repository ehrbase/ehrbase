/*
 * Copyright (c) 2020-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.queryimpl;

import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER;
import static org.ehrbase.jooq.pg.Tables.EVENT_CONTEXT;
import static org.jooq.impl.DSL.select;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;
import org.ehrbase.aql.definition.VariableDefinition;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.jooq.pg.Routines;
import org.jooq.Field;
import org.jooq.JSON;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Chevalley
 * @since 1.0
 */
public class FunctionBasedNodePredicateCallTest {

    FieldResolutionContext fieldResolutionContext;

    @Before
    public void setUp() {
        fieldResolutionContext = new FieldResolutionContext(
                DSL.using(SQLDialect.POSTGRES),
                null,
                null,
                new VariableDefinition("test", "test", "test", false),
                IQueryImpl.Clause.SELECT,
                null,
                null,
                null);
    }

    @Test
    public void testConstruct1() {
        String[] pathArray = {
            "'other_context'",
            "'/items[openEHR-EHR-CLUSTER.case_identification.v0]'",
            "'0'",
            "'/items[at0001]'",
            "$AQL_NODE_NAME_PREDICATE$",
            "'Fall-Kennung'",
            "'0'",
            "'/value'",
            "'value'",
        };

        FunctionBasedNodePredicateCall functionBasedNodePredicateCall =
                new FunctionBasedNodePredicateCall(fieldResolutionContext, Arrays.asList(pathArray));
        Function<Field<UUID>, Field<JSON>> function = Routines::jsContext;
        Field<?> test = functionBasedNodePredicateCall.resolve(function, EVENT_CONTEXT.ID);

        assertNotNull(test);
    }

    @Test
    public void testConstruct2() {
        String[] pathArray = {
            "'other_context'",
            "'/items[openEHR-EHR-CLUSTER.case_identification.v0]'",
            "'0'",
            "'/items[at0001]'",
            "$AQL_NODE_NAME_PREDICATE$",
            "'Fall-Kennung'",
            "'0'",
            "'/name'",
            "'0'",
            "'value'",
        };

        FunctionBasedNodePredicateCall functionBasedNodePredicateCall =
                new FunctionBasedNodePredicateCall(fieldResolutionContext, Arrays.asList(pathArray));
        Function<Field<UUID>, Field<JSON>> function = Routines::jsContext;
        Field<?> test = functionBasedNodePredicateCall.resolve(function, EVENT_CONTEXT.ID);

        assertNotNull(test);
    }

    @Test
    public void testConstruct3() {
        String[] pathArray = {
            "'other_context'",
            "'/items[openEHR-EHR-CLUSTER.case_identification.v0]'",
            AQL_NODE_NAME_PREDICATE_MARKER,
            "'item-1'",
            "'/items[at0001]'",
            AQL_NODE_NAME_PREDICATE_MARKER,
            "'item-2'",
            "'/name'",
            "'0'",
            "'value'",
        };

        FunctionBasedNodePredicateCall functionBasedNodePredicateCall =
                new FunctionBasedNodePredicateCall(fieldResolutionContext, Arrays.asList(pathArray));
        Function<Field<UUID>, Field<JSON>> function = Routines::jsContext;
        Field<?> test = functionBasedNodePredicateCall.resolve(function, EVENT_CONTEXT.ID);

        String dummySelect = select(test).toString();

        assertEquals(
                dummySelect,
                "select jsonb_extract_path_text(cast(\"ehr\".\"aql_node_name_predicate\"(\n"
                        + "  cast(jsonb_extract_path_text(\"ehr\".\"aql_node_name_predicate\"(\n"
                        + "  cast(jsonb_extract_path_text(cast(\"ehr\".\"js_context\"(\"ehr\".\"event_context\".\"id\") as jsonb),'other_context','/items[openEHR-EHR-CLUSTER.case_identification.v0]') as jsonb),\n"
                        + "  'item-1',\n"
                        + "  ''\n"
                        + "),'/items[at0001]') as jsonb),\n"
                        + "  'item-2',\n"
                        + "  ''\n"
                        + ") as jsonb),'/name','0','value')");
    }

    @Test
    public void testConstruct4() {
        String[] pathArray = {
            "'other_context'",
            "'/items[openEHR-EHR-CLUSTER.case_identification.v0]'",
            "$AQL_NODE_NAME_PREDICATE$",
            "'node predicate 1'",
            "'0'",
            "'/items[at0001]'",
            "$AQL_NODE_NAME_PREDICATE$",
            "'node predicate 2'",
            "'0'",
            "'/name'",
            "'0'",
            "'value'",
        };

        FunctionBasedNodePredicateCall functionBasedNodePredicateCall =
                new FunctionBasedNodePredicateCall(fieldResolutionContext, Arrays.asList(pathArray));
        Function<Field<UUID>, Field<JSON>> function = Routines::jsContext;
        Field<?> test = functionBasedNodePredicateCall.resolve(function, EVENT_CONTEXT.ID);

        assertNotNull(test);
    }
}
