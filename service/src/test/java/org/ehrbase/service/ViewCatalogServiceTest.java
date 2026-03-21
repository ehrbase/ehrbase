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
import static org.mockito.Mockito.mock;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for ViewCatalogService.
 * Integration tests (ViewCatalogServiceIT) require real PG18 — see WS4 Task 11.
 */
class ViewCatalogServiceTest {

    @Test
    void constructorAcceptsDslContext() {
        DSLContext mockDsl = mock(DSLContext.class, Mockito.RETURNS_DEEP_STUBS);
        var service = new ViewCatalogService(mockDsl);
        assertThat(service).isNotNull();
    }

    @Test
    void viewCatalogEntryRecord() {
        var entry = new ViewCatalogService.ViewCatalogEntry(
                java.util.UUID.randomUUID(),
                "v_blood_pressure",
                "ehr_views",
                "template",
                "ehrbase_blood_pressure_simple.de.v0",
                "auto",
                "Current data view for blood pressure template",
                false);

        assertThat(entry.viewName()).isEqualTo("v_blood_pressure");
        assertThat(entry.viewSchema()).isEqualTo("ehr_views");
        assertThat(entry.viewType()).isEqualTo("template");
        assertThat(entry.templateId()).isEqualTo("ehrbase_blood_pressure_simple.de.v0");
        assertThat(entry.source()).isEqualTo("auto");
        assertThat(entry.isMaterialized()).isFalse();
    }

    @Test
    void viewCatalogEntryMaterialized() {
        var entry = new ViewCatalogService.ViewCatalogEntry(
                java.util.UUID.randomUUID(),
                "mv_compliance_dashboard",
                "ehr_views",
                "compliance",
                null,
                "auto",
                "Compliance KPI dashboard",
                true);

        assertThat(entry.isMaterialized()).isTrue();
        assertThat(entry.templateId()).isNull();
    }
}
