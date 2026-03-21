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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.rest.api.dto.EhrResponseDto;
import org.ehrbase.service.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;

class EhrControllerTest {

    private static final String SYSTEM_ID = "test.ehr.controller";

    private final EhrService mockEhrService = mock();
    private final SystemService mockSystemService = mock();
    private final RequestContext mockRequestContext = mock();

    private final EhrController spyController =
            spy(new EhrController(mockEhrService, mockSystemService, mockRequestContext));

    @BeforeEach
    void setUp() {
        Mockito.reset(mockEhrService, mockSystemService, mockRequestContext, spyController);
        when(mockSystemService.getSystemId()).thenReturn(SYSTEM_ID);
        doReturn(URI.create("https://test.ehr.controller/api/v1/ehrs"))
                .when(spyController)
                .locationUri(any(String[].class));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private EhrStatus ehrStatus() {
        var status = new EhrStatus();
        status.setUid(new ObjectVersionId(UUID.randomUUID().toString(), "system", "1"));
        status.setSubject(new PartySelf());
        status.setArchetypeNodeId("openEHR-EHR-EHR_STATUS.generic.v1");
        status.setName(new com.nedap.archie.rm.datavalues.DvText("EHR Status"));
        status.setModifiable(true);
        status.setQueryable(true);
        return status;
    }

    // ========================
    // Migrated from OpenehrEhrControllerTest: createEhr() (3 prefer variations)
    // ========================
    @ParameterizedTest
    @ValueSource(strings = {"", "return=minimal", "return=representation"})
    void createEhr(String prefer) {
        UUID ehrId = UUID.fromString("a6ddec4c-a68a-49ef-963e-3e0bc1970a28");
        var status = ehrStatus();

        when(mockEhrService.create(isNull(), any())).thenReturn(ehrId);
        when(mockEhrService.getEhrStatus(ehrId)).thenReturn(status);
        when(mockEhrService.getLatestVersionUidOfStatus(ehrId))
                .thenReturn(new ObjectVersionId(UUID.randomUUID().toString(), SYSTEM_ID, "1"));
        when(mockEhrService.getCreationTime(ehrId)).thenReturn(new DvDateTime(OffsetDateTime.now()));

        var response = spyController.createEhr(null, prefer);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        if (prefer.equals("return=representation")) {
            EhrResponseDto body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.ehrId()).isEqualTo(ehrId);
            assertThat(body.systemId()).isEqualTo(SYSTEM_ID);
            assertThat(body.ehrStatus()).isNotNull();
        } else {
            assertThat(response.getBody()).isNull();
        }
    }

    // ========================
    // Migrated from OpenehrEhrControllerTest: createEhrWithId() (3 prefer variations)
    // ========================
    @ParameterizedTest
    @ValueSource(strings = {"", "return=minimal", "return=representation"})
    void createEhrWithId(String prefer) {
        UUID ehrId = UUID.fromString("a6ddec4c-a68a-49ef-963e-3e0bc1970a28");
        var status = ehrStatus();

        when(mockEhrService.create(eq(ehrId), any())).thenReturn(ehrId);
        when(mockEhrService.getEhrStatus(ehrId)).thenReturn(status);
        when(mockEhrService.getLatestVersionUidOfStatus(ehrId))
                .thenReturn(new ObjectVersionId(UUID.randomUUID().toString(), SYSTEM_ID, "1"));
        when(mockEhrService.getCreationTime(ehrId)).thenReturn(new DvDateTime(OffsetDateTime.now()));

        var response = spyController.createEhrWithId(ehrId.toString(), null, prefer);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        if (prefer.equals("return=representation")) {
            EhrResponseDto body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.ehrId()).isEqualTo(ehrId);
        } else {
            assertThat(response.getBody()).isNull();
        }
    }

    // ========================
    // Migrated from OpenehrEhrControllerTest: createEhrWithIdInvalidUUID()
    // ========================
    @Test
    void createEhrWithIdInvalidUUID() {
        assertThatThrownBy(() -> spyController.createEhrWithId("invalid", null, null))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessageContaining("Invalid");
    }

    // ========================
    // Migrated from OpenehrEhrControllerTest: getEhrByIdInvalidUUID()
    // ========================
    @Test
    void getEhrInvalidUUID() {
        assertThatThrownBy(() -> spyController.getEhr("not-a-uuid"))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessageContaining("Invalid");
    }

    // ========================
    // Migrated from OpenehrEhrControllerTest: getEhrByIdNotExist()
    // ========================
    @Test
    void getEhrNotExist() {
        UUID ehrId = UUID.fromString("46e8518f-e9b7-45de-b214-1588466d71d6");
        doThrow(new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId))
                .when(mockEhrService)
                .checkEhrExists(ehrId);

        assertThatThrownBy(() -> spyController.getEhr(ehrId.toString()))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("No EHR found");
    }

    // ========================
    // Migrated from OpenehrEhrControllerTest: getEhrBy()
    // ========================
    @Test
    void getEhr() {
        UUID ehrId = UUID.fromString("0c1f9fce-05bd-4f6f-a558-fc27a2140795");
        var status = ehrStatus();

        when(mockEhrService.getEhrStatus(ehrId)).thenReturn(status);
        when(mockEhrService.getCreationTime(ehrId)).thenReturn(new DvDateTime(OffsetDateTime.now()));

        var response = spyController.getEhr(ehrId.toString());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        EhrResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.ehrId()).isEqualTo(ehrId);
        assertThat(body.systemId()).isEqualTo(SYSTEM_ID);
        assertThat(body.ehrStatus()).isSameAs(status);
    }

    // ========================
    // Migrated from OpenehrEhrControllerTest: getEhrBySubjectNotFound()
    // ========================
    @Test
    void findBySubjectNotFound() {
        when(mockEhrService.findBySubject(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spyController.findBySubject("test_subject", "some:external:id"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("No EHR found");
    }

    // ========================
    // Migrated from OpenehrEhrControllerTest: getEhrBySubject()
    // ========================
    @Test
    void findBySubject() {
        UUID ehrId = UUID.fromString("d2c04bbd-fbd5-4a39-ade3-848a336037ed");
        var status = ehrStatus();

        when(mockEhrService.findBySubject("test_subject", "some:external:id")).thenReturn(Optional.of(ehrId));
        when(mockEhrService.getEhrStatus(ehrId)).thenReturn(status);
        when(mockEhrService.getCreationTime(ehrId)).thenReturn(new DvDateTime(OffsetDateTime.now()));

        ResponseEntity<EhrResponseDto> response = spyController.findBySubject("test_subject", "some:external:id");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        EhrResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.ehrId()).isEqualTo(ehrId);
        assertThat(body.ehrStatus()).isSameAs(status);
    }

    // ========================
    // NEW: Test BaseApiController utility methods
    // ========================
    @Test
    void parseEhrIdValid() {
        UUID expected = UUID.fromString("a6ddec4c-a68a-49ef-963e-3e0bc1970a28");
        assertThat(spyController.parseEhrId("a6ddec4c-a68a-49ef-963e-3e0bc1970a28"))
                .isEqualTo(expected);
    }

    @Test
    void preferRepresentationLogic() {
        assertThat(spyController.preferRepresentation("return=representation")).isTrue();
        assertThat(spyController.preferRepresentation("return=minimal")).isFalse();
        assertThat(spyController.preferRepresentation(null)).isFalse();
        assertThat(spyController.preferRepresentation("")).isFalse();
    }
}
