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
package org.ehrbase.jooq.dbencoding.wrappers.json.writer.translator_db2raw;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import org.junit.jupiter.api.Test;

public class GenericRmTypeTest {

    @Test
    public void testAddSpecializedType() {
        String dvIntervalExample = "{\n" + "  \"lower\": {\n"
                + "    \"value\": \"2019-11-22T00:00+01:00\",\n"
                + "    \"epoch_offset\": 1.5743772E9\n"
                + "  },\n"
                + "  \"upper\": {\n"
                + "    \"value\": \"2020-12-22T00:00+01:00\",\n"
                + "    \"epoch_offset\": 1.5743772E9\n"
                + "  },\n"
                + "  \"lowerUnbounded\": false,\n"
                + "  \"upperUnbounded\": false,\n"
                + "  \"lowerIncluded\": true,\n"
                + "  \"upperIncluded\": true,\n"
                + "  \"_type\": \"DV_INTERVAL\\u003cDV_DATE_TIME\\u003e\"\n"
                + "}";

        LinkedTreeMap linkedTreeMap = new GsonBuilder().create().fromJson(dvIntervalExample, LinkedTreeMap.class);

        LinkedTreeMap specializedMap =
                new GenericRmType((String) linkedTreeMap.get("_type")).inferSpecialization(linkedTreeMap);

        assertEquals(((LinkedTreeMap) specializedMap.get("lower")).get("_type"), "DV_DATE_TIME");
        assertEquals(((LinkedTreeMap) specializedMap.get("upper")).get("_type"), "DV_DATE_TIME");
    }
}
