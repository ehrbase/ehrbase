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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AncestorStructureRmTypeTest {

    @Test
    void entry() {
        assertThat(AncestorStructureRmType.ENTRY.getNonStructureDescendants()).isEmpty();
        assertThat(AncestorStructureRmType.ENTRY.getDescendants())
                .containsAll(AncestorStructureRmType.CARE_ENTRY.getDescendants());
    }

    @Test
    void item() {
        assertThat(AncestorStructureRmType.ITEM.getNonStructureDescendants()).isEmpty();
        assertThat(AncestorStructureRmType.ITEM.getDescendants())
                .containsExactlyInAnyOrder(StructureRmType.CLUSTER, StructureRmType.ELEMENT);
    }

    @ParameterizedTest
    @EnumSource(AncestorStructureRmType.class)
    void byTypeName(AncestorStructureRmType type) {

        assertThat(AncestorStructureRmType.byTypeName(type.name())).contains(type);
    }
}
