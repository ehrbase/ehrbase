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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;

public class AqlExpressionWithParametersTest {

    @Test
    public void testParseAql() {

        Map<String, Object> map = new HashMap<>();

        map.put("nameValue1", "nameValue1");
        map.put("nameValue2", "nameValue2");
        map.put("max_value", 123);
        map.put("another_value", 456);
        map.put("another_value", 456);
        map.put("ehrId", UUID.fromString("f002a367-52ad-4bee-aa14-67627db677ad"));

        String expectedSubstituted = "select\n"
                + "   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005, 'nameValue1']/value/magnitude as diastolic,\n"
                + "   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004, 'nameValue2']/value/magnitude as systolic\n"
                + "from EHR e[ehr_id/value='f002a367-52ad-4bee-aa14-67627db677ad']\n"
                + "  contains COMPOSITION a\n"
                + "    contains OBSERVATION o_bp[openEHR-EHR-OBSERVATION.blood_pressure.v1]\n"
                + "where o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude < 123\n"
                + "   and o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude > 456";

        String aql = "select\n"
                + "   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005, $nameValue1]/value/magnitude as diastolic,\n"
                + "   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004, $nameValue2]/value/magnitude as systolic\n"
                + "from EHR e[ehr_id/value=$ehrId]\n"
                + "  contains COMPOSITION a\n"
                + "    contains OBSERVATION o_bp[openEHR-EHR-OBSERVATION.blood_pressure.v1]\n"
                + "where o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude < $max_value\n"
                + "   and o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude > $another_value";

        String aqlExpression = new AqlExpressionWithParameters().substitute(aql, map);

        assertEquals(expectedSubstituted, aqlExpression);
    }

    @Test
    public void testParseMissingValue() {

        Map<String, Object> map = new HashMap<>();

        map.put("nameValue1", "nameValue1");
        //        map.put("nameValue2", "nameValue2");
        map.put("max_value", 123);
        map.put("another_value", 456);
        map.put("ehrId", UUID.fromString("f002a367-52ad-4bee-aa14-67627db677ad"));

        String aql = "select\n"
                + "   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005, '$nameValue1']/value/magnitude as diastolic,\n"
                + "   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004, '$nameValue2']/value/magnitude as systolic\n"
                + "from EHR e[ehr_id/value='$ehrId']\n"
                + "  contains COMPOSITION a\n"
                + "    contains OBSERVATION o_bp[openEHR-EHR-OBSERVATION.blood_pressure.v1]\n"
                + "where o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude < $max_value\n"
                + "   and o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude > $another_value";

        try {
            String aqlExpression = new AqlExpressionWithParameters().substitute(aql, map);
            fail("Missing parameter hasn't been detected");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }
    }

    @Test
    public void testParameter1() {
        Map<String, Object> map = new HashMap<>();
        map.put("name-Value-1", "nameValue1");

        assertEquals("'nameValue1'", new AqlExpressionWithParameters().substitute("$name-Value-1", map));
    }

    @Test
    public void testParameter2() {
        Map<String, Object> map = new HashMap<>();
        map.put("name_Value_1", "nameValue1");

        assertEquals("'nameValue1'", new AqlExpressionWithParameters().substitute("$name_Value_1", map));
    }

    @Test
    public void testParameter3() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", 1234);

        assertEquals("1234", new AqlExpressionWithParameters().substitute("$value", map));
    }

    @Test
    public void testParameter4() {
        Map<String, Object> map = new HashMap<>();
        map.put("terminologyId", "openehr");

        assertEquals(
                "[at0002 and value/defining_code/terminology_id/value='openehr']",
                new AqlExpressionWithParameters()
                        .substitute("[at0002 and value/defining_code/terminology_id/value=$terminologyId]", map));
    }

    @Test
    public void testJsonParameters() {

        // from https://specifications.openehr.org/releases/ITS-REST/latest/query.html#query-execute-query-get
        String jsonParameters = " {"
                + " \"q\": \"SELECT c FROM EHR e[ehr_id/value=$ehr_id] CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.encounter.v1] CONTAINS OBSERVATION obs[openEHR-EHR-OBSERVATION.blood_pressure.v1] WHERE obs/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude >= $systolic_bp\",\n"
                + "  \"offset\": 0,\n"
                + "  \"fetch\": 10,"
                + " \"query-parameters\": {\n"
                + "    \"nameValue1\": \"nameValue1\","
                + "    \"nameValue2\": \"nameValue2\","
                + "    \"ehr_id\": \"f002a367-52ad-4bee-aa14-67627db677ad\",\n"
                + "    \"diastolic_bp\": 80,\n"
                + "    \"systolic_bp\": 140\n"
                + "  }"
                + "}";

        String expectedSubstituted =
                "(query (queryExpr (select select (selectExpr (identifiedPath o_bp / (objectPath (pathPart data (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0001))) ])) / (pathPart events (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0006))) ])) / (pathPart data (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0003))) ])) / (pathPart items (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0005 , 'nameValue1'))) ])) / (pathPart value) / (pathPart magnitude))) as diastolic , (selectExpr (identifiedPath o_bp / (objectPath (pathPart data (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0001))) ])) / (pathPart events (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0006))) ])) / (pathPart data (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0003))) ])) / (pathPart items (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0004 , 'nameValue2'))) ])) / (pathPart value) / (pathPart magnitude))) as systolic))) (from from (fromEHR EHR e (standardPredicate [ (predicateExpr (predicateAnd (predicateEquality (predicateOperand (objectPath (pathPart ehr_id) / (pathPart value))) = (predicateOperand (operand 'f002a367-52ad-4bee-aa14-67627db677ad'))))) ])) contains (containsExpression (containExpressionBool (contains (simpleClassExpr COMPOSITION a) contains (containsExpression (containExpressionBool (contains (simpleClassExpr (archetypedClassExpr OBSERVATION o_bp [ openEHR-EHR-OBSERVATION.blood_pressure.v1 ]))))))))) (where where (identifiedExpr (identifiedEquality (identifiedOperand (identifiedPath o_bp / (objectPath (pathPart data (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0001))) ])) / (pathPart events (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0006))) ])) / (pathPart data (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0003))) ])) / (pathPart items (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0005))) ])) / (pathPart value) / (pathPart magnitude)))) < (identifiedOperand (operand 80.0))) and (identifiedEquality (identifiedOperand (identifiedPath o_bp / (objectPath (pathPart data (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0001))) ])) / (pathPart events (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0006))) ])) / (pathPart data (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0003))) ])) / (pathPart items (predicate [ (nodePredicateOr (nodePredicateAnd (nodePredicateComparable at0004))) ])) / (pathPart value) / (pathPart magnitude)))) > (identifiedOperand (operand 140.0))))) <EOF>))";

        String aql = "select\n"
                + "   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005, $nameValue1]/value/magnitude as diastolic,\n"
                + "   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004, $nameValue2]/value/magnitude as systolic\n"
                + "from EHR e[ehr_id/value=$ehr_id]\n"
                + "  contains COMPOSITION a\n"
                + "    contains OBSERVATION o_bp[openEHR-EHR-OBSERVATION.blood_pressure.v1]\n"
                + "where o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude < $diastolic_bp\n"
                + "   and o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude > $systolic_bp";

        AqlExpressionWithParameters aqlExpression = new AqlExpressionWithParameters().parse(aql, jsonParameters);

        assertEquals(expectedSubstituted, aqlExpression.dump());
    }
}
