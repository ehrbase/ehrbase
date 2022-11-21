/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.util.LinkedHashMap;
import org.junit.Test;

public class VariableMultiMapTest {

    private String[][] variableSet = {
        {"LabordatenID", "/context/other_context[at0001]/items[at0002]/value/value"},
        {"PatientID", "/ehr_status/subject/external_ref/id/value"},
        {"FallID", "/items[at0001]/value/value"},
        {"Befund", "/items[at0001]/value/value"}
    };

    @Test
    public void testAqlResult() {
        AqlResult aqlResult = new AqlResult(null, null);

        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (String[] entry : variableSet) {
            linkedHashMap.put(entry[0], entry[1]);
        }

        aqlResult.setVariables(linkedHashMap);

        assertEquals(4, aqlResult.getVariables().size());

        // check for a column
        assertTrue(aqlResult.variablesContains("FallID"));

        // reverse lookup
        assertTrue(aqlResult.variablesContains("/items[at0001]/value/value"));
    }
}
