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
package org.ehrbase.aql.sql.queryimpl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.ehrbase.aql.TestAqlBase;
import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.definition.I_VariableDefinitionHelper;
import org.ehrbase.aql.sql.PathResolver;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.SelectSelectStep;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;

public class JsonbEntryQueryTest extends TestAqlBase {

    JsonbEntryQuery cut;

    @Before
    public void setUp() {

        String query = "select "
                + " c/content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0004, 'Systolic']/value/magnitude"
                + " from EHR e contains composition c";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        PathResolver pathResolver = new PathResolver(knowledge, contains.getIdentifierMapper());
        cut = new JsonbEntryQuery(testDomainAccess, knowledge, pathResolver);
    }

    @Test
    public void testMakeField() throws Exception {
        String query = "select\n" + "a, d\n"
                + "from EHR e\n"
                + "contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]"
                + "  CONTAINS ACTION d[openEHR-EHR-ACTION.immunisation_procedure.v1]";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        PathResolver pathResolver = new PathResolver(knowledge, contains.getIdentifierMapper());

        JsonbEntryQuery cut = new JsonbEntryQuery(testDomainAccess, knowledge, pathResolver);

        // CCH 191016: EHR-163 required trailing '/value' as now the query allows canonical json return
        Field<?> actual = cut.makeField(
                        "IDCR - Immunisation summary.v0",
                        "d",
                        I_VariableDefinitionHelper.build(
                                "description[at0001]/items[at0002]/value/value", "test", "d", false, false, false),
                        IQueryImpl.Clause.SELECT)
                .getQualifiedFieldOrLast(0)
                .getSQLField();

        SelectSelectStep<? extends Record1<?>> selectQuery = DSL.select(actual);
        assertThat(selectQuery.getQuery().toString())
                .isEqualToIgnoringWhitespace(
                        "select (" + QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION
                                + "((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)) \"test\"");

        // NB. the remaining part (#>>'{/description[at0001],/items[at0002],0,/value,value}') is not in the query since
        // this used as a lateral join

        assertThat(actual.toString()).hasToString("\"test\"");
    }

    @Test
    public void testMakeFieldWithNodeNamePredicate1() throws Exception {
        Field<?> actual = cut.makeField(
                        "ehrbase_blood_pressure_simple.de.v0",
                        "c",
                        I_VariableDefinitionHelper.build(
                                "content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1,'Blood pressure (Training sample)']/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude",
                                "test",
                                "c",
                                false,
                                false,
                                false),
                        IQueryImpl.Clause.SELECT)
                .getQualifiedFieldOrLast(0)
                .getSQLField();

        SelectSelectStep<? extends Record1<?>> selectQuery = DSL.select(actual);
        assertThat(selectQuery.getQuery().toString())
                .isEqualToIgnoringWhitespace(
                        "select cast(ehr.aql_node_name_predicate(\"ehr\".\"entry\".\"entry\",'Blood pressure (Training sample)','/composition[openEHR-EHR-COMPOSITION.sample_encounter.v1],/content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]')#>>'{/data[at0001],/events,/events[at0002],0,/data[at0003],/items[at0004],0,/value,magnitude}' as numeric) \"test\"");
        assertThat(actual.toString()).hasToString("\"test\"");
    }

    @Test
    public void testMakeFieldWithNodeNamePredicate2() throws Exception {
        Field<?> actual = cut.makeField(
                        "ehrbase_blood_pressure_simple.de.v0",
                        "c",
                        I_VariableDefinitionHelper.build(
                                "content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0004, 'Systolic']/value/magnitude",
                                "test",
                                "c",
                                false,
                                false,
                                false),
                        IQueryImpl.Clause.SELECT)
                .getQualifiedFieldOrLast(0)
                .getSQLField();

        SelectSelectStep<? extends Record1<?>> selectQuery = DSL.select(actual);
        assertThat(selectQuery.getQuery().toString())
                .isEqualToIgnoringWhitespace(
                        "select cast(ehr.aql_node_name_predicate(\"ehr\".\"entry\".\"entry\", 'Systolic','/composition[openEHR-EHR-COMPOSITION.sample_encounter.v1],/content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1],0,/data[at0001],/events,/events[at0002],0,/data[at0003],/items[at0004]')#>>'{/value,magnitude}' as numeric) \"test\"");
        assertThat(actual.toString()).hasToString("\"test\"");
    }

    @Test
    public void testMakeFieldWithNodeNamePredicate3() throws Exception {
        Field<?> actual = cut.makeField(
                        "ehrbase_blood_pressure_simple.de.v0",
                        "c",
                        I_VariableDefinitionHelper.build(
                                "content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1,'Blood pressure (Training sample)']/data[at0001]/events[at0002]/data[at0003]/items[at0004, 'Systolic']/value/magnitude",
                                "test",
                                "c",
                                false,
                                false,
                                false),
                        IQueryImpl.Clause.SELECT)
                .getQualifiedFieldOrLast(0)
                .getSQLField();

        SelectSelectStep<? extends Record1<?>> selectQuery = DSL.select(actual);
        assertThat(selectQuery.getQuery().toString())
                .isEqualToIgnoringWhitespace(
                        "select cast(ehr.aql_node_name_predicate((ehr.aql_node_name_predicate(\"ehr\".\"entry\".\"entry\",'Blood pressure (Training sample)','/composition[openEHR-EHR-COMPOSITION.sample_encounter.v1],/content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]')#>'{/data[at0001],/events,/events[at0002],0,/data[at0003],/items[at0004]}')::jsonb,'Systolic','')#>>'{/value,magnitude}' as numeric) \"test\"");
        assertThat(actual.toString()).hasToString("\"test\"");
    }

    @Test
    public void testMakeFieldWithNodeNamePredicate4() throws Exception {
        Field<?> actual = cut.makeField(
                        "ehrbase_blood_pressure_simple.de.v0",
                        "c",
                        I_VariableDefinitionHelper.build(
                                "content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1,'Blood pressure (Training sample)']/data[at0001, 'history']/events[at0002]/data[at0003]/items[at0004, 'Systolic']/value/magnitude",
                                "test",
                                "c",
                                false,
                                false,
                                false),
                        IQueryImpl.Clause.SELECT)
                .getQualifiedFieldOrLast(0)
                .getSQLField();

        SelectSelectStep<? extends Record1<?>> selectQuery = DSL.select(actual);
        assertThat(selectQuery.getQuery().toString())
                .isEqualToIgnoringWhitespace(
                        "select cast(ehr.aql_node_name_predicate((ehr.aql_node_name_predicate((ehr.aql_node_name_predicate(\"ehr\".\"entry\".\"entry\",'Blood pressure (Training sample)','/composition[openEHR-EHR-COMPOSITION.sample_encounter.v1],/content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]')#>'{/data[at0001]}')::jsonb,'history','')#>'{/events,/events[at0002],0,/data[at0003],/items[at0004]}')::jsonb,'Systolic','')#>>'{/value,magnitude}' as numeric) \"test\"");
        assertThat(actual.toString()).hasToString("\"test\"");
    }

    @Test
    public void testMakeFieldWithNodeNamePredicate5() throws Exception {
        Field<?> actual = cut.makeField(
                        "ehrbase_blood_pressure_simple.de.v0",
                        "c",
                        I_VariableDefinitionHelper.build(
                                "content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]/data[at0001, 'history']/events[at0002]/data[at0003]/items[at0004]/value/magnitude",
                                "test",
                                "c",
                                false,
                                false,
                                false),
                        IQueryImpl.Clause.SELECT)
                .getQualifiedFieldOrLast(0)
                .getSQLField();

        SelectSelectStep<? extends Record1<?>> selectQuery = DSL.select(actual);
        assertThat(selectQuery.getQuery().toString())
                .isEqualToIgnoringWhitespace(
                        "select cast(ehr.aql_node_name_predicate(\"ehr\".\"entry\".\"entry\", 'history','/composition[openEHR-EHR-COMPOSITION.sample_encounter.v1],/content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1],0,/data[at0001]')#>>'{/events,/events[at0002],0,/data[at0003],/items[at0004],0,/value,magnitude}' as numeric) \"test\"");
        assertThat(actual.toString()).hasToString("\"test\"");
    }

    @Test
    public void testMakeFieldWithNodeNamePredicate6() throws Exception {

        Field<?> actual = cut.makeField(
                        "ehrbase_blood_pressure_simple.de.v0",
                        "c",
                        I_VariableDefinitionHelper.build(
                                "content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]/data[at0001 and name/value='history']/events[at0002]/data[at0003]/items[at0004]/value/magnitude",
                                "test",
                                "c",
                                false,
                                false,
                                false),
                        IQueryImpl.Clause.SELECT)
                .getQualifiedFieldOrLast(0)
                .getSQLField();

        SelectSelectStep<? extends Record1<?>> selectQuery = DSL.select(actual);
        assertThat(selectQuery.getQuery().toString())
                .isEqualToIgnoringWhitespace(
                        "select cast(ehr.aql_node_name_predicate(\"ehr\".\"entry\".\"entry\",'history','/composition[openEHR-EHR-COMPOSITION.sample_encounter.v1],/content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1],0,/data[at0001]')#>>'{/events,/events[at0002],0,/data[at0003],/items[at0004],0,/value,magnitude}' as numeric) \"test\"");
        assertThat(actual.toString()).hasToString("\"test\"");
    }
}
