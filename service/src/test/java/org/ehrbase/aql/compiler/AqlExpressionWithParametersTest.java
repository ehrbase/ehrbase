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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AqlExpressionWithParametersTest {

    @Test
    void testParseAql() {

        Map<String, Object> map = new HashMap<>();

        map.put("nameValue1", "nameValue1");
        map.put("nameValue2", "nameValue2");
        map.put("max_value", 123);
        map.put("another_value", 456);
        map.put("ehrId", UUID.fromString("f002a367-52ad-4bee-aa14-67627db677ad"));

        String expectedSubstituted =
                """
                select
                   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005, 'nameValue1']/value/magnitude as diastolic,
                   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004, 'nameValue2']/value/magnitude as systolic
                from EHR e[ehr_id/value='f002a367-52ad-4bee-aa14-67627db677ad']
                  contains COMPOSITION a
                    contains OBSERVATION o_bp[openEHR-EHR-OBSERVATION.blood_pressure.v1]
                where o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude < 123
                   and o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude > 456""";
        String aql =
                """
                select
                   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005, $nameValue1]/value/magnitude as diastolic,
                   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004, $nameValue2]/value/magnitude as systolic
                from EHR e[ehr_id/value=$ehrId]
                  contains COMPOSITION a
                    contains OBSERVATION o_bp[openEHR-EHR-OBSERVATION.blood_pressure.v1]
                where o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude < $max_value
                   and o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude > $another_value""";

        String aqlExpression = new AqlExpressionWithParameters().substitute(aql, map);

        assertEquals(expectedSubstituted, aqlExpression);
    }

    @Test
    void testParseMissingValue() {

        Map<String, Object> map = new HashMap<>();

        map.put("nameValue1", "nameValue1");
        //        map.put("nameValue2", "nameValue2");
        map.put("max_value", 123);
        map.put("another_value", 456);
        map.put("ehrId", UUID.fromString("f002a367-52ad-4bee-aa14-67627db677ad"));

        String aql =
                """
                select
                   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005, '$nameValue1']/value/magnitude as diastolic,
                   o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004, '$nameValue2']/value/magnitude as systolic
                from EHR e[ehr_id/value='$ehrId']
                  contains COMPOSITION a
                    contains OBSERVATION o_bp[openEHR-EHR-OBSERVATION.blood_pressure.v1]
                where o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude < $max_value
                   and o_bp/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude > $another_value""";

        try {
            String aqlExpression = new AqlExpressionWithParameters().substitute(aql, map);
            fail("Missing parameter hasn't been detected");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }
    }

    @Test
    void testParameter1() {
        Map<String, Object> map = new HashMap<>();
        map.put("name-Value-1", "nameValue1");

        assertEquals("'nameValue1'", new AqlExpressionWithParameters().substitute("$name-Value-1", map));
    }

    @Test
    void testParameter2() {
        Map<String, Object> map = new HashMap<>();
        map.put("name_Value_1", "nameValue1");

        assertEquals("'nameValue1'", new AqlExpressionWithParameters().substitute("$name_Value_1", map));
    }

    @Test
    void testParameter3() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", 1234);

        assertEquals("1234", new AqlExpressionWithParameters().substitute("$value", map));
    }

    @Test
    void testParameter4() {
        Map<String, Object> map = new HashMap<>();
        map.put("terminologyId", "openehr");

        assertEquals(
                "[at0002 and value/defining_code/terminology_id/value='openehr']",
                new AqlExpressionWithParameters()
                        .substitute("[at0002 and value/defining_code/terminology_id/value=$terminologyId]", map));
    }
}
