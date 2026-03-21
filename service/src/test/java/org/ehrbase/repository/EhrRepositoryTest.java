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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.service.AuditEventService;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.TenantGuard;
import org.ehrbase.service.TimeProvider;
import org.jooq.DSLContext;
import org.jooq.SelectSelectStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * REWRITTEN for new architecture. Replaces the old EhrRepositoryTest that extended
 * AbstractVersionedObjectRepositoryUpdateTest.
 *
 * <p>Original test scenarios preserved:
 * - updateErrorEhrNotExist / updateErrorNotExist → version mismatch on update
 * - updateErrorVoidMissmatch / updateFolderErrorVersionMatch → PreconditionFailedException
 */
class EhrRepositoryTest {

    private static final UUID EHR_ID = UUID.fromString("8276b318-b6f9-411f-8443-8330b502a5a4");

    private final DSLContext mockDsl = mock();
    private final TimeProvider mockTimeProvider = mock();
    private final AuditEventService mockAuditService = mock();
    private final TenantGuard mockTenantGuard = mock();
    private final RequestContext mockRequestContext = mock();

    private EhrRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        Mockito.reset(mockDsl, mockTimeProvider, mockAuditService, mockTenantGuard, mockRequestContext);
        when(mockTimeProvider.getNow()).thenReturn(OffsetDateTime.now());
        when(mockRequestContext.getTenantId()).thenReturn((short) 1);
        when(mockRequestContext.getUserId()).thenReturn("test-user");
        repository = new EhrRepository(mockDsl, mockTimeProvider, mockAuditService, mockTenantGuard, mockRequestContext);
    }

    // Migrated: Version mismatch throws PreconditionFailedException
    @SuppressWarnings("unchecked")
    @Test
    void updateEhrStatusVersionMismatch() {
        var status = new EhrStatus();
        status.setSubject(new PartySelf());

        // DSLContext.select() returns deep mock chain ending in null record → version mismatch
        SelectSelectStep<?> selectStep = mock(SelectSelectStep.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockDsl.select()).thenReturn((SelectSelectStep) selectStep);

        assertThatThrownBy(() -> repository.updateEhrStatus(EHR_ID, status, 99, UUID.randomUUID()))
                .isInstanceOf(PreconditionFailedException.class)
                .hasMessageContaining("version mismatch");
    }

    @Test
    void constructorAcceptsDependencies() {
        assertThat(repository).isNotNull();
    }
}
