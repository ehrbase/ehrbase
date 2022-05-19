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
package org.ehrbase.aql.compiler;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class WhereClauseUtilTest {

    @Test
    public void testIsBalancedBlocks() {
        List<Object> expression =
                Arrays.asList(("a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/magnitude,>,38,"
                                + "AND,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',"
                                + "AND,e::ehr_id/value,IN,(,'849bf097-bd16-44fc-a394-10676284a012',,,'34b2e263-00eb-40b8-88f1-823c87096457',),"
                                + "OR,(,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',"
                                + "AND, a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',)")
                        .split(","));

        assertTrue(new WhereClauseUtil(expression).isBalancedBlocks());

        expression = Arrays.asList(("a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/magnitude,>,38,"
                        + "AND,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',"
                        + "AND,e::ehr_id/value,IN,'849bf097-bd16-44fc-a394-10676284a012',,,'34b2e263-00eb-40b8-88f1-823c87096457',),"
                        + "OR,(,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',"
                        + "AND, a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',)")
                .split(","));

        assertFalse(new WhereClauseUtil(expression).isBalancedBlocks());

        expression = Arrays.asList(("a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/magnitude,>,38,"
                        + "AND,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',"
                        + "AND,e::ehr_id/value,IN,(,'849bf097-bd16-44fc-a394-10676284a012',,,'34b2e263-00eb-40b8-88f1-823c87096457',"
                        + "OR,(,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',"
                        + "AND, a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',)")
                .split(","));

        assertFalse(new WhereClauseUtil(expression).isBalancedBlocks());
    }
}
