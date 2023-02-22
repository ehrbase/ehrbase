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
package org.ehrbase.dao.access.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class StoredQueryQualifiedNameTest {

    @Test
    void testFullName() {
        String name = "org.example.departmentx.test::diabetes-patient-overview";
        SemVer version = SemVer.parse("1.0.2");

        StoredQueryQualifiedName storedQueryQualifiedName = new StoredQueryQualifiedName(name, version);

        assertNotNull(storedQueryQualifiedName);

        assertEquals("org.example.departmentx.test", storedQueryQualifiedName.reverseDomainName());
        assertEquals("diabetes-patient-overview", storedQueryQualifiedName.semanticId());
        assertEquals("1.0.2", storedQueryQualifiedName.semVer().toVersionString());
    }

    @Test
    void testIncompleteName() {
        String name = "org.example.departmentx.test::diabetes-patient-overview";

        StoredQueryQualifiedName storedQueryQualifiedName = new StoredQueryQualifiedName(name, null);

        assertNotNull(storedQueryQualifiedName);

        assertEquals("org.example.departmentx.test", storedQueryQualifiedName.reverseDomainName());
        assertEquals("diabetes-patient-overview", storedQueryQualifiedName.semanticId());
        assertNull(storedQueryQualifiedName.semVer());
    }

    @Test
    void testBadlyFormedName() {
        String name = "org.example.departmentx.test";
        SemVer version = SemVer.parse("");

        assertThrows(IllegalArgumentException.class, () -> {
            new StoredQueryQualifiedName(name, version);
        });
    }

    @Test
    void testToString() {
        assertThat(new StoredQueryQualifiedName(
                        "org.example.departmentx.test::diabetes-patient-overview", SemVer.NO_VERSION))
                .hasToString("org.example.departmentx.test::diabetes-patient-overview");
        assertThat(new StoredQueryQualifiedName(
                        "org.example.departmentx.test::diabetes-patient-overview", SemVer.parse("1.2")))
                .hasToString("org.example.departmentx.test::diabetes-patient-overview/1.2");
    }
}
