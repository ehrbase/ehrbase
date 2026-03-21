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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for MaterializedViewRefreshService.
 */
class MaterializedViewRefreshServiceTest {

    private final DSLContext mockDsl = mock(DSLContext.class, Mockito.RETURNS_DEEP_STUBS);
    private final AuditEventService mockAuditService = mock();
    private MaterializedViewRefreshService service;

    @BeforeEach
    void setUp() {
        service = new MaterializedViewRefreshService(mockDsl, mockAuditService);
    }

    @Test
    void constructorAcceptsDependencies() {
        assertThat(service).isNotNull();
    }

    @Test
    void refreshViewSucceeds() {
        boolean result = service.refreshView("ehr_views", "mv_compliance_dashboard");
        assertThat(result).isTrue();
        verify(mockDsl).execute("REFRESH MATERIALIZED VIEW CONCURRENTLY ehr_views.mv_compliance_dashboard");
    }

    @Test
    void refreshViewFailsGracefully() {
        when(mockDsl.execute(anyString())).thenThrow(new RuntimeException("View does not exist"));

        boolean result = service.refreshView("ehr_views", "nonexistent_view");
        assertThat(result).isFalse();
    }

    @Test
    void refreshViewBuildsCorrectFqn() {
        service.refreshView("ehr_views", "mv_test_view");
        verify(mockDsl).execute("REFRESH MATERIALIZED VIEW CONCURRENTLY ehr_views.mv_test_view");
    }
}
