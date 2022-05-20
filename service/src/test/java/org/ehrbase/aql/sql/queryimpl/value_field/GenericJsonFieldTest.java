/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.queryimpl.value_field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ehrbase.jooq.pg.Tables.*;
import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;
import static org.junit.Assert.assertNotNull;

import org.ehrbase.aql.TestAqlBase;
import org.ehrbase.aql.definition.VariableDefinition;
import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.QueryImplConstants;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class GenericJsonFieldTest extends TestAqlBase {

    FieldResolutionContext fieldResolutionContext;
    JoinSetup joinSetup = new JoinSetup();

    @Before
    public void setUp() {
        fieldResolutionContext = new FieldResolutionContext(
                testDomainAccess.getContext(),
                "test",
                "test",
                new VariableDefinition("test", null, "test", false),
                IQueryImpl.Clause.SELECT,
                null,
                testDomainAccess.getIntrospectService(),
                null);

        JoinSetup joinSetup = new JoinSetup();
    }

    @Test
    public void testField() {
        Field field = new GenericJsonField(fieldResolutionContext, joinSetup)
                .hierObjectId(JoinBinder.ehrRecordTable.field(EHR_.ID));

        assertNotNull(field);
    }

    @Test
    public void testFieldWithPath() {
        String jsonPath = "defining_code/code_string";
        Field field = new GenericJsonField(fieldResolutionContext, joinSetup)
                .forJsonPath(jsonPath)
                .dvCodedText(ENTRY.CATEGORY);

        assertNotNull(field);

        assertThat(DSL.select(field).getQuery().toString())
                .as(jsonPath)
                .isEqualToIgnoringWhitespace("select" + " jsonb_extract_path_text("
                        + "       cast(\"ehr\".\"js_dv_coded_text_inner\"(\"ehr\".\"entry\".\"category\") as jsonb),"
                        + "   'defining_code',"
                        + "  'code_string') \"/test\"");
    }

    @Test
    public void testFieldWithIterativeMarker() {
        String jsonPath = "mappings/" + QueryImplConstants.AQL_NODE_ITERATIVE_MARKER + "/" + "match";
        Field field = new GenericJsonField(fieldResolutionContext, joinSetup)
                .forJsonPath(jsonPath)
                .dvCodedText(ENTRY.CATEGORY);

        assertNotNull(field);

        assertThat(DSL.select(field).getQuery().toString())
                .as(jsonPath)
                .isEqualToIgnoringWhitespace("select" + " jsonb_extract_path_text("
                        + "       cast("
                        + QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION + "("
                        + "           cast(cast(jsonb_extract_path("
                        + "               cast(\"ehr\".\"js_dv_coded_text_inner\"(\"ehr\".\"entry\".\"category\") as jsonb),'mappings')"
                        + "            as jsonb)"
                        + "        as jsonb))"
                        + "    as jsonb),"
                        + "   'match') \"/test\"");
    }

    @Test
    @Ignore("couldn't simulate the right tokenized expression for test!")
    public void testFieldWithMultipleIterativeMarker() {
        String jsonPath = "mappings/" + QueryImplConstants.AQL_NODE_ITERATIVE_MARKER + "items/"
                + QueryImplConstants.AQL_NODE_ITERATIVE_MARKER + "/value";
        Field field = new GenericJsonField(fieldResolutionContext, joinSetup)
                .forJsonPath(jsonPath)
                .dvCodedText(ENTRY.CATEGORY);

        assertNotNull(field);
        assertThat(DSL.select(field).getQuery().toString())
                .as(jsonPath)
                .isEqualToIgnoringWhitespace("select" + " jsonb_extract_path_text("
                        + "   cast("
                        + QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION + "(" + "       cast(jsonb_extract_path_text("
                        + "           cast("
                        + QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION + "(" + "               cast(cast("
                        + "                   jsonb_extract_path("
                        + "                           cast(\"ehr\".\"js_dv_coded_text_inner\"(\"ehr\".\"entry\".\"category\") as jsonb),"
                        + "                           'mappings') as jsonb"
                        + "                    ) as jsonb)"
                        + "                ) as jsonb),"
                        + "               '/items','0')"
                        + "            as jsonb)"
                        + "           ) as jsonb),"
                        + "       'value') \"/test\"");
    }

    @Test
    public void testMultiArguments4() {
        Field field = new GenericJsonField(fieldResolutionContext, joinSetup)
                .partyRef(
                        PARTY_IDENTIFIED.PARTY_REF_NAMESPACE,
                        PARTY_IDENTIFIED.PARTY_REF_TYPE,
                        PARTY_IDENTIFIED.PARTY_REF_SCHEME,
                        PARTY_IDENTIFIED.PARTY_REF_VALUE);

        assertNotNull(field);

        assertThat(DSL.select(field).getQuery().toString())
                .as("multiple arguments")
                .isEqualToIgnoringWhitespace("select cast(\"ehr\".\"js_party_ref\"(\n"
                        + "  \"ehr\".\"party_identified\".\"party_ref_namespace\", \n"
                        + "  \"ehr\".\"party_identified\".\"party_ref_type\", \n"
                        + "  \"ehr\".\"party_identified\".\"party_ref_scheme\", \n"
                        + "  \"ehr\".\"party_identified\".\"party_ref_value\"\n"
                        + ") as varchar) \"/test\"");
    }
}
