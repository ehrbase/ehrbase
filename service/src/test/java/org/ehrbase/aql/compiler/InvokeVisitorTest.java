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
package org.ehrbase.aql.compiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.I_VariableDefinitionHelper;
import org.ehrbase.openehr.sdk.util.functional.Try;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.junit.Test;

public class InvokeVisitorTest {

    @Test
    public void shouldVisitInvokeExpressionExpandOperation() {
        // postman request for expansion is: GET
        // https://r4.ontoserver.csiro.au/fhir/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/surface
        ExternalTerminologyValidation mock = mock(ExternalTerminologyValidation.class);
        when(mock.expand(any()))
                .thenReturn(List.of(
                        new DvCodedText("Occlusal", new CodePhrase("O")),
                        new DvCodedText("Mesial", new CodePhrase("M")),
                        new DvCodedText("Distoclusal", new CodePhrase("DO")),
                        new DvCodedText("Lingual", new CodePhrase("L")),
                        new DvCodedText("Incisal", new CodePhrase("I")),
                        new DvCodedText("Ventral", new CodePhrase("V")),
                        new DvCodedText("Mesioclusodistal", new CodePhrase("MOD")),
                        new DvCodedText("Mesioclusal", new CodePhrase("MO")),
                        new DvCodedText("Distal", new CodePhrase("D")),
                        new DvCodedText("Distoincisal", new CodePhrase("DI")),
                        new DvCodedText("Buccal", new CodePhrase("B"))));

        WhereVisitor cut = new WhereVisitor(mock);
        String aql = "SELECT o/data[at0002]/events[at0003] AS systolic " + "FROM EHR [ehr_id/value='1234'] "
                + "CONTAINS COMPOSITION c "
                + "CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.blood_pressure.v1] "
                + "WHERE c/archetype_details/template_id/value matches {'Flormidal', TERMINOLOGY('expand','http://hl7.org/fhir/4.0','url=http://hl7.org/fhir/ValueSet/surface'), 'Kloralhidrat'}";
        ParseTree tree = QueryHelper.setupParseTree(aql);
        cut.visit(tree);

        List<Object> whereExpression = cut.getWhereExpression();
        assertThat(whereExpression).size().isEqualTo(29);

        I_VariableDefinition where1 = (I_VariableDefinition) whereExpression.get(0);
        I_VariableDefinition expected1 =
                I_VariableDefinitionHelper.build("archetype_details/template_id/value", null, "c", false, false, false);
        I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(where1, expected1);

        assertThat(whereExpression.get(1)).isEqualTo(" IN ");

        assertThat(whereExpression.get(2)).isEqualTo("(");

        assertThat(whereExpression.get(3)).isEqualTo("'Flormidal'");

        assertThat(whereExpression.get(4)).isEqualTo(",");

        assertThat(whereExpression.get(5)).isEqualTo("'O'");

        assertThat(whereExpression.get(6)).isEqualTo(",");

        assertThat(whereExpression.get(7)).isEqualTo("'M'");

        assertThat(whereExpression.get(8)).isEqualTo(",");

        assertThat(whereExpression.get(9)).isEqualTo("'DO'");

        assertThat(whereExpression.get(10)).isEqualTo(",");

        assertThat(whereExpression.get(11)).isEqualTo("'L'");

        assertThat(whereExpression.get(12)).isEqualTo(",");

        assertThat(whereExpression.get(13)).isEqualTo("'I'");

        assertThat(whereExpression.get(14)).isEqualTo(",");

        assertThat(whereExpression.get(15)).isEqualTo("'V'");

        assertThat(whereExpression.get(16)).isEqualTo(",");

        assertThat(whereExpression.get(17)).isEqualTo("'MOD'");

        assertThat(whereExpression.get(18)).isEqualTo(",");

        assertThat(whereExpression.get(19)).isEqualTo("'MO'");

        assertThat(whereExpression.get(20)).isEqualTo(",");

        assertThat(whereExpression.get(21)).isEqualTo("'D'");

        assertThat(whereExpression.get(22)).isEqualTo(",");

        assertThat(whereExpression.get(23)).isEqualTo("'DI'");

        assertThat(whereExpression.get(24)).isEqualTo(",");

        assertThat(whereExpression.get(25)).isEqualTo("'B'");

        assertThat(whereExpression.get(26)).isEqualTo(",");

        assertThat(whereExpression.get(27)).isEqualTo("'Kloralhidrat'");

        assertThat(whereExpression.get(28)).isEqualTo(")");
    }

    @Test
    public void shouldVisitInvokeExpressionValidateOperation() {
        // postman request for expansion is: GET
        // https://r4.ontoserver.csiro.au/fhir/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/surface
        ExternalTerminologyValidation mock = mock(ExternalTerminologyValidation.class);
        when(mock.validate(any())).thenReturn(Try.success(Boolean.TRUE));

        WhereVisitor cut = new WhereVisitor(mock);
        String aql = "SELECT o/data[at0002]/events[at0003] AS systolic " + "FROM EHR [ehr_id/value='1234'] "
                + "CONTAINS COMPOSITION c "
                + "CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.blood_pressure.v1] "
                + "WHERE c/archetype_details/template_id/value MATCHES {'Flormidal', TERMINOLOGY('validate','http://hl7.org/fhir/4.0','system=http://snomed.info/sct&code=122298005&url=http://snomed.info/sct?fhir_vs&display=Astrovirus RNA assay'), 'Kloralhidrat'}";
        ParseTree tree = QueryHelper.setupParseTree(aql);
        cut.visit(tree);

        List<Object> whereExpression = cut.getWhereExpression();
        assertThat(whereExpression).size().isEqualTo(9);

        I_VariableDefinition where1 = (I_VariableDefinition) whereExpression.get(0);
        I_VariableDefinition expected1 =
                I_VariableDefinitionHelper.build("archetype_details/template_id/value", null, "c", false, false, false);
        I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(where1, expected1);

        assertThat(whereExpression.get(1)).isEqualTo(" IN ");

        assertThat(whereExpression.get(2)).isEqualTo("(");

        assertThat(whereExpression.get(3)).isEqualTo("'Flormidal'");

        assertThat(whereExpression.get(4)).isEqualTo(",");

        assertThat(whereExpression.get(5)).isEqualTo(true);

        assertThat(whereExpression.get(6)).isEqualTo(",");

        assertThat(whereExpression.get(7)).isEqualTo("'Kloralhidrat'");

        assertThat(whereExpression.get(8)).isEqualTo(")");
    }
}
