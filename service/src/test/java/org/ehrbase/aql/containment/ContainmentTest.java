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
package org.ehrbase.aql.containment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.ehrbase.aql.TestAqlBase;
import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.compiler.Contains;
import org.junit.Test;

public class ContainmentTest extends TestAqlBase {

    @Test
    public void testInterpretContainement() {

        String query = "select\n" + "c\n"
                + "from EHR e\n"
                + "contains COMPOSITION c[openEHR-EHR-COMPOSITION.report-result.v1]\n"
                + "contains ("
                + "CLUSTER f[openEHR-EHR-CLUSTER.case_identification.v0] and\n"
                + "CLUSTER z[openEHR-EHR-CLUSTER.specimen.v1] or\n"
                + "CLUSTER j[openEHR-EHR-CLUSTER.laboratory_test_panel.v0]\n"
                + "contains CLUSTER g[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1])\n"
                + "where\n"
                + "c/name/value='Virologischer Befund' and\n"
                + "g/items[at0001]/name='Nachweis' and\n"
                + "g/items[at0024]/name='Virus'\n";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();
        assertTrue(contains.getTemplates().contains("Virologischer Befund"));
    }

    @Test
    public void testInterpretContainement2() {

        String query = "SELECT q as ErregerBEZK\n" + "from EHR e\n"
                + "contains COMPOSITION c\n"
                + "contains OBSERVATION v[openEHR-EHR-OBSERVATION.laboratory_test_result.v1]\n"
                + "contains ( "
                + "(\n"
                + "CLUSTER h[openEHR-EHR-CLUSTER.laboratory_test_panel.v0] and\n"
                + "CLUSTER x[openEHR-EHR-CLUSTER.specimen.v1])"
                + " or\n"
                + "CLUSTER q[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1]"
                + ")";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();
        assertTrue(contains.getTemplates().contains("Virologischer Befund"));
    }

    @Test
    public void testInterpretContainement3() {

        String query = "    select\n" + "    m/data[at0001]/items[at0004]/value/value as Beginn,\n"
                + "    m/data[at0001]/items[at0005]/value/value as Ende,\n"
                + "    k/items[at0048]/value/defining_code/code_string as Fachabteilung,\n"
                + "    k/items[at0027, 'Stationskennung']/value as StationID\n"
                + "    from EHR e\n"
                + "    contains (\n"
                + "                    CLUSTER f[openEHR-EHR-CLUSTER.case_identification.v0] and\n"
                + "                    CLUSTER z[openEHR-EHR-CLUSTER.specimen.v1] and\n"
                + "                    CLUSTER j[openEHR-EHR-CLUSTER.laboratory_test_panel.v0]\n"
                + "                    contains CLUSTER g[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1])";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();
        assertTrue(contains.getTemplates().contains("Virologischer Befund"));
    }

    @Test
    public void testInterpretContainement_testehr_nested_5_levels_contains() {

        String query = "SELECT c/uid/value, cluster2/items[at0002]/value" + " FROM EHR e[ehr_id/value=$ehr_id]"
                + " CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.nesting.v1]"
                + " CONTAINS SECTION [openEHR-EHR-SECTION.nested.v1]"
                + " CONTAINS ITEM_TREE [openEHR-EHR-ITEM_TREE.nested.v1]"
                + " CONTAINS CLUSTER [openEHR-EHR-CLUSTER.nested.v1]"
                + " CONTAINS CLUSTER cluster2[openEHR-EHR-CLUSTER.nested2.v1]"
                + " WHERE cluster2/items[at0002]/value/magnitude > 5";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();
        assertTrue(contains.requiresTemplateWhereClause());
        assertTrue(contains.getTemplates().contains("nested.en.v1"));
    }

    @Test
    public void testInterpretContainementSingleComposition() {

        String query = "SELECT c/uid/value, cluster2/items[at0002]/value" + " FROM EHR e[ehr_id/value=$ehr_id]"
                + " CONTAINS COMPOSITION"
                + " WHERE cluster2/items[at0002]/value/magnitude > 5";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        assertTrue(contains.requiresTemplateWhereClause());
    }

    @Test
    public void testInterpretContainementRejectNonExistingCompositionNodeId() {

        String query = "SELECT c/uid/value, cluster2/items[at0002]/value" + " FROM EHR e[ehr_id/value=$ehr_id]"
                + " CONTAINS COMPOSITION[openEHR-EHR-COMPOSITION.unknown.v1] "
                + " WHERE cluster2/items[at0002]/value/magnitude > 5";

        AqlExpression aqlExpression = new AqlExpression().parse(query);

        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();
        assertTrue(contains.getTemplates().isEmpty());
    }

    @Test
    public void testInterpretContainementSingleCompositionTraversal() {

        String query = "SELECT c/uid/value,\n"
                + "   o/data[at0001]/events[at0002]/data[at0003]/items[at0078.2]/value/magnitude\n"
                + "   FROM EHR e[ehr_id/value=$ehr_id]\n"
                + "   CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.report-result.v1]\n"
                + "   CONTAINS OBSERVATION o[openEHR-EHR-OBSERVATION.lab_test-blood_glucose.v1]\n"
                + "   WHERE c/uid/value='4ec376b8-fa1a-4349-ab58-31d7f8436f2c::local.ehrbase.org::1' and o/data[at0001]/events[at0002]/data[at0003]/items[at0078.2]/value/magnitude > 200";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        assertTrue(contains.getTemplates().contains("LabResults1"));
    }

    @Test
    public void testFullPathResolution() {

        String query = "SELECT e, o" + " FROM EHR e"
                + " CONTAINS COMPOSITION c"
                + "      CONTAINS ADMIN_ENTRY o[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0]"
                + "           CONTAINS CLUSTER l[openEHR-EHR-CLUSTER.location.v1]";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        assertTrue(contains.getTemplates().contains("Patientenaufenthalt"));

        // check that *both* o and l have resolved path
        assertEquals(
                "/content[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0]",
                ((Containment) contains.getIdentifierMapper().getContainer("o"))
                        .getPath("Patientenaufenthalt")
                        .toArray()[0]);
        assertNotNull(
                "/content[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0]/data[at0001]/items[openEHR-EHR-CLUSTER.location.v1]",
                ((Containment) contains.getIdentifierMapper().getContainer("l"))
                        .getPath("Patientenaufenthalt")
                        .toArray()[0]);
    }

    @Test
    public void testFullPathResolution2() {

        String query = "SELECT e, o" + " FROM EHR e"
                + " CONTAINS COMPOSITION"
                + "      CONTAINS ADMIN_ENTRY"
                + "           CONTAINS CLUSTER l[openEHR-EHR-CLUSTER.location.v1]";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        assertTrue(contains.getTemplates().contains("Patientenaufenthalt"));

        // check that *both* o and l have resolved path
        assertNotNull(
                "/content[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0]/data[at0001]/items[openEHR-EHR-CLUSTER.location.v1]",
                ((Containment) contains.getIdentifierMapper().getContainer("l"))
                        .getPath("Patientenaufenthalt")
                        .toArray()[0]);
    }

    @Test
    public void testFullPathResolution3() {

        String query = "SELECT e, o" + " FROM EHR e"
                + " CONTAINS COMPOSITION"
                + "      CONTAINS ADMIN_ENTRY"
                + "           CONTAINS CLUSTER [openEHR-EHR-CLUSTER.location.v1]";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        assertTrue(contains.getTemplates().contains("Patientenaufenthalt"));
    }

    @Test
    public void testFullPathResolution4() {

        String query = "SELECT e, o" + " FROM EHR e"
                + " CONTAINS COMPOSITION"
                + "      CONTAINS ADMIN_ENTRY"
                + "           CONTAINS OBSERVATION";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        assertTrue(!contains.getTemplates().contains("Patientenaufenthalt"));
    }

    @Test
    public void testContainNotCommutative() {

        // the first query check for composition contains admin_entry contains both cluster a AND cluster n
        String query = "SELECT u" + " FROM EHR e "
                + "   CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.event_summary.v0] "
                + "       CONTAINS "
                + "           (ADMIN_ENTRY u[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0] "
                + "                   CONTAINS CLUSTER a[openEHR-EHR-CLUSTER.location.v1] "
                + "                   and CLUSTER n[openEHR-EHR-CLUSTER.case_identification.v0])";

        Contains contains = new Contains(new AqlExpression().parse(query).getParseTree(), knowledge).process();
        assertTrue(contains.getTemplates().isEmpty());

        // the first query check for composition contains cluster n AND admin_entry contains cluster a
        String query1 = "SELECT u" + " FROM EHR e "
                + "   CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.event_summary.v0] "
                + "       CONTAINS "
                + "           (CLUSTER n[openEHR-EHR-CLUSTER.case_identification.v0] "
                + "            and ADMIN_ENTRY u[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0] "
                + "                   CONTAINS CLUSTER a[openEHR-EHR-CLUSTER.location.v1] "
                + "                   )";

        AqlExpression aqlExpression = new AqlExpression().parse(query1);
        contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        assertTrue(contains.getTemplates().contains("Patientenaufenthalt"));
    }

    @Test
    public void testContainWithUnknownContainment() {

        // the first query check for composition contains admin_entry contains both cluster a AND cluster n
        String query = "SELECT u" + " FROM EHR e "
                + "   CONTAINS COMPOSITION c "
                + "       CONTAINS "
                + "           ("
                + "                   CLUSTER a[openEHR-EHR-CLUSTER.location.v1] "
                + "                   and "
                + "                   CLUSTER n[openEHR-EHR-CLUSTER.case_identification.v0]"
                + "                   and "
                + "                   CLUSTER z[openEHR-EHR-CLUSTER.unknown.v0]"
                + "            )";

        Contains contains = new Contains(new AqlExpression().parse(query).getParseTree(), knowledge).process();
        assertTrue(contains.getTemplates().isEmpty());
    }
}
