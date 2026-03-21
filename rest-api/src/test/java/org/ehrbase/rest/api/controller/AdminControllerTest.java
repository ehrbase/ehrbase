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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.ehrbase.api.service.SystemService;
import org.ehrbase.service.MaterializedViewRefreshService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AdminControllerTest {

    private final DSLContext mockDsl = mock();
    private final SystemService mockSystemService = mock();
    private final MaterializedViewRefreshService mockMatViewService = mock();

    private final AdminController controller = new AdminController(mockDsl, mockSystemService, mockMatViewService);

    @Test
    void refreshSchemaCallsService() {
        var response = controller.refreshSchema();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "refreshed");
        verify(mockMatViewService).refreshAllMaterializedViews();
    }

    @Test
    void triggerMigrationNotImplemented() {
        assertThatThrownBy(() -> controller.triggerMigration()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void migrationStatusNotImplemented() {
        assertThatThrownBy(() -> controller.migrationStatus()).isInstanceOf(UnsupportedOperationException.class);
    }
}
