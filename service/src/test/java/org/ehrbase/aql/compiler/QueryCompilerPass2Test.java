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
package org.ehrbase.aql.compiler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.assertj.core.api.Assertions;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.I_VariableDefinitionHelper;
import org.junit.Ignore;
import org.junit.Test;

public class QueryCompilerPass2Test {

    @Test
    public void getVariables() {
        ParseTreeWalker walker = new ParseTreeWalker();

        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select e/ehr_id/value  " + "from EHR e ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            List<I_VariableDefinition> variables = cut.getVariables();
            assertThat(variables).size().isEqualTo(1);
            I_VariableDefinition variableDefinition = variables.get(0);
            I_VariableDefinition expected =
                    I_VariableDefinitionHelper.build("ehr_id/value", null, "e", false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(variableDefinition, expected);
        }

        // DISTINCT
        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select DISTINCT e/ehr_id/value  " + "from EHR e ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            List<I_VariableDefinition> variables = cut.getVariables();
            assertThat(variables).size().isEqualTo(1);
            I_VariableDefinition variableDefinition = variables.get(0);
            I_VariableDefinition expected =
                    I_VariableDefinitionHelper.build("ehr_id/value", null, "e", true, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(variableDefinition, expected);
        }

        // with alias
        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select e/ehr_id/value as id " + "from EHR e ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            List<I_VariableDefinition> variables = cut.getVariables();
            assertThat(variables).size().isEqualTo(1);
            I_VariableDefinition variableDefinition = variables.get(0);
            I_VariableDefinition expected =
                    I_VariableDefinitionHelper.build("ehr_id/value", "id", "e", false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(variableDefinition, expected);
        }

        // multiple variables
        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql =
                    "select  e/ehr_id/value, a/context/health_care_facility/name/value, d/description[at0001]/items[at0002]/value "
                            + "from EHR e  contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1] contains ACTION d[openEHR-EHR-ACTION.immunisation_procedure.v1]";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            List<I_VariableDefinition> variables = cut.getVariables();
            assertThat(variables).size().isEqualTo(3);

            I_VariableDefinition variableDefinition1 = variables.get(0);
            I_VariableDefinition expected1 =
                    I_VariableDefinitionHelper.build("ehr_id/value", null, "e", false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(variableDefinition1, expected1);

            I_VariableDefinition variableDefinition2 = variables.get(1);
            I_VariableDefinition expected2 = I_VariableDefinitionHelper.build(
                    "context/health_care_facility/name/value", null, "a", false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(variableDefinition1, expected1);

            I_VariableDefinition variableDefinition3 = variables.get(1);
            I_VariableDefinition expected3 = I_VariableDefinitionHelper.build(
                    "description[at0001]/items[at0002]/value", null, "d", false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(variableDefinition1, expected1);
        }
    }

    @Test
    public void getTopAttributes() {
        ParseTreeWalker walker = new ParseTreeWalker();

        // No Top Attribut
        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select e/ehr_id/value " + "from EHR e ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            TopAttributes actual = cut.getTopAttributes();
            assertThat(actual).isNull();
        }

        // Top Attribut without direction
        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select TOP 2 e/ehr_id/value " + "from EHR e ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            TopAttributes actual = cut.getTopAttributes();
            assertThat(actual).isNotNull();
            assertThat(actual.getWindow()).isEqualTo(2);
            assertThat(actual.getDirection()).isNull();
        }

        // Top Attribut with direction BACKWARD
        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select TOP 2 BACKWARD e/ehr_id/value " + "from EHR e ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            TopAttributes actual = cut.getTopAttributes();
            assertThat(actual).isNotNull();
            assertThat(actual.getWindow()).isEqualTo(2);
            assertThat(actual.getDirection()).isEqualTo(TopAttributes.TopDirection.BACKWARD);
        }

        // Top Attribut with direction FORWARD
        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select TOP 2 FORWARD e/ehr_id/value " + "from EHR e ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            TopAttributes actual = cut.getTopAttributes();
            assertThat(actual).isNotNull();
            assertThat(actual.getWindow()).isEqualTo(2);
            assertThat(actual.getDirection()).isEqualTo(TopAttributes.TopDirection.FORWARD);
        }
    }

    @Test
    public void getOrderAttributes() {

        ParseTreeWalker walker = new ParseTreeWalker();

        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select a/uid/value as uid, a/composer/name as author, a/context/start_time/value  "
                    + "from EHR e  contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1] "
                    + "order by a/context/start_time/value DESC";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);

            List<OrderAttribute> orderAttributes = cut.getOrderAttributes();
            Assertions.assertThat(orderAttributes).size().isEqualTo(1);

            OrderAttribute orderAttribute = orderAttributes.get(0);
            assertThat(orderAttribute.getDirection()).isEqualTo(OrderAttribute.OrderDirection.DESC);
            I_VariableDefinition expected =
                    I_VariableDefinitionHelper.build("context/start_time/value", null, "a", false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(
                    orderAttribute.getVariableDefinition(), expected);
        }

        // with alias
        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql =
                    "select a/uid/value as uid, a/composer/name as author, a/context/start_time/value as date_created "
                            + "from EHR e  contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1] "
                            + "order by date_created ASC";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);

            List<OrderAttribute> orderAttributes = cut.getOrderAttributes();
            Assertions.assertThat(orderAttributes).size().isEqualTo(1);

            OrderAttribute orderAttribute = orderAttributes.get(0);
            assertThat(orderAttribute.getDirection()).isEqualTo(OrderAttribute.OrderDirection.ASC);
            I_VariableDefinition expected =
                    I_VariableDefinitionHelper.build(null, "date_created", null, false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(
                    orderAttribute.getVariableDefinition(), expected);
        }

        // with default direction
        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql =
                    "select a/uid/value as uid, a/composer/name as author, a/context/start_time/value as date_created "
                            + "from EHR e  contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1] "
                            + "order by date_created";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);

            List<OrderAttribute> orderAttributes = cut.getOrderAttributes();
            Assertions.assertThat(orderAttributes).size().isEqualTo(1);

            OrderAttribute orderAttribute = orderAttributes.get(0);
            assertThat(orderAttribute.getDirection()).isEqualTo(OrderAttribute.OrderDirection.ASC);
            I_VariableDefinition expected =
                    I_VariableDefinitionHelper.build(null, "date_created", null, false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(
                    orderAttribute.getVariableDefinition(), expected);
        }
    }

    @Test
    public void getLimitAttribute() {

        ParseTreeWalker walker = new ParseTreeWalker();

        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select e/ehr_id/value " + "from EHR e ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            Integer actual = cut.getLimitAttribute();
            assertThat(actual).isNull();
        }

        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select e/ehr_id/value " + "from EHR e LIMIT 6 ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            Integer actual = cut.getLimitAttribute();
            assertThat(actual).isEqualTo(6);
        }
    }

    @Test
    public void getOffsetAttribute() {

        ParseTreeWalker walker = new ParseTreeWalker();

        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select e/ehr_id/value " + "from EHR e ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            Integer actual = cut.getOffsetAttribute();
            assertThat(actual).isNull();
        }

        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select e/ehr_id/value " + "from EHR e LIMIT 5 OFFSET 6 ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);
            Integer actual = cut.getOffsetAttribute();
            assertThat(actual).isEqualTo(6);
        }
    }

    @Test
    public void testFunction1() {

        ParseTreeWalker walker = new ParseTreeWalker();

        {
            QueryCompilerPass2 cut = new QueryCompilerPass2();
            String aql = "select count(a/context/start_time/value)  "
                    + "from EHR e  contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1] ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);

            I_VariableDefinition expected =
                    I_VariableDefinitionHelper.build("context/start_time/value", null, "a", false, false, false);
            assertThat(expected).isNotNull();
        }
    }

    @Test
    @Ignore("in progress")
    public void testCompositionNodeWithPredicate() {
        ParseTreeWalker walker = new ParseTreeWalker();
        QueryCompilerPass2 cut = new QueryCompilerPass2();
        String aql = "SELECT\n" + "  \t  c[name/value = 'Diagnose']/uid/value as Diagnose,\n"
                + "  \t  c[composer/external_ref/id/value = 'Dr Mabuse']/uid/value as MabuseComposition,\n"
                + "  \t  c[context/start_time/value > '2020-01-01']/uid/value as NewerComposition\n"
                + "\tFROM\n"
                + "  \t  EHR e\n"
                + "  \t  contains COMPOSITION c[openEHR-EHR-COMPOSITION.report.v1]";
        ParseTree tree = QueryHelper.setupParseTree(aql);
        walker.walk(cut, tree);
        Integer actual = cut.getOffsetAttribute();
        assertThat(actual).isEqualTo(6);
    }

    @Test
    public void limitAndOrderByAnyOrder() {
        var walker = new ParseTreeWalker();
        var compiler = new QueryCompilerPass2();

        String aql;
        ParseTree tree;

        aql = "select e/ehr_id/value from EHR e LIMIT 5 OFFSET 6";
        tree = QueryHelper.setupParseTree(aql);
        walker.walk(compiler, tree);
        assertThat(compiler.getLimitAttribute()).isEqualTo(5);
        assertThat(compiler.getOffsetAttribute()).isEqualTo(6);
        assertThat(compiler.getOffsetAttribute()).isEqualTo(6);

        aql = "select e/ehr_id/value from EHR e ORDER BY e/ehr_id/value DESC";
        tree = QueryHelper.setupParseTree(aql);
        walker.walk(compiler, tree);
        assertThat(compiler.getOrderAttributes().get(0).getDirection()).isEqualTo(OrderAttribute.OrderDirection.DESC);

        aql = "select e/ehr_id/value from EHR e LIMIT 5 ORDER BY e/ehr_id/value DESC";
        tree = QueryHelper.setupParseTree(aql);
        walker.walk(compiler, tree);
        assertThat(compiler.getLimitAttribute()).isEqualTo(5);
        assertThat(compiler.getOrderAttributes().get(0).getDirection()).isEqualTo(OrderAttribute.OrderDirection.DESC);

        aql = "select e/ehr_id/value from EHR e ORDER BY e/ehr_id/value DESC LIMIT 5";
        tree = QueryHelper.setupParseTree(aql);
        walker.walk(compiler, tree);
        assertThat(compiler.getLimitAttribute()).isEqualTo(5);
        assertThat(compiler.getOrderAttributes().get(0).getDirection()).isEqualTo(OrderAttribute.OrderDirection.DESC);

        aql = "select e/ehr_id/value from EHR e LIMIT 5 OFFSET 6 ORDER BY e/ehr_id/value DESC";
        tree = QueryHelper.setupParseTree(aql);
        walker.walk(compiler, tree);
        assertThat(compiler.getLimitAttribute()).isEqualTo(5);
        assertThat(compiler.getOffsetAttribute()).isEqualTo(6);
        assertThat(compiler.getOrderAttributes().get(0).getDirection()).isEqualTo(OrderAttribute.OrderDirection.DESC);

        aql = "select e/ehr_id/value from EHR e ORDER BY e/ehr_id/value DESC LIMIT 5 OFFSET 6";
        tree = QueryHelper.setupParseTree(aql);
        walker.walk(compiler, tree);
        assertThat(compiler.getLimitAttribute()).isEqualTo(5);
        assertThat(compiler.getOffsetAttribute()).isEqualTo(6);
        assertThat(compiler.getOrderAttributes().get(0).getDirection()).isEqualTo(OrderAttribute.OrderDirection.DESC);
    }
}
