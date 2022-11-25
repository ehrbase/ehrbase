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
package org.ehrbase.aql.sql.queryimpl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.ehrbase.aql.TestAqlBase;
import org.junit.Before;
import org.junit.Test;

/*
 * Copyright (c) 2022. vitasystems GmbH and Hannover Medical School.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

public class IterativeNodeTest extends TestAqlBase {

    IterativeNode cut;

    @Before
    public void setUp() {
        cut = new IterativeNode(this.testDomainAccess, "IDCR - Immunisation summary.v0", knowledge);
    }

    @Test
    public void iterativeAt() {

        assertThat(cut.iterativeAt(List.of(
                        "/composition[openEHR-EHR-COMPOSITION.health_summary.v1]",
                        "/content[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0]",
                        "0")))
                .isEmpty();

        assertThat(cut.iterativeAt(List.of(
                        "/composition[openEHR-EHR-COMPOSITION.health_summary.v1]",
                        "/content[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                        "0")))
                .containsExactly(2);

        assertThat(cut.iterativeAt(List.of(
                        "/composition[openEHR-EHR-COMPOSITION.health_summary.v1]",
                        "/content[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                        "0",
                        "/description[at0001]",
                        "/items[at0002]",
                        "0",
                        "/value,value")))
                .containsExactly(2);
    }

    @Test
    public void aqlPathInJsonbArray() {
        assertThat(cut.aqlPathInJsonbArray(
                        List.of("content[openEHR-EHR-ACTION.immunisation_procedure.v1]"),
                        List.of(
                                "/composition[openEHR-EHR-COMPOSITION.health_summary.v1]",
                                "/content[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                                "0",
                                "/description[at0001]",
                                "/items[at0002]",
                                "0",
                                "/value,value")))
                .isEqualTo(2);

        assertThat(cut.aqlPathInJsonbArray(
                        List.of("content[openEHR-EHR-ACTION.immunisation_procedure.v1]"),
                        List.of(
                                "/composition[openEHR-EHR-COMPOSITION.health_summary.v1]",
                                "/content[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                                "0",
                                "/description[at0001]",
                                "/items[at0004]",
                                "0",
                                "/value,magnitude")))
                .isEqualTo(2);

        assertThat(cut.aqlPathInJsonbArray(
                        List.of("content[openEHR-EHR-ACTION.immunisation_procedure.v1]"),
                        List.of(
                                "/composition[openEHR-EHR-COMPOSITION.health_summary.v1]",
                                "/content[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                                "0")))
                .isEqualTo(2);

        assertThat(cut.aqlPathInJsonbArray(
                        List.of("content[openEHR-EHR-ACTION.immunisation_procedure.v1]"),
                        List.of(
                                "/composition[openEHR-EHR-COMPOSITION.health_summary.v1]",
                                "/content[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                                "0",
                                "/description[at0001]",
                                "/items[openEHR-EHR-CLUSTER.test_all_types.v1]",
                                "0",
                                "/items[at0001]",
                                "0",
                                "/items[at0002]",
                                "0",
                                "/items[at0004]",
                                "0",
                                "/value,value")))
                .isEqualTo(2);
    }

    @Test
    public void compact() {
        assertThat(IterativeNode.compact(List.of())).isEmpty();
        assertThat(IterativeNode.compact(List.of("0", "1", "2", "3"))).isEmpty();

        assertThat(IterativeNode.compact(
                        List.of("/composition", "/content", "0", "1", "2", "/events", "/activities", "value")))
                .containsExactly("content", "value");

        assertThat(IterativeNode.compact(List.of(
                        "/composition[openEHR-EHR-COMPOSITION.health_summary.v1]",
                        "/content[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                        "0")))
                .containsExactly("content[openEHR-EHR-ACTION.immunisation_procedure.v1]");

        assertThat(IterativeNode.compact(List.of(
                        "/composition[openEHR-EHR-COMPOSITION.health_summary.v1]",
                        "/content[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                        "0",
                        "/description[at0001]",
                        "/items[at0002]",
                        "0",
                        "/value,value")))
                .containsExactly(
                        "content[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                        "description[at0001]",
                        "items[at0002]",
                        "value,value");

        assertThat(IterativeNode.compact(List.of(
                        "/composition[openEHR-EHR-COMPOSITION.health_summary.v1]",
                        "/content[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                        "0",
                        "/description[at0001]",
                        "/items[openEHR-EHR-CLUSTER.test_all_types.v1]",
                        "0",
                        "/items[at0001]",
                        "0",
                        "/items[at0002]",
                        "0",
                        "/items[at0004]",
                        "0",
                        "/value,value")))
                .containsExactly(
                        "content[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                        "description[at0001]",
                        "items[openEHR-EHR-CLUSTER.test_all_types.v1]",
                        "items[at0001]",
                        "items[at0002]",
                        "items[at0004]",
                        "value,value");

        assertThat(IterativeNode.compact(List.of(
                        "/composition[openEHR-EHR-COMPOSITION.report.v1]",
                        "/content[openEHR-EHR-SECTION.procedures_rcp.v1]",
                        "0",
                        "/items[openEHR-EHR-SECTION.adhoc.v1]",
                        "0",
                        "/items[openEHR-EHR-ACTION.procedure.v1]",
                        "0",
                        "/description[at0001]",
                        "/items[at0002]",
                        "0",
                        "/name,0,value")))
                .containsExactly(
                        "content[openEHR-EHR-SECTION.procedures_rcp.v1]",
                        "items[openEHR-EHR-SECTION.adhoc.v1]",
                        "items[openEHR-EHR-ACTION.procedure.v1]",
                        "description[at0001]",
                        "items[at0002]",
                        "name,0,value");

        assertThat(IterativeNode.compact(List.of(
                        "/composition[openEHR-EHR-COMPOSITION.minimal.v1]",
                        "/content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]",
                        "$AQL_NODE_NAME_PREDICATE$",
                        "'Blood pressure (Training sample)'",
                        "0",
                        "/data[at0001]",
                        "$AQL_NODE_NAME_PREDICATE$",
                        "'history'",
                        "/events",
                        "/events[at0002]",
                        "0",
                        "/data[at0003]",
                        "/items[at0004]",
                        "$AQL_NODE_NAME_PREDICATE$",
                        "'Systolic'",
                        "0",
                        "/value,magnitude")))
                .containsExactly(
                        "content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]",
                        "$AQL_NODE_NAME_PREDICATE$",
                        "'Blood pressure (Training sample)'",
                        "data[at0001]",
                        "$AQL_NODE_NAME_PREDICATE$",
                        "'history'",
                        "events[at0002]",
                        "data[at0003]",
                        "items[at0004]",
                        "$AQL_NODE_NAME_PREDICATE$",
                        "'Systolic'",
                        "value,magnitude");
    }
}
