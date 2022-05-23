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
package org.ehrbase.ehr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LocatableHelperTest {

    @Test
    void testDividePathIntoSegments() {
        List<String> pathSegments =
                LocatableHelper.dividePathIntoSegments("context/other_context[at0001]/items[at0003]/value/value");
        assertEquals(5, pathSegments.size());

        pathSegments = LocatableHelper.dividePathIntoSegments(
                "content[openEHR-EHR-SECTION.adhoc.v1 and name/value='Medication Summary']/items[openEHR-EHR-ACTION.medication.v1]/description[at0017]/items[openEHR-EHR-CLUSTER.medication.v1]/items[at0152]");
        assertEquals(5, pathSegments.size());

        pathSegments = LocatableHelper.dividePathIntoSegments(
                "/content[openEHR-EHR-SECTION.adhoc.v1 and name/value='Advanced Directives']/items[openEHR-EHR-EVALUATION.limitation_of_treatment.v0]/data[at0001]/items[at0006]");
        assertEquals(4, pathSegments.size());
    }
}
