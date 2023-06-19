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

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.I_VariableDefinitionHelper;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;
import org.junit.Assert;
import org.junit.Test;

public class WhereVisitorTest {

    @Test
    public void getWhereExpression() {

        {
            WhereVisitor cut = new WhereVisitor(mock(ExternalTerminologyValidation.class));
            String aql =
                    "select a  from EHR  [ehr_id/value = '26332710-16f3-4b54-aae9-4d11c141388c'] contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]"
                            + "where a/composer/name='Tony Stark'";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            cut.visit(tree);

            List<Object> whereExpression = cut.getWhereExpression();
            assertThat(whereExpression).size().isEqualTo(3);

            I_VariableDefinition where1 = (I_VariableDefinition) whereExpression.get(0);
            I_VariableDefinition expected =
                    I_VariableDefinitionHelper.build("composer/name", null, "a", false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(where1, expected);

            assertThat(whereExpression.get(1)).isEqualTo("=");

            assertThat(whereExpression.get(2)).isEqualTo("'Tony Stark'");
        }

        {
            WhereVisitor cut = new WhereVisitor(mock(ExternalTerminologyValidation.class));
            String aql = "SELECT o/data[at0002]/events[at0003] AS systolic " + "FROM EHR [ehr_id/value='1234'] "
                    + "CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.encounter.v1] "
                    + "CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.blood_pressure.v1] "
                    + "WHERE o/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/value > 140";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            cut.visit(tree);

            List<Object> whereExpression = cut.getWhereExpression();
            assertThat(whereExpression).size().isEqualTo(3);

            I_VariableDefinition where1 = (I_VariableDefinition) whereExpression.get(0);
            I_VariableDefinition expected = I_VariableDefinitionHelper.build(
                    "data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/value",
                    null,
                    "o",
                    false,
                    false,
                    false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(where1, expected);

            assertThat(whereExpression.get(1)).isEqualTo(">");

            assertThat(whereExpression.get(2)).isEqualTo("140");
        }

        {
            WhereVisitor cut = new WhereVisitor(mock(ExternalTerminologyValidation.class));
            String aql = "SELECT o/data[at0002]/events[at0003] AS systolic " + "FROM EHR [ehr_id/value='1234'] "
                    + "CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.encounter.v1] "
                    + "CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.blood_pressure.v1] "
                    + "WHERE o/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/value > "
                    + "c/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/value";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            cut.visit(tree);

            List<Object> whereExpression = cut.getWhereExpression();
            assertThat(whereExpression).size().isEqualTo(3);

            I_VariableDefinition where1 = (I_VariableDefinition) whereExpression.get(0);
            I_VariableDefinition expected1 = I_VariableDefinitionHelper.build(
                    "data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/value",
                    null,
                    "o",
                    false,
                    false,
                    false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(where1, expected1);

            assertThat(whereExpression.get(1)).isEqualTo(">");

            I_VariableDefinition where2 = (I_VariableDefinition) whereExpression.get(2);
            I_VariableDefinition expected2 = I_VariableDefinitionHelper.build(
                    "data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/value",
                    null,
                    "c",
                    false,
                    false,
                    false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(where2, expected2);
        }

        {
            WhereVisitor cut = new WhereVisitor(mock(ExternalTerminologyValidation.class));
            String aql =
                    "select a  from EHR  [ehr_id/value = '26332710-16f3-4b54-aae9-4d11c141388c'] contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]"
                            + "where a/composer/name='Tony Stark' and a/name/value='Immunisation summary'";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            cut.visit(tree);

            List<Object> whereExpression = cut.getWhereExpression();
            assertThat(whereExpression).size().isEqualTo(7);

            I_VariableDefinition where1 = (I_VariableDefinition) whereExpression.get(0);
            I_VariableDefinition expected1 =
                    I_VariableDefinitionHelper.build("composer/name", null, "a", false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(where1, expected1);

            assertThat(whereExpression.get(1)).isEqualTo("=");

            assertThat(whereExpression.get(2)).isEqualTo("'Tony Stark'");

            assertThat(whereExpression.get(3)).isEqualTo("and");

            I_VariableDefinition where5 = (I_VariableDefinition) whereExpression.get(4);
            I_VariableDefinition expected5 =
                    I_VariableDefinitionHelper.build("name/value", null, "a", false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(where5, expected5);

            assertThat(whereExpression.get(5)).isEqualTo("=");

            assertThat(whereExpression.get(6)).isEqualTo("'Immunisation summary'");
        }

        {
            WhereVisitor cut = new WhereVisitor(mock(ExternalTerminologyValidation.class));
            String aql = "SELECT o/data[at0002]/events[at0003] AS systolic " + "FROM EHR [ehr_id/value='1234'] "
                    + "CONTAINS COMPOSITION c "
                    + "CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.blood_pressure.v1] "
                    + "WHERE c/archetype_details/template_id/value matches {'iEHR - Healthlink - Referral.v0','iEHR - Healthlink - Discharge Sumary.v0'} ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            cut.visit(tree);

            List<Object> whereExpression = cut.getWhereExpression();
            assertThat(whereExpression).size().isEqualTo(7);

            I_VariableDefinition where1 = (I_VariableDefinition) whereExpression.get(0);
            I_VariableDefinition expected1 = I_VariableDefinitionHelper.build(
                    "archetype_details/template_id/value", null, "c", false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(where1, expected1);

            assertThat(whereExpression.get(1)).isEqualTo(" IN ");

            assertThat(whereExpression.get(2)).isEqualTo("(");

            assertThat(whereExpression.get(3)).isEqualTo("'iEHR - Healthlink - Referral.v0'");

            assertThat(whereExpression.get(4)).isEqualTo(",");

            assertThat(whereExpression.get(5)).isEqualTo("'iEHR - Healthlink - Discharge Sumary.v0'");

            assertThat(whereExpression.get(6)).isEqualTo(")");
        }
    }

    @Test
    public void testStructuredAST1() {
        WhereVisitor cut = new WhereVisitor(mock(ExternalTerminologyValidation.class));
        String aql = "SELECT e " + "FROM EHR e[ehr_id/value='1234'] "
                + "WHERE ((e/ehr_id/value = '1111' AND e/ehr_id/value = '2222') OR (e/ehr_id/value = '333' OR e/ehr_id/value = '444')) ";
        ParseTree tree = QueryHelper.setupParseTree(aql);
        cut.visit(tree);

        List<Object> whereExpression = cut.getWhereExpression();
        assertThat(whereExpression).size().isEqualTo(21);

        assertEquals(
                whereExpression.toString(),
                "[(, (, e::ehr_id/value, =, '1111', AND, e::ehr_id/value, =, '2222', ), OR, (, e::ehr_id/value, =, '333', OR, e::ehr_id/value, =, '444', ), )]");
    }

    @Test
    public void testEmptyWhereStatement() {
        WhereVisitor cut = new WhereVisitor(mock(ExternalTerminologyValidation.class));
        String aql = "select e/ehr_id/value from EHR e";
        ParseTree tree = QueryHelper.setupParseTree(aql);
        cut.visit(tree);

        List<Object> whereExpression = cut.getWhereExpression();
        assertThat(whereExpression).size().isEqualTo(0);
    }

    @Test
    public void testEXISTS() {
        WhereVisitor cut = new WhereVisitor(mock(ExternalTerminologyValidation.class));
        String aql = "select c\n" + "from EHR e\n"
                + "contains COMPOSITION c\n"
                + "WHERE EXISTS c/content[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0]";

        ParseTree tree = QueryHelper.setupParseTree(aql);
        cut.visit(tree);

        List<Object> whereExpression = cut.getWhereExpression();
        assertThat(whereExpression).size().isEqualTo(2);

        //        assertEquals(whereExpression.toString(), "[(, (, e::ehr_id/value, =, '1111', AND, e::ehr_id/value, =,
        // '2222', ), OR, (, e::ehr_id/value, =, '333', OR, e::ehr_id/value, =, '444', ), )]");
    }

    @Test
    public void testTerminologyWhereStatement() {
        ExternalTerminologyValidation mock = mock(ExternalTerminologyValidation.class);
        TerminologyId terminologyId = new TerminologyId("http://fhir.de/CodeSystem/dimdi/atc");

        List<DvCodedText> result = new ArrayList<>();
        result.add(new DvCodedText("Heparingruppe", new CodePhrase(terminologyId, "B01AB")));
        result.add(new DvCodedText("Heparin", new CodePhrase(terminologyId, "B01AB01")));
        result.add(new DvCodedText("Antithrombin III, Antithrombin alfa", new CodePhrase(terminologyId, "B01AB02")));
        result.add(new DvCodedText("Dalteparin", new CodePhrase(terminologyId, "B01AB04")));
        result.add(new DvCodedText("Nadroparin", new CodePhrase(terminologyId, "B01AB06")));

        TerminologyParam tp = TerminologyParam.ofServiceApi("hl7.org/fhir/R4");
        tp.setParameter("https: //www.netzwerk-universitaetsmedizin.de/fhir/ValueSet/anticoagulants-atc");
        tp.setOperation("expand");

        when(mock.expand(tp)).thenReturn(result);

        WhereVisitor cut = new WhereVisitor(mock);
        String aql = "select  a_a/data[at0002]/items[at0022] " + "from EHR e "
                + "contains COMPOSITION a "
                + "contains EVALUATION a_a[openEHR-EHR-EVALUATION.gender.v1] "
                + "WHERE a_a/data[at0002]/items[at0022]/value/defining_code/code_string matches{TERMINOLOGY('expand', 'hl7.org/fhir/R4', 'https: //www.netzwerk-universitaetsmedizin.de/fhir/ValueSet/anticoagulants-atc')}";
        ParseTree tree = QueryHelper.setupParseTree(aql);
        cut.visit(tree);

        List<Object> whereExpression = cut.getWhereExpression();
        assertThat(whereExpression).size().isEqualTo(13);
    }

    @Test
    public void testTerminologyWhereStatementNotSupported() {
        ExternalTerminologyValidation mock = mock(ExternalTerminologyValidation.class);

        TerminologyParam tp = TerminologyParam.ofServiceApi("hl7.org/fhir/R4");
        tp.setParameter("http://hl7.org/fhir/ValueSet/animal-breeds");
        tp.setOperation("expand");

        when(mock.expand(tp))
                .thenThrow(
                        new InternalServerException(
                                "Terminology server operation failed:'Error response received from FHIR terminology server. "
                                        + "HTTP status: 404. Body: {\\\"resourceType\\\":\\\"OperationOutcome\\\",\\\"issue\\\":[{\\\"severity\\\":\\\"error\\\",\\\"code\\\":\\\"not-found\\\",\\\"diagnostics\\\":\\\""
                                        + "[5389b21a-d873-41d0-8c79-4390796c40bc]: Could not find value set http://hl7.org/fhir/ValueSet/animal-breeds. If this is an implicit value set please make sure the url is correct. "
                                        + "Implicit values sets for different code systems are specified in https://www.hl7.org/fhir/terminologies-systems.html .\\\"}]}'"));

        WhereVisitor cut = new WhereVisitor(mock);
        String aql = "select  a_a/data[at0002]/items[at0022] " + "from EHR e "
                + "contains COMPOSITION a "
                + "contains EVALUATION a_a[openEHR-EHR-EVALUATION.gender.v1] "
                + "WHERE a_a/data[at0002]/items[at0022]/value/defining_code/code_string matches{TERMINOLOGY('expand', 'hl7.org/fhir/R4', 'http://hl7.org/fhir/ValueSet/animal-breeds')}";
        ParseTree tree = QueryHelper.setupParseTree(aql);

        Assert.assertThrows(IllegalArgumentException.class, () -> cut.visit(tree));
    }
}
