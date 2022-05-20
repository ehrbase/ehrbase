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
package org.ehrbase.aql.sql.queryimpl.attribute.eventcontext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OtherContextPredicateTest {

    @Test
    void testPath() {
        String queryPath = "context/other_context";
        assertEquals("context/other_context", new OtherContextPredicate(queryPath).adjustForQuery());

        queryPath = "context/other_context[at0001]";
        assertEquals("context/other_context", new OtherContextPredicate(queryPath).adjustForQuery());

        queryPath = "context/other_context[at0001]/items[at0003]/value/value";
        assertEquals(
                "context/other_context/items[at0003]/value/value",
                new OtherContextPredicate(queryPath).adjustForQuery());
    }
}
