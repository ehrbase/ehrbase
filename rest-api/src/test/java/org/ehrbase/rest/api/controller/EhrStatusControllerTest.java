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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.service.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Migrated from OpenehrEhrStatusControllerTest (6 tests) + new tests.
 */
class EhrStatusControllerTest {

    private static final UUID EHR_ID = UUID.fromString("7d44b88c-4199-4bad-97dc-d78268e01398");
    private static final String SYSTEM_ID = "test.ehr-status.controller";

    private final EhrService mockEhrService = mock();
    private final RequestContext mockRequestContext = mock();

    private final EhrStatusController spyController = spy(new EhrStatusController(mockEhrService, mockRequestContext));

    @BeforeEach
    void setUp() {
        Mockito.reset(mockEhrService, mockRequestContext, spyController);
        doReturn(URI.create("https://test/api/v1/ehrs/" + EHR_ID + "/ehr_status"))
                .when(spyController)
                .locationUri(any(String[].class));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private EhrStatus ehrStatus(String uid) {
        var status = new EhrStatus();
        status.setUid(new ObjectVersionId(uid, SYSTEM_ID, "1"));
        status.setSubject(new PartySelf());
        status.setArchetypeNodeId("openEHR-EHR-EHR_STATUS.generic.v1");
        status.setName(new com.nedap.archie.rm.datavalues.DvText("EHR Status"));
        status.setModifiable(true);
        status.setQueryable(true);
        return status;
    }

    // Migrated: getEhrStatusByVersionNotFound
    @Test
    void getEhrStatusByVersionNotFound() {
        String versionUid = "13a82993-a489-421a-ac88-5cec001bd58c::" + SYSTEM_ID + "::42";
        when(mockEhrService.getEhrStatusAtVersion(eq(EHR_ID), any(), eq(42))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spyController.getEhrStatusByVersion(EHR_ID.toString(), versionUid))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    // Migrated: getEhrStatusVersionByTimeLatest (null timestamp → latest)
    @Test
    void getEhrStatusLatest() {
        var status = ehrStatus(UUID.randomUUID().toString());
        when(mockEhrService.getEhrStatus(EHR_ID)).thenReturn(status);
        when(mockEhrService.getLatestVersionUidOfStatus(EHR_ID))
                .thenReturn(new ObjectVersionId(UUID.randomUUID().toString(), SYSTEM_ID, "1"));

        var response = spyController.getEhrStatus(EHR_ID.toString(), null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(status);
    }

    // Migrated: getEhrStatusByVersionIdInvalid (missing version → InvalidApiParameterException)
    @Test
    void getEhrStatusByVersionIdInvalid() {
        String invalidVersionUid = "13a82993-a489-421a-ac88-5cec001bd58c";
        assertThatThrownBy(() -> spyController.getEhrStatusByVersion(EHR_ID.toString(), invalidVersionUid))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    // Migrated: getEhrStatusByVersionId (valid → 200 with status)
    @Test
    void getEhrStatusByVersionId() {
        String versionUid = "13a82993-a489-421a-ac88-5cec001bd58c::" + SYSTEM_ID + "::1";
        var status = ehrStatus("13a82993-a489-421a-ac88-5cec001bd58c");

        @SuppressWarnings("unchecked")
        OriginalVersion<EhrStatus> ov = mock(OriginalVersion.class);
        when(ov.getData()).thenReturn(status);
        when(mockEhrService.getEhrStatusAtVersion(eq(EHR_ID), any(), eq(1))).thenReturn(Optional.of(ov));

        var response = spyController.getEhrStatusByVersion(EHR_ID.toString(), versionUid);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(status);
    }

    // Migrated: updateEhrStatus (3 prefer variations → 200/204)
    @ParameterizedTest
    @ValueSource(strings = {"", "return=minimal", "return=representation"})
    void updateEhrStatus(String prefer) {
        var status = ehrStatus(UUID.randomUUID().toString());
        String ifMatch = "\"" + UUID.randomUUID() + "::" + SYSTEM_ID + "::1\"";

        when(mockEhrService.updateStatus(eq(EHR_ID), any(), any(), any(), any()))
                .thenReturn(status);
        when(mockEhrService.getLatestVersionUidOfStatus(EHR_ID))
                .thenReturn(new ObjectVersionId(UUID.randomUUID().toString(), SYSTEM_ID, "2"));

        var response = spyController.updateEhrStatus(EHR_ID.toString(), status, ifMatch, prefer);

        if (prefer.equals("return=representation")) {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(status);
        } else {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();
        }
    }

    // NEW: EHR not found
    @Test
    void getEhrStatusEhrNotFound() {
        doThrow(new ObjectNotFoundException("ehr", "No EHR found"))
                .when(mockEhrService)
                .checkEhrExists(EHR_ID);

        assertThatThrownBy(() -> spyController.getEhrStatus(EHR_ID.toString(), null))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    // NEW: Invalid EHR ID
    @Test
    void getEhrStatusInvalidEhrId() {
        assertThatThrownBy(() -> spyController.getEhrStatus("not-a-uuid", null))
                .isInstanceOf(InvalidApiParameterException.class);
    }
}
