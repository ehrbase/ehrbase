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

import static org.junit.Assert.*;

import org.junit.Test;

public class VariableAqlPathTest {

    @Test
    public void getSuffix() {

        String aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/value";

        assertEquals("value", new VariableAqlPath(aPath).getSuffix());
    }

    @Test
    public void getInfix() {

        String aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/value";

        assertEquals("data[at0001]/events[at0002]/data[at0003]/items[at0007]", new VariableAqlPath(aPath).getInfix());
    }

    @Test
    public void testIsPartialAqlDataValuePath() {
        String aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/value";

        assertTrue(new VariableAqlPath(aPath).isPartialAqlDataValuePath());

        aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/value/magnitude";

        assertFalse(new VariableAqlPath(aPath).isPartialAqlDataValuePath());

        aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/name";

        assertTrue(new VariableAqlPath(aPath).isPartialAqlDataValuePath());

        aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/time";

        assertTrue(new VariableAqlPath(aPath).isPartialAqlDataValuePath());
    }
}
