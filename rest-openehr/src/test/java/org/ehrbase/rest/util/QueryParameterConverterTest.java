/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.rest.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.ehrbase.rest.RestModuleConfiguration;
import org.junit.jupiter.api.Test;

public class QueryParameterConverterTest {

    private RestModuleConfiguration.QueryParameterConverter converter() {
        return new RestModuleConfiguration.QueryParameterConverter();
    }

    @Test
    void convertEmpty() {

        Map<String, Object> map = converter().convert("");
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @Test
    void convertSingleParameter() {

        Map<String, Object> map = converter().convert("param=foo");
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("foo", map.get("param"));
    }

    @Test
    void convertSingleParameterEmpty() {

        Map<String, Object> map = converter().convert("param=");
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("", map.get("param"));
    }

    @Test
    void convertSingleParameterOnly() {

        Map<String, Object> map = converter().convert("invalid");
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("invalid"));
        assertEquals("", map.get("invalid"));
    }

    @Test
    void convertMultipleParameter() {

        Map<String, Object> map = converter().convert("param=foo&other=");
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("foo", map.get("param"));
        assertEquals("", map.get("other"));
    }
}
