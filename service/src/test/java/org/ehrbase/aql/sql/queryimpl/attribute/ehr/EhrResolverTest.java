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
package org.ehrbase.aql.sql.queryimpl.attribute.ehr;

import static org.junit.Assert.*;

import org.junit.Test;

public class EhrResolverTest {

    @Test
    public void testIsEhrAttribute() {

        assertTrue(EhrResolver.isEhrAttribute("ehr_id"));
        assertTrue(EhrResolver.isEhrAttribute("ehr_status"));
        assertTrue(EhrResolver.isEhrAttribute("system_id"));
        assertTrue(EhrResolver.isEhrAttribute("time_created/value"));

        assertFalse(EhrResolver.isEhrAttribute("time_created/time"));
    }
}
