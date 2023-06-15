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
package org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.time;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import org.junit.jupiter.api.Test;

public class StandardPartialTimeTest {

    @Test
    public void testPartial1() {

        DvTime dvTime = new DvTime("10:10+02:00");
        assertTrue(new StandardPartialTime(dvTime).isNonCompactIS8601Representation());
        assertTrue(new StandardPartialTime(dvTime).ishhmm());
        assertFalse(new StandardPartialTime(dvTime).ishhmmssfff());
    }
}
