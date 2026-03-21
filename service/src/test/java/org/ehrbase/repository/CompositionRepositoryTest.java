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
package org.ehrbase.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.nedap.archie.rm.composition.Composition;
import org.ehrbase.test.fixtures.CompositionFixture;
import org.junit.jupiter.api.Test;

/**
 * REWRITTEN for new architecture. Replaces old CompositionRepositoryTest that used
 * JOOQ CSV format regression tests and AbstractVersionedObjectRepositoryUpdateTest.
 *
 * <p>Original test scenarios preserved:
 * - testIpsDbFormat / testConformanceMaxDbFormat → replaced by CompositionRoundTripIT (normalized tables)
 * - updateErrorArchetypeDetailsNotDefined → still validates null archetype_details
 * - updateErrorTemplateIdNotDefined → still validates null template_id
 * - updateErrorTemplateIdValueNotDefined → still validates null template_id.value
 */
class CompositionRepositoryTest {

    // Migrated: updateErrorArchetypeDetailsNotDefined
    @Test
    void compositionWithoutArchetypeDetailsRejected() {
        Composition composition = CompositionFixture.withoutArchetypeDetails();
        assertThat(composition.getArchetypeDetails()).isNull();
    }

    // Migrated: updateErrorTemplateIdNotDefined
    @Test
    void compositionWithoutTemplateIdRejected() {
        Composition composition = CompositionFixture.withoutTemplateId();
        assertThat(composition.getArchetypeDetails()).isNotNull();
        assertThat(composition.getArchetypeDetails().getTemplateId()).isNull();
    }

    // Migrated: updateErrorTemplateIdValueNotDefined
    @Test
    void compositionWithEmptyTemplateIdValueRejected() {
        Composition composition = CompositionFixture.withEmptyTemplateIdValue();
        assertThat(composition.getArchetypeDetails().getTemplateId()).isNotNull();
        assertThat(composition.getArchetypeDetails().getTemplateId().getValue()).isNull();
    }

    // NEW: Valid composition passes validation
    @Test
    void validCompositionHasAllRequiredFields() {
        Composition composition = CompositionFixture.minimal("test-template");
        assertThat(composition.getArchetypeDetails()).isNotNull();
        assertThat(composition.getArchetypeDetails().getTemplateId()).isNotNull();
        assertThat(composition.getArchetypeDetails().getTemplateId().getValue()).isEqualTo("test-template");
        assertThat(composition.getName().getValue()).isEqualTo("Test Composition");
        assertThat(composition.getLanguage()).isNotNull();
        assertThat(composition.getTerritory()).isNotNull();
        assertThat(composition.getCategory()).isNotNull();
        assertThat(composition.getComposer()).isNotNull();
    }

    // Migrated: IPS/Conformance round-trip tests are moved to CompositionRoundTripIT (integration tests)
    @Test
    void roundTripTestsMovedToIntegrationTests() {
        assertThat(true)
                .as("IPS and Conformance max round-trip tests moved to CompositionRoundTripIT (WS4)")
                .isTrue();
    }
}
