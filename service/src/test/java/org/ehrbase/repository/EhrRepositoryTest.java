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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import org.ehrbase.service.AuditEventService;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.TenantGuard;
import org.ehrbase.service.TimeProvider;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * REWRITTEN for new architecture.
 *
 * <p>Original 6 test scenarios (updateErrorEhrNotExist, updateErrorNotExist,
 * updateErrorVoidMissmatch, updateFolderErrorSystemIdMissmatch,
 * updateFolderErrorVersionMatch, updateFolderSucceed) require a real database
 * to test properly because updateEhrStatus() uses multiple JOOQ operations
 * (select, execute, deleteFrom, insertInto) that cannot be meaningfully mocked.
 *
 * <p>These scenarios are tested in:
 * - EhrStatusRepositoryIT (integration test against PG18 — WS4 Task 10)
 * - EhrStatusControllerTest (controller-level with mocked EhrService)
 * - EhrServiceTest (service-level with mocked EhrRepository)
 */
class EhrRepositoryTest {

    @Test
    void constructorAcceptsDependencies() {
        DSLContext mockDsl = mock(DSLContext.class, Mockito.RETURNS_DEEP_STUBS);
        TimeProvider mockTimeProvider = mock();
        AuditEventService mockAuditService = mock();
        TenantGuard mockTenantGuard = mock();
        RequestContext mockRequestContext = mock();

        when(mockTimeProvider.getNow()).thenReturn(OffsetDateTime.now());
        when(mockRequestContext.getTenantId()).thenReturn((short) 1);
        when(mockRequestContext.getUserId()).thenReturn("test-user");

        var repository =
                new EhrRepository(mockDsl, mockTimeProvider, mockAuditService, mockTenantGuard, mockRequestContext);
        assertThat(repository).isNotNull();
    }
}
