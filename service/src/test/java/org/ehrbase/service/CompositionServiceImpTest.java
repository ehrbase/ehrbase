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
package org.ehrbase.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * ADAPTED for new architecture.
 *
 * <p>Original tests:
 * - ensureTemplateCompatible (21 parameterized cases) — static method removed from new CompositionServiceImp.
 *   Template compatibility is now handled at schema generation time (SchemaGenerator validates on upload).
 * - isOlder (SemVer matrix test) — static method removed. SemVer comparison still tested in SemVerTest.
 *
 * <p>These scenarios are covered by:
 * - SchemaGeneratorTest (template compatibility at upload time)
 * - SemVerTest / SemVerUtilTest (version comparison logic)
 */
class CompositionServiceImpTest {

    @Disabled("ensureTemplateCompatible removed — template compatibility checked at schema generation time")
    @Test
    void ensureTemplateCompatible() {
        assertThat(true).as("See SchemaGeneratorTest for template compatibility validation").isTrue();
    }

    @Disabled("isOlder removed — version comparison still tested in SemVerTest")
    @Test
    void isOlder() {
        assertThat(true).as("See SemVerTest for SemVer comparison tests").isTrue();
    }
}
