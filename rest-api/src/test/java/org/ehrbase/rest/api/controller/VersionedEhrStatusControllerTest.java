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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.RevisionHistory;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.service.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

/**
 * Migrated from OpenehrVersionedEhrStatusControllerTest (10 tests).
 */
class VersionedEhrStatusControllerTest {

    private static final UUID EHR_ID = UUID.fromString("7d44b88c-4199-4bad-97dc-d78268e01398");
    private static final String SYSTEM_ID = "test.versioned-ehr-status.controller";

    private final EhrService mockEhrService = mock();
    private final RequestContext mockRequestContext = mock();

    private final VersionedEhrStatusController controller =
            new VersionedEhrStatusController(mockEhrService, mockRequestContext);

    @BeforeEach
    void setUp() {
        Mockito.reset(mockEhrService, mockRequestContext);
    }

    // Migrated: retrieveVersionedEhrStatusByEhrErrorEhrUUID
    @Test
    void getVersionedEhrStatusInvalidEhrId() {
        assertThatThrownBy(() -> controller.getVersionedEhrStatus("not-a-uuid"))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    // Migrated: retrieveVersionedEhrStatusByEhr
    @Test
    void getVersionedEhrStatus() {
        var vs = mock(VersionedEhrStatus.class);
        when(mockEhrService.getVersionedEhrStatus(EHR_ID)).thenReturn(vs);

        var response = controller.getVersionedEhrStatus(EHR_ID.toString());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vs);
    }

    // Migrated: retrieveVersionedEhrStatusByEhr — EHR not found
    @Test
    void getVersionedEhrStatusEhrNotFound() {
        doThrow(new ObjectNotFoundException("ehr", "No EHR found"))
                .when(mockEhrService)
                .checkEhrExists(EHR_ID);

        assertThatThrownBy(() -> controller.getVersionedEhrStatus(EHR_ID.toString()))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    // Migrated: retrieveVersionedEhrStatusRevisionHistoryByEhr
    @Test
    void getRevisionHistory() {
        var history = mock(RevisionHistory.class);
        when(mockEhrService.getRevisionHistoryOfVersionedEhrStatus(EHR_ID)).thenReturn(history);

        var response = controller.getRevisionHistory(EHR_ID.toString());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(history);
    }

    // Migrated: retrieveVersionOfEhrStatusByVersionUid
    @SuppressWarnings("unchecked")
    @Test
    void getVersionByUid() {
        UUID statusId = UUID.randomUUID();
        String versionUid = statusId + "::" + SYSTEM_ID + "::1";
        OriginalVersion<EhrStatus> ov = mock(OriginalVersion.class);
        when(mockEhrService.getEhrStatusAtVersion(eq(EHR_ID), eq(statusId), eq(1)))
                .thenReturn(Optional.of(ov));

        var response = controller.getVersionByUid(EHR_ID.toString(), versionUid);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(ov);
    }

    // Migrated: retrieveVersionOfEhrStatusByVersionUidErrorVersionId
    @Test
    void getVersionByUidInvalidFormat() {
        assertThatThrownBy(() -> controller.getVersionByUid(EHR_ID.toString(), "not-a-version-uid"))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    // Migrated: retrieveVersionOfEhrStatusByVersionUid — not found
    @Test
    void getVersionByUidNotFound() {
        UUID statusId = UUID.randomUUID();
        String versionUid = statusId + "::" + SYSTEM_ID + "::99";
        when(mockEhrService.getEhrStatusAtVersion(eq(EHR_ID), eq(statusId), eq(99)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getVersionByUid(EHR_ID.toString(), versionUid))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    // Migrated: retrieveVersionOfEhrStatusByVersionUidErrorEhrUUID
    @Test
    void getVersionByUidInvalidEhrId() {
        assertThatThrownBy(() -> controller.getVersionByUid("not-a-uuid", "whatever"))
                .isInstanceOf(InvalidApiParameterException.class);
    }
}
