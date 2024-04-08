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
package org.ehrbase.openehr.dbformat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RmAttributeAliasTest {

    @Test
    void checkAliases() {
        Set<String> attributes = RmAttributeAlias.VALUES.stream()
                .map(RmAttributeAlias::attribute)
                .collect(Collectors.toSet());

        attributes.forEach(a -> assertThatThrownBy(() -> RmAttributeAlias.getAttribute(a))
                .withFailMessage(() -> "Alias name clashes with an existing attribute " + a)
                .isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void rmToJsonPathParts() {
        assertThat(RmAttributeAlias.rmToJsonPathParts("archetype_details/template_id/value"))
                .isEqualTo(new String[] {"ad", "tm", "V"});
        assertThat(RmAttributeAlias.rmToJsonPathParts("subject/external_ref/id/value"))
                .isEqualTo(new String[] {"su", "er", "X", "V"});
    }
}
