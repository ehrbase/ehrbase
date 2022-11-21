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

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ehrbase.aql.TestAqlBase;
import org.ehrbase.aql.containment.*;
import org.junit.Test;

public class QueryCompilerPass1Test extends TestAqlBase {

    @Test
    public void testGetIdentifierMapper() {
        ParseTreeWalker walker = new ParseTreeWalker();

        // variable for ehr
        {
            QueryCompilerPass1 cut = new QueryCompilerPass1();
            String aql = "select e/ehr_id/value " + "from EHR e ";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);

            IdentifierMapper identifierMapper = cut.getIdentifierMapper();

            assertThat(identifierMapper.getClassName("e")).isEqualTo("EHR");
            assertThat(identifierMapper.getEhrContainer()).isNull();
        }
        // variable for ehr with predicate
        {
            QueryCompilerPass1 cut = new QueryCompilerPass1();
            String aql =
                    "select e/ehr_id/value " + "from EHR e [ehr_id/value = '26332710-16f3-4b54-aae9-4d11c141388c']";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);

            IdentifierMapper identifierMapper = cut.getIdentifierMapper();

            assertThat(identifierMapper.getClassName("e")).isEqualTo("EHR");
            assertThat(identifierMapper.getEhrContainer().getField()).isEqualTo("ehr_id/value");
            assertThat(identifierMapper.getEhrContainer().getIdentifier()).isEqualTo("e");
            assertThat(identifierMapper.getEhrContainer().getOperator()).isEqualTo("=");
            assertThat(identifierMapper.getEhrContainer().getValue())
                    .isEqualTo("'26332710-16f3-4b54-aae9-4d11c141388c'");
        }

        // variable for ehr and compose
        {
            QueryCompilerPass1 cut = new QueryCompilerPass1();
            String aql = "select e/ehr_id/value "
                    + "from EHR e contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);

            IdentifierMapper identifierMapper = cut.getIdentifierMapper();

            assertThat(identifierMapper.getClassName("e")).isEqualTo("EHR");

            assertThat(identifierMapper.getClassName("a")).isEqualTo("COMPOSITION");
            assertThat(identifierMapper.getArchetypeId("a")).isEqualTo("openEHR-EHR-COMPOSITION.health_summary.v1");
        }

        // variable  compose
        {
            QueryCompilerPass1 cut = new QueryCompilerPass1();
            String aql = "select a/uid/value "
                    + "from EHR [ehr_id/value = '26332710-16f3-4b54-aae9-4d11c141388c']  contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            walker.walk(cut, tree);

            IdentifierMapper identifierMapper = cut.getIdentifierMapper();

            assertThat(identifierMapper.getClassName("a")).isEqualTo("COMPOSITION");
            assertThat(identifierMapper.getArchetypeId("a")).isEqualTo("openEHR-EHR-COMPOSITION.health_summary.v1");
        }
    }

    //    @Test
    //    public void testGetClosedSetList() {
    //
    //
    //        ParseTreeWalker walker = new ParseTreeWalker();
    //
    //
    //        // no containment
    //        {
    //            QueryCompilerPass1 cut = new QueryCompilerPass1();
    //            String aql = "select e/ehr_id/value " +
    //                    "from EHR e";
    //            ParseTree tree = QueryHelper.setupParseTree(aql);
    //            walker.walk(cut, tree);
    //            List<ContainmentSet> actual = cut.getClosedSetList();
    //            assertThat(actual).size().isEqualTo(1);
    //            assertThat(actual.get(0)).isNull();
    //
    //        }
    //
    //
    //        // single containment
    //        {
    //            QueryCompilerPass1 cut = new QueryCompilerPass1();
    //            String aql = "select e/ehr_id/value " +
    //                    "from EHR e  contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]";
    //            ParseTree tree = QueryHelper.setupParseTree(aql);
    //            walker.walk(cut, tree);
    //            List<ContainmentSet> actual = cut.getClosedSetList();
    //            assertThat(actual).size().isEqualTo(1);
    //            ContainmentSet containmentSet = actual.get(0);
    //            assertThat(containmentSet.getEnclosing()).isNull();
    //            assertThat(containmentSet.getParentSet()).isNull();
    //            assertThat(containmentSet.getContainmentList()).size().isEqualTo(1);
    //            Containment containment = (Containment) containmentSet.getContainmentList().get(0);
    //            Containment expected = ContainmentTest.buildContainment(null, "a",
    // "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment, expected);
    //        }
    //
    //        // nested containment
    //        {
    //            QueryCompilerPass1 cut = new QueryCompilerPass1();
    //            String aql = "select e/ehr_id/value " +
    //                    "from EHR e  contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1] contains ACTION
    // d[openEHR-EHR-ACTION.immunisation_procedure.v1]";
    //            ParseTree tree = QueryHelper.setupParseTree(aql);
    //            walker.walk(cut, tree);
    //            List<ContainmentSet> actual = cut.getClosedSetList();
    //            assertThat(actual).size().isEqualTo(1);
    //            ContainmentSet containmentSet = actual.get(0);
    //            assertThat(containmentSet.getEnclosing()).isNull();
    //            assertThat(containmentSet.getParentSet()).isNull();
    //            assertThat(containmentSet.getContainmentList()).size().isEqualTo(2);
    //
    //
    //            Containment containment1 = (Containment) containmentSet.getContainmentList().get(0);
    //            Containment expected1 = ContainmentTest.buildContainment(null, "a",
    // "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment1, expected1);
    //
    //
    //            Containment containment2 = (Containment) containmentSet.getContainmentList().get(1);
    //            Containment expected2 = ContainmentTest.buildContainment(containment1, "d",
    // "openEHR-EHR-ACTION.immunisation_procedure.v1", "ACTION", null);
    //            ContainmentTest.checkEqual(containment2, expected2);
    //        }
    //
    //
    //        // triple nested containment
    //        {
    //            QueryCompilerPass1 cut = new QueryCompilerPass1();
    //            String aql = "select e/ehr_id/value " +
    //                    "from EHR e  contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1] contains ACTION
    // d[openEHR-EHR-ACTION.immunisation_procedure.v1] contains CLUSTER a_a[openEHR-EHR-CLUSTER.device.v1]";
    //            ParseTree tree = QueryHelper.setupParseTree(aql);
    //            walker.walk(cut, tree);
    //            List<ContainmentSet> actual = cut.getClosedSetList();
    //            assertThat(actual).size().isEqualTo(1);
    //            ContainmentSet containmentSet = actual.get(0);
    //            assertThat(containmentSet.getEnclosing()).isNull();
    //            assertThat(containmentSet.getParentSet()).isNull();
    //            assertThat(containmentSet.getContainmentList()).size().isEqualTo(3);
    //
    //
    //            Containment containment1 = (Containment) containmentSet.getContainmentList().get(0);
    //            Containment expected1 = ContainmentTest.buildContainment(null, "a",
    // "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment1, expected1);
    //
    //
    //            Containment containment2 = (Containment) containmentSet.getContainmentList().get(1);
    //            Containment expected2 = ContainmentTest.buildContainment(containment1, "d",
    // "openEHR-EHR-ACTION.immunisation_procedure.v1", "ACTION", null);
    //            ContainmentTest.checkEqual(containment2, expected2);
    //
    //            Containment containment3 = (Containment) containmentSet.getContainmentList().get(1);
    //            Containment expected3 = ContainmentTest.buildContainment(containment1, "a_a",
    // "openEHR-EHR-CLUSTER.device.v1", "CLUSTER", null);
    //            ContainmentTest.checkEqual(containment2, expected2);
    //        }
    //
    //        // boolean operators
    //        {
    //            QueryCompilerPass1 cut = new QueryCompilerPass1();
    //            String aql = "select e/ehr_id/value" +
    //                    " from EHR e CONTAINS COMPOSITION a [openEHR-EHR-COMPOSITION.health_summary.v1] AND
    // COMPOSITION d [openEHR-EHR-COMPOSITION.referral.v1]";
    //            ParseTree tree = QueryHelper.setupParseTree(aql);
    //            walker.walk(cut, tree);
    //            List<ContainmentSet> actual = cut.getClosedSetList();
    //            assertThat(actual).size().isEqualTo(1);
    //            ContainmentSet containmentSet = actual.get(0);
    //            assertThat(containmentSet.getEnclosing()).isNull();
    //            assertThat(containmentSet.getParentSet()).isNull();
    //            assertThat(containmentSet.getContainmentList()).size().isEqualTo(3);
    //
    //
    //            Containment containment1 = (Containment) containmentSet.getContainmentList().get(0);
    //            Containment expected1 = ContainmentTest.buildContainment(null, "a",
    // "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment1, expected1);
    //
    //
    // assertThat(((ContainOperator)containmentSet.getContainmentList().get(1)).getOperator()).isEqualTo("AND");
    //
    //            Containment containment2 = (Containment) containmentSet.getContainmentList().get(2);
    //            Containment expected2 = ContainmentTest.buildContainment(null, "d",
    // "openEHR-EHR-COMPOSITION.referral.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment2, expected2);
    //        }
    //
    //        // multiple of the same  boolean operators
    //        {
    //            QueryCompilerPass1 cut = new QueryCompilerPass1();
    //            String aql = "select e/ehr_id/value" +
    //                    " from EHR e CONTAINS COMPOSITION a [openEHR-EHR-COMPOSITION.health_summary.v1] XOR
    // COMPOSITION d [openEHR-EHR-COMPOSITION.administrative_encounter.v1] XOR  COMPOSITION c
    // [openEHR-EHR-COMPOSITION.referral.v1] ";
    //            ParseTree tree = QueryHelper.setupParseTree(aql);
    //            walker.walk(cut, tree);
    //            List<ContainmentSet> actual = cut.getClosedSetList();
    //            assertThat(actual).size().isEqualTo(1);
    //            ContainmentSet containmentSet = actual.get(0);
    //            assertThat(containmentSet.getEnclosing()).isNull();
    //            assertThat(containmentSet.getParentSet()).isNull();
    //            assertThat(containmentSet.getContainmentList()).size().isEqualTo(5);
    //
    //
    //            Containment containment1 = (Containment) containmentSet.getContainmentList().get(0);
    //            Containment expected1 = ContainmentTest.buildContainment(null, "a",
    // "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment1, expected1);
    //
    //
    //
    // assertThat(((ContainOperator)containmentSet.getContainmentList().get(1)).getOperator()).isEqualTo("XOR");
    //
    //            Containment containment2 = (Containment) containmentSet.getContainmentList().get(2);
    //            Containment expected2 = ContainmentTest.buildContainment(null, "d",
    // "openEHR-EHR-COMPOSITION.administrative_encounter.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment2, expected2);
    //
    //
    // assertThat(((ContainOperator)containmentSet.getContainmentList().get(3)).getOperator()).isEqualTo("XOR");
    //
    //            Containment containment3 = (Containment) containmentSet.getContainmentList().get(4);
    //            Containment expected3 = ContainmentTest.buildContainment(null, "c",
    // "openEHR-EHR-COMPOSITION.referral.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment3, expected3);
    //        }
    //
    //        // multiple of different  boolean operators
    //        {
    //            QueryCompilerPass1 cut = new QueryCompilerPass1();
    //            String aql = "select e/ehr_id/value" +
    //                    " from EHR e CONTAINS COMPOSITION a [openEHR-EHR-COMPOSITION.health_summary.v1] XOR ACTION d
    // [openEHR-EHR-ACTION.immunisation_procedure.v1] AND COMPOSITION c [openEHR-EHR-COMPOSITION.referral.v1] ";
    //            ParseTree tree = QueryHelper.setupParseTree(aql);
    //            walker.walk(cut, tree);
    //            List<ContainmentSet> actual = cut.getClosedSetList();
    //            assertThat(actual).size().isEqualTo(1);
    //            ContainmentSet containmentSet = actual.get(0);
    //            assertThat(containmentSet.getEnclosing()).isNull();
    //            assertThat(containmentSet.getParentSet()).isNull();
    //            assertThat(containmentSet.getContainmentList()).size().isEqualTo(5);
    //
    //
    //            Containment containment1 = (Containment) containmentSet.getContainmentList().get(0);
    //            Containment expected1 = ContainmentTest.buildContainment(null, "a",
    // "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment1, expected1);
    //
    //
    // assertThat(((ContainOperator)containmentSet.getContainmentList().get(1)).getOperator()).isEqualTo("XOR");
    //
    //            Containment containment2 = (Containment) containmentSet.getContainmentList().get(2);
    //            Containment expected2 = ContainmentTest.buildContainment(null, "d",
    // "openEHR-EHR-ACTION.immunisation_procedure.v1", "ACTION", null);
    //            ContainmentTest.checkEqual(containment2, expected2);
    //
    //
    // assertThat(((ContainOperator)containmentSet.getContainmentList().get(3)).getOperator()).isEqualTo("AND");
    //
    //            Containment containment3 = (Containment) containmentSet.getContainmentList().get(4);
    //            Containment expected3 = ContainmentTest.buildContainment(null, "c",
    // "openEHR-EHR-COMPOSITION.referral.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment3, expected3);
    //        }
    //
    //
    //        // boolean operators in nested containment
    //        {
    //            QueryCompilerPass1 cut = new QueryCompilerPass1();
    //            String aql = "select e/ehr_id/value" +
    //                    " from EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.referral.v1]\n" +
    //                    "        CONTAINS (OBSERVATION o [openEHR-EHR-OBSERVATION.laboratory-hba1c.v1] AND OBSERVATION
    // o1 [openEHR-EHR-OBSERVATION.laboratory-glucose.v1])";
    //            ParseTree tree = QueryHelper.setupParseTree(aql);
    //            walker.walk(cut, tree);
    //            List<ContainmentSet> actual = cut.getClosedSetList();
    //            assertThat(actual).size().isEqualTo(2);
    //
    //
    //            ContainmentSet containmentSet2 = actual.get(1);
    //            assertThat(containmentSet2.getEnclosing()).isNull();
    //            assertThat(containmentSet2.getParentSet()).isNull();
    //            assertThat(containmentSet2.getContainmentList()).size().isEqualTo(1);
    //
    //
    //            Containment containment1 = (Containment) containmentSet2.getContainmentList().get(0);
    //            Containment expected1 = ContainmentTest.buildContainment(null, "c",
    // "openEHR-EHR-COMPOSITION.referral.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment1, expected1);
    //
    //            ContainmentSet containmentSet1 = actual.get(0);
    //            assertThat(containmentSet1.getEnclosing()).isEqualTo(containment1);
    //            assertThat(containmentSet1.getParentSet()).isEqualTo(containmentSet2);
    //            assertThat(containmentSet1.getContainmentList()).size().isEqualTo(3);
    //
    //
    //            Containment containment2 = (Containment) containmentSet1.getContainmentList().get(0);
    //            Containment expected12 = ContainmentTest.buildContainment(containment1, "o",
    // "openEHR-EHR-OBSERVATION.laboratory-hba1c.v1", "OBSERVATION", null);
    //            ContainmentTest.checkEqual(containment2, expected12);
    //
    //
    // assertThat(((ContainOperator)containmentSet1.getContainmentList().get(1)).getOperator()).isEqualTo("AND");
    //
    //            Containment containment3 = (Containment) containmentSet1.getContainmentList().get(2);
    //            Containment expected3 = ContainmentTest.buildContainment(containment1, "o1",
    // "openEHR-EHR-OBSERVATION.laboratory-glucose.v1", "OBSERVATION", null);
    //            ContainmentTest.checkEqual(containment3, expected3);
    //        }
    //
    //
    //        // boolean operators over nested containment
    //        {
    //            QueryCompilerPass1 cut = new QueryCompilerPass1();
    //            String aql = "select e/ehr_id/value" +
    //                    " from EHR e CONTAINS (COMPOSITION c [openEHR-EHR-COMPOSITION.referral.v1]\n" +
    //                    "        CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.laboratory-hba1c.v1]) OR COMPOSITION
    // c2 [openEHR-EHR-COMPOSITION.health_summary.v1]";
    //            ParseTree tree = QueryHelper.setupParseTree(aql);
    //            walker.walk(cut, tree);
    //            List<ContainmentSet> actual = cut.getClosedSetList();
    //            assertThat(actual).size().isEqualTo(2);
    //
    //
    //            ContainmentSet containmentSet2 = actual.get(1);
    //            assertThat(containmentSet2.getEnclosing()).isNull();
    //            assertThat(containmentSet2.getParentSet()).isNull();
    //            assertThat(containmentSet2.getContainmentList()).size().isEqualTo(2);
    //
    //
    // assertThat(((ContainOperator)containmentSet2.getContainmentList().get(0)).getOperator()).isEqualTo("OR");
    //
    //            Containment containment3 = (Containment) containmentSet2.getContainmentList().get(1);
    //            Containment expected3 = ContainmentTest.buildContainment(null, "c2",
    // "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment3, expected3);
    //
    //
    //            ContainmentSet containmentSet1 = actual.get(0);
    //            assertThat(containmentSet1.getEnclosing()).isEqualTo(ContainmentTest.buildContainment(null, null,
    // null, null, null));
    //            assertThat(containmentSet1.getParentSet()).isNull();
    //            assertThat(containmentSet1.getContainmentList()).size().isEqualTo(2);
    //
    //
    //            Containment containment1 = (Containment) containmentSet1.getContainmentList().get(0);
    //            Containment expected1 = ContainmentTest.buildContainment(null, "c",
    // "openEHR-EHR-COMPOSITION.referral.v1", "COMPOSITION", null);
    //            ContainmentTest.checkEqual(containment1, expected1);
    //
    //            Containment containment2 = (Containment) containmentSet1.getContainmentList().get(1);
    //            Containment expected2 = ContainmentTest.buildContainment(containment1, "o",
    // "openEHR-EHR-OBSERVATION.laboratory-hba1c.v1", "OBSERVATION", null);
    //            ContainmentTest.checkEqual(containment2, expected2);
    //        }
    //
    //
    //    }

}
