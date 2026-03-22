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
package org.ehrbase.rest.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.ViewCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class QueryControllerTest {

    private final ViewCatalogService mockViewCatalog = mock();
    private final RequestContext mockRequestContext = mock();

    private final QueryController controller = new QueryController(mockViewCatalog, mockRequestContext);

    @Test
    void listViewsReturnsEntries() {
        when(mockRequestContext.getTenantId()).thenReturn((short) 1);
        var entry = new ViewCatalogService.ViewCatalogEntry(
                UUID.randomUUID(), "v_blood_pressure", "ehr_views", "template", "blood_pressure.v1", "generated",
                "Blood pressure view", false);
        when(mockViewCatalog.listViews(null, (short) 1)).thenReturn(List.of(entry));

        ResponseEntity<List<ViewCatalogService.ViewCatalogEntry>> response = controller.listViews();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().viewName()).isEqualTo("v_blood_pressure");
    }

    @Test
    void listViewsReturnsEmptyList() {
        when(mockRequestContext.getTenantId()).thenReturn((short) 1);
        when(mockViewCatalog.listViews(null, (short) 1)).thenReturn(List.of());

        ResponseEntity<List<ViewCatalogService.ViewCatalogEntry>> response = controller.listViews();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }
}
