/*
 * Copyright (c) 2026 vitasystems GmbH.
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.util.SemVer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompositionServiceImpTest {

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            a,a,true
            a,b,false
            a.v1,b.v1,false
            a.en,b.en,false
            a.en.v1,b.en.v1,false
            a.de,a.en,true
            a.de-ch,a.en-gb,true
            a.en,a,true
            a.v1,a,true
            a,a.v1,false
            a.v1,a.v1,true
            a.v1.0,a.v1,true
            a.v0,a.v1,false
            a.en.v1,a.de-ch,true
            a.en,a.de-ch.v1,false
            a.en.v1,a.de-ch.v1.0,false
            a.en.v1.0,a.de-ch.v1,true
            patient.vitals,patient.vitals.v2,false
            patient.vitals.v2,patient.vitals,true
        """)
    void ensureTemplateCompatible(String newTemplateId, String existingTemplateId, boolean compatible) {
        if (compatible) {
            CompositionServiceImp.ensureTemplateCompatible(newTemplateId, existingTemplateId);
        } else {
            assertThatThrownBy(() -> CompositionServiceImp.ensureTemplateCompatible(newTemplateId, existingTemplateId))
                    .isInstanceOf(InvalidApiParameterException.class);
        }
    }

    @Test
    void isOlder() {
        SemVer[] sortedVersions = Stream.of("", "0", "0.0", "0.0.0", "0.0.12", "1", "1.0", "1.0.0", "12", "12.34.56")
                .map(SemVer::parse)
                .toArray(SemVer[]::new);

        for (int b = 0; b < sortedVersions.length; b++)
            for (int v = 0; v < sortedVersions.length; v++) {
                SemVer version = sortedVersions[v];
                SemVer baseVersion = sortedVersions[b];
                boolean older = v < b;
                assertThat(CompositionServiceImp.isOlder(version, baseVersion)).isEqualTo(older);
            }
    }
}
