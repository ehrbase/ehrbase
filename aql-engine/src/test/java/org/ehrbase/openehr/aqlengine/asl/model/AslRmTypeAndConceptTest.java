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
package org.ehrbase.openehr.aqlengine.asl.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AslRmTypeAndConceptTest {
    @Test
    void fromArchetypeNodeId() {

        assertThat(AslRmTypeAndConcept.fromArchetypeNodeId("openEHR-EHR-OBSERVATION.symptom_sign_screening.v0"))
                .isEqualTo(new AslRmTypeAndConcept("OB", ".symptom_sign_screening.v0"));
        assertThat(AslRmTypeAndConcept.fromArchetypeNodeId("at123")).isEqualTo(new AslRmTypeAndConcept(null, "at123"));
        assertThat(AslRmTypeAndConcept.fromArchetypeNodeId("id123")).isEqualTo(new AslRmTypeAndConcept(null, "id123"));
        assertThrows(
                IllegalArgumentException.class,
                () -> AslRmTypeAndConcept.fromArchetypeNodeId("openEHR-EHR-OBSERVATION"));
        assertThrows(IllegalArgumentException.class, () -> AslRmTypeAndConcept.fromArchetypeNodeId("nr123"));
    }

    @Test
    void toEntityConcept() {
        assertThat(AslRmTypeAndConcept.toEntityConcept("openEHR-EHR-OBSERVATION.symptom_sign_screening.v0"))
                .isEqualTo(".symptom_sign_screening.v0");
        assertThat(AslRmTypeAndConcept.toEntityConcept("at123")).isEqualTo("at123");
        assertThat(AslRmTypeAndConcept.toEntityConcept("id123")).isEqualTo("id123");
        assertThrows(
                IllegalArgumentException.class, () -> AslRmTypeAndConcept.toEntityConcept("openEHR-EHR-OBSERVATION"));
        assertThrows(IllegalArgumentException.class, () -> AslRmTypeAndConcept.toEntityConcept("nr123"));
    }
}
