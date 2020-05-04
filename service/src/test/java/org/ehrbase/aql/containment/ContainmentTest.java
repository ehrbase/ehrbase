/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
 *
 * This file is part of project EHRbase
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

package org.ehrbase.aql.containment;

import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.compiler.Statements;
import org.ehrbase.aql.sql.binding.ContainBinder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ContainmentTest {


    @Test
    public void setArchetypeId() {
        Containment cut = new Containment(null);

        cut.setArchetypeId(null);
        assertThat(cut.getArchetypeId()).isNull();

        cut.setArchetypeId("[openEHR-EHR-OBSERVATION.laboratory-hba1c.v1]");
        assertThat(cut.getArchetypeId()).isEqualTo("openEHR-EHR-OBSERVATION.laboratory-hba1c.v1");

        cut.setArchetypeId("openEHR-EHR-OBSERVATION.laboratory-hba1c.v1");
        assertThat(cut.getArchetypeId()).isEqualTo("openEHR-EHR-OBSERVATION.laboratory-hba1c.v1");
    }


    public static Containment buildContainment(Containment enclosingContainment, String symbol, String archetypeId, String composition, String path) {
        Containment expected = new Containment(enclosingContainment);
        expected.setSymbol(symbol);
        expected.setArchetypeId(archetypeId);
        expected.setClassName(composition);
        expected.setPath(path);
        return expected;
    }

    public static void checkEqual(Containment containment, Containment expected) {
        assertThat(containment.getSymbol()).isEqualTo(expected.getSymbol());
        assertThat(containment.getArchetypeId()).isEqualTo(expected.getArchetypeId());
        assertThat(containment.getClassName()).isEqualTo(expected.getClassName());
        assertThat(containment.getPath()).isEqualTo(expected.getPath());
        assertThat(containment.getEnclosingContainment()).isEqualTo(expected.getEnclosingContainment());
    }

    @Test
    public void testInterpretContainement(){

        String query = "select\n" +
                "c\n" +
                "from EHR e\n" +
                "contains COMPOSITION c[openEHR-EHR-COMPOSITION.report-result.v1]\n" +
                "contains (" +
                "CLUSTER f[openEHR-EHR-CLUSTER.case_identification.v0] and\n" +
                "CLUSTER z[openEHR-EHR-CLUSTER.specimen.v1] and\n" +
                "CLUSTER j[openEHR-EHR-CLUSTER.laboratory_test_panel.v0]\n" +
                "contains CLUSTER g[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1])\n" +
                "where\n" +
                "c/name/value='Virologischer Befund' and\n" +
                "g/items[at0001]/name='Nachweis' and\n" +
                "g/items[at0024]/name='Virus'\n";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree()).process();
//        Statements statements = new Statements(aqlExpression.getParseTree(), contains.getIdentifierMapper()).process() ;

        String containSQL = new ContainBinder(contains.getNestedSets()).bind();

        assertThat(containSQL).isEqualToIgnoringNewLines("SELECT DISTINCT comp_id FROM ehr.containment WHERE label ~'openEHR_EHR_COMPOSITION_report_result_v1.*.openEHR_EHR_CLUSTER_laboratory_test_panel_v0.*.openEHR_EHR_CLUSTER_laboratory_test_analyte_v1' \n" +
                "INTERSECT \n" +
                "SELECT DISTINCT comp_id FROM ehr.containment WHERE label ~'openEHR_EHR_COMPOSITION_report_result_v1.*.openEHR_EHR_CLUSTER_case_identification_v0' \n" +
                "INTERSECT \n" +
                "SELECT DISTINCT comp_id FROM ehr.containment WHERE label ~'openEHR_EHR_COMPOSITION_report_result_v1.*.openEHR_EHR_CLUSTER_specimen_v1'");
    }

//    @Test
//    public void testInterpretContainement2(){
//
//        String query = "SELECT q as ErregerBEZK\n" +
//                "from EHR e\n" +
//                "contains COMPOSITION c\n" +
//                "contains OBSERVATION v[openEHR-EHR-OBSERVATION.laboratory_test_result.v1]\n" +
//                "contains ( (\n" +
//                "CLUSTER h[openEHR-EHR-CLUSTER.laboratory_test_panel.v0] and\n" +
//                "CLUSTER x[openEHR-EHR-CLUSTER.specimen.v0]) and\n" +
//                "CLUSTER q[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1])";
//
//        AqlExpression aqlExpression = new AqlExpression().parse(query);
//        Contains contains = new Contains(aqlExpression.getParseTree()).process();
////        Statements statements = new Statements(aqlExpression.getParseTree(), contains.getIdentifierMapper()).process() ;
//
//        String containSQL = new ContainBinder(contains.getNestedSets()).bind();
//
//        assertThat(containSQL).isEqualToIgnoringNewLines("SELECT DISTINCT comp_id FROM ehr.containment WHERE label ~'openEHR_EHR_COMPOSITION_report_result_v1.*.openEHR_EHR_CLUSTER_laboratory_test_panel_v0.*.openEHR_EHR_CLUSTER_laboratory_test_analyte_v1' \n" +
//                "INTERSECT \n" +
//                "SELECT DISTINCT comp_id FROM ehr.containment WHERE label ~'openEHR_EHR_COMPOSITION_report_result_v1.*.openEHR_EHR_CLUSTER_case_identification_v0' \n" +
//                "INTERSECT \n" +
//                "SELECT DISTINCT comp_id FROM ehr.containment WHERE label ~'openEHR_EHR_COMPOSITION_report_result_v1.*.openEHR_EHR_CLUSTER_specimen_v1'");
//    }

}