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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import org.ehrbase.aql.TestAqlBase;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.jooq.DSLContext;
import org.junit.Test;

/**
 * Created by christian on 4/1/2016.
 */
public class AqlExpressionTest extends TestAqlBase {

    private DSLContext context = DSLContextHelper.buildContext();

    @Test
    public void testDump() {

        String query = "SELECT o/data[at0002]/events[at0003] AS systolic\n" + "FROM EHR [ehr_id/value='1234'] \n"
                + "CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.encounter.v1] \n"
                + "CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.blood_pressure.v1]\n"
                + "WHERE o/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/value > 140";

        assertThat(new AqlExpression().parse(query).dump())
                .isEqualTo(
                        "(query (queryExpr (select SELECT (selectExpr (identifiedPath o / (objectPath (pathPart data (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0002))) ])) / (pathPart events (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0003))) ])))) AS systolic)) (from FROM (fromEHR EHR (standardPredicate [ (predicateExpr (predicateAnd (predicateEquality (predicateOperand (objectPath (pathPart ehr_id) / (pathPart value))) = (predicateOperand (operand '1234'))))) ])) CONTAINS (containsExpression (containExpressionBool (contains (simpleClassExpr (archetypedClassExpr COMPOSITION c [ openEHR-EHR-COMPOSITION.encounter.v1 ])) CONTAINS (containsExpression (containExpressionBool (contains (simpleClassExpr (archetypedClassExpr OBSERVATION o [ openEHR-EHR-OBSERVATION.blood_pressure.v1 ]))))))))) (where WHERE (identifiedExpr (identifiedEquality (identifiedOperand (identifiedPath o / (objectPath (pathPart data (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0001))) ])) / (pathPart events (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0006))) ])) / (pathPart data (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0003))) ])) / (pathPart items (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0004))) ])) / (pathPart value) / (pathPart value)))) > (identifiedOperand (operand 140))))) <EOF>))");
    }

    @Test
    public void testPass1() {

        String query = "SELECT o/data[at0002]/events[at0003] AS systolic\n" + "FROM EHR [ehr_id/value='1234'] \n"
                + "CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.encounter.v1] \n"
                + "CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.blood_pressure.v1]\n"
                + "WHERE o/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/value > 140";

        AqlExpression cut = new AqlExpression().parse(query);
        Contains contains = new Contains(cut.getParseTree(), knowledge).process();

        assertThat(contains.getIdentifierMapper()).isNotNull();
        assertThat(contains.getTemplates()).isNotNull();
        //        assertThat(contains.getContainClause()).isNotNull();
    }

    @Test
    public void testPass2() {

        String query = "SELECT o/data[at0002]/events[at0003] AS systolic\n" + "FROM EHR [ehr_id/value='1234'] \n"
                + "CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.encounter.v1] \n"
                + "CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.blood_pressure.v1]\n"
                + "WHERE o/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/value > 140";

        AqlExpression cut = new AqlExpression().parse(query);

        Statements statements = new Statements(
                        cut.getParseTree(),
                        new Contains(cut.getParseTree(), knowledge).process().getIdentifierMapper(),
                        mock(ExternalTerminologyValidation.class))
                .process();

        assertThat(statements.getVariables()).isNotNull();
        assertThat(statements.getWhereClause()).isNotNull();
    }

    @Test
    public void testRejectDuplicateAliases() {

        String query = "SELECT o/data[at0002]/events[at0003] AS value, o/data[at0003]/events[at0004] as value\n"
                + "FROM EHR [ehr_id/value='1234'] \n"
                + "CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.encounter.v1] \n"
                + "CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.blood_pressure.v1]";

        AqlExpression cut = new AqlExpression().parse(query);

        try {
            new Statements(
                            cut.getParseTree(),
                            new Contains(cut.getParseTree(), knowledge)
                                    .process()
                                    .getIdentifierMapper(),
                            mock(ExternalTerminologyValidation.class))
                    .process();

            fail("duplicate alias has not been detected");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testWhereExpressionWithParenthesis() {

        String query = "select\n" + "    e/ehr_id,\n"
                + "    a_a/data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/magnitude,\n"
                + "    a_a/data[at0002]/events[at0003]/time/value\n"
                + "from EHR e\n"
                + "contains COMPOSITION a\n"
                + "contains OBSERVATION a_a[openEHR-EHR-OBSERVATION.body_temperature.v1]\n"
                + "where a_a/data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/magnitude>38\n"
                + "AND  a_a/data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units = '°C'\n"
                + "AND e/ehr_id/value MATCHES {\n"
                + "    '849bf097-bd16-44fc-a394-10676284a012',\n"
                + "    '34b2e263-00eb-40b8-88f1-823c87096457'}\n"
                + "    OR (a_a/data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units = '°C' AND a_a/data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units = '°C')";

        AqlExpression cut = new AqlExpression().parse(query);

        Statements statements = new Statements(
                        cut.getParseTree(),
                        new Contains(cut.getParseTree(), knowledge).process().getIdentifierMapper(),
                        mock(ExternalTerminologyValidation.class))
                .process();

        assertThat(statements.getWhereClause()).isNotNull();
    }

    @Test
    public void testSpecificRule1() {

        String query = "COMPOSITIONc[openEHR-EHR-COMPOSITION.123.minimal.v1]";
        try {
            AqlExpression cut = new AqlExpression().parse(query, "archetypedClassExpr");

            fail("the invalid archetype id should have been rejected");
        } catch (Exception e) {
        }
    }
}
