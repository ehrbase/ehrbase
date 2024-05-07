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
package org.ehrbase.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class StoredQueryQualifiedNameTest {

    @Test
    void withFullName() {
        String name = "org.example.departmentx.test::diabetes-patient-overview";
        SemVer version = SemVer.parse("1.0.2");

        StoredQueryQualifiedName storedQueryQualifiedName = StoredQueryQualifiedName.create(name, version);

        assertNotNull(storedQueryQualifiedName);
        assertEquals(version, storedQueryQualifiedName.semVer());

        assertEquals("org.example.departmentx.test", storedQueryQualifiedName.reverseDomainName());
        assertEquals("diabetes-patient-overview", storedQueryQualifiedName.semanticId());
        assertEquals("1.0.2", storedQueryQualifiedName.semVer().toVersionString());
        assertEquals("org.example.departmentx.test::diabetes-patient-overview", storedQueryQualifiedName.toName());
        assertEquals(
                "org.example.departmentx.test::diabetes-patient-overview/1.0.2",
                storedQueryQualifiedName.toQualifiedNameString());
        assertEquals(
                "org.example.departmentx.test::diabetes-patient-overview/1.0.2", storedQueryQualifiedName.toString());
    }

    @Test
    void withIncompleteName() {
        String name = "org.example.departmentx.test::diabetes-patient-overview";

        StoredQueryQualifiedName storedQueryQualifiedName = StoredQueryQualifiedName.create(name, SemVer.NO_VERSION);

        assertNotNull(storedQueryQualifiedName);
        assertSame(SemVer.NO_VERSION, storedQueryQualifiedName.semVer());

        assertEquals("org.example.departmentx.test", storedQueryQualifiedName.reverseDomainName());
        assertEquals("diabetes-patient-overview", storedQueryQualifiedName.semanticId());
        assertEquals("", storedQueryQualifiedName.semVer().toVersionString());
        assertEquals("org.example.departmentx.test::diabetes-patient-overview", storedQueryQualifiedName.toName());
        assertEquals(
                "org.example.departmentx.test::diabetes-patient-overview",
                storedQueryQualifiedName.toQualifiedNameString());
        assertEquals("org.example.departmentx.test::diabetes-patient-overview", storedQueryQualifiedName.toString());
    }

    @Test
    void withBadlyFormedName() {
        String name = "org.example.departmentx.test";
        SemVer version = SemVer.parse("");

        assertThrows(IllegalArgumentException.class, () -> StoredQueryQualifiedName.create(name, version));
    }

    @Test
    void testToString() {

        assertThat(StoredQueryQualifiedName.create(
                        "org.example.departmentx.test::diabetes-patient-overview", SemVer.NO_VERSION))
                .hasToString("org.example.departmentx.test::diabetes-patient-overview");
        assertThat(StoredQueryQualifiedName.create(
                        "org.example.departmentx.test::diabetes-patient-overview", SemVer.parse("1.2")))
                .hasToString("org.example.departmentx.test::diabetes-patient-overview/1.2");
    }
}
