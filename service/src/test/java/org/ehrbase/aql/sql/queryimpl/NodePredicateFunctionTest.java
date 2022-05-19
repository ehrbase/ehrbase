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
package org.ehrbase.aql.sql.queryimpl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class NodePredicateFunctionTest {

    // representation for:
    // content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0004,'Systolic']

    private String[] testPath = {
        "/content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]",
        "0",
        "/data[at0001]",
        "/events",
        "/events[at0002]",
        "0",
        "/data[at0003]",
        "/items[at0004]",
        "$AQL_NODE_NAME_PREDICATE$",
        "'Systolic'",
        "0",
        "/value",
        "/magnitude"
    };

    @Test
    public void testSQLTranslation() {

        List itemPathArray = new NodePredicateCall(Arrays.asList(testPath.clone())).resolve();

        assertThat(itemPathArray.get(0).toString())
                .as(String.join("", Arrays.asList(testPath)))
                .isEqualToIgnoringNewLines("ehr.aql_node_name_predicate(" + "\"ehr\".\"entry\".\"entry\","
                        + "'Systolic',"
                        + "'/content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1],0,/data[at0001],/events,/events[at0002],0,/data[at0003],/items[at0004]'"
                        + ")"
                        + "#>>'{/value,/magnitude}'");
    }
}
