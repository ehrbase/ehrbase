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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RmTypeAliasTest {

    @Test
    void checkStructureAliases() {
        Arrays.stream(StructureRmType.values())
                .forEach(v -> assertThat(RmTypeAlias.getAlias(v.name())).isEqualTo(v.getAlias()));

        Set<String> typesWithAliases =
                RmTypeAlias.values.stream().map(RmTypeAlias::type).collect(Collectors.toSet());

        typesWithAliases.forEach(t -> assertThatThrownBy(() -> RmTypeAlias.getRmType(t))
                .withFailMessage(() -> "Alias name clashes with an existing type: " + t)
                .isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void checkSwitchCompleteness() {
        RmTypeAlias.values.forEach(rta -> {
            assertThat(RmTypeAlias.getAlias(rta.type())).isEqualTo(rta.alias());
            assertThat(RmTypeAlias.getRmType(rta.alias())).isEqualTo(rta.type());
        });
    }
}
