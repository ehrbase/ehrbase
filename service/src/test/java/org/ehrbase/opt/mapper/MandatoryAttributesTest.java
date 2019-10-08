/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.opt.mapper;

import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MandatoryAttributesTest {

    @Test
    public void testToMapDV_TEXT() {
        MandatoryAttributes cut = new MandatoryAttributes("DV_TEXT");

        Map<String, Object> actual = cut.toMap();
        assertThat(actual).size().isEqualTo(1);
        Map<String, Object> value = (Map<String, Object>) actual.get("value");
        assertThat(value).size().isEqualTo(1);
        assertThat(value).containsEntry("type", "STRING");

    }

    @Test
    public void testToMapDV_COMPOSITION() {
        MandatoryAttributes cut = new MandatoryAttributes("COMPOSITION");

        Map<String, Object> actual = cut.toMap();
        assertThat(actual).size().isEqualTo(4);
        assertThat(actual).containsKeys("composer", "language", "category", "territory");
        actual.values().forEach
                (
                        e -> assertThat(((Map) e)).size().isEqualTo(3)
                );

    }
}