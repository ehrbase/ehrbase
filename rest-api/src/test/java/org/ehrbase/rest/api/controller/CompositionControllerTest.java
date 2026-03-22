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

import com.nedap.archie.rm.composition.Composition;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
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
 * New tests for CompositionController — covers CRUD, format negotiation, versioning.
 */
class CompositionControllerTest {

    private static final UUID EHR_ID = UUID.fromString("7d44b88c-4199-4bad-97dc-d78268e01398");
    private static final UUID COMP_ID = UUID.fromString("a6ddec4c-a68a-49ef-963e-3e0bc1970a28");
    private static final String SYSTEM_ID = "test.composition.controller";

    private final CompositionService mockCompositionService = mock();
    private final EhrService mockEhrService = mock();
    private final SystemService mockSystemService = mock();
    private final RequestContext mockRequestContext = mock();
    private final org.jooq.DSLContext mockDsl = mock();

    private final CompositionController spyController = spy(new CompositionController(
            mockCompositionService, mockEhrService, mockSystemService, mockRequestContext, mockDsl));

    @BeforeEach
    void setUp() {
        Mockito.reset(
                mockCompositionService, mockEhrService, mockSystemService, mockRequestContext, mockDsl, spyController);
        when(mockSystemService.getSystemId()).thenReturn(SYSTEM_ID);
        doReturn(URI.create("https://test/api/v2/ehrs/" + EHR_ID + "/compositions"))
                .when(spyController)
                .locationUri(any(String[].class));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // Create composition with prefer variations
    @ParameterizedTest
    @ValueSource(strings = {"", "return=minimal", "return=representation"})
    void createComposition(String prefer) {
        var composition = mock(Composition.class);
        when(mockCompositionService.buildComposition(any(), any(), any())).thenReturn(composition);
        when(mockCompositionService.create(eq(EHR_ID), any())).thenReturn(Optional.of(COMP_ID));
        when(mockCompositionService.serialize(any(), any())).thenReturn(new StructuredString("{}", null));

        var response = spyController.createComposition(EHR_ID.toString(), "{}", "application/json", null, null, prefer);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        if (prefer.equals("return=representation")) {
            assertThat(response.getBody()).isNotNull();
        } else {
            assertThat(response.getBody()).isNull();
        }
    }

    // Invalid EHR ID
    @Test
    void createCompositionInvalidEhrId() {
        assertThatThrownBy(() -> spyController.createComposition("invalid", "{}", "application/json", null, null, null))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    // Get composition by ID
    @Test
    void getComposition() {
        var composition = mock(Composition.class);
        when(mockCompositionService.retrieve(eq(EHR_ID), eq(COMP_ID), any())).thenReturn(Optional.of(composition));
        when(mockCompositionService.serialize(any(), any())).thenReturn(new StructuredString("{\"test\":true}", null));
        when(mockCompositionService.getLastVersionNumber(EHR_ID, COMP_ID)).thenReturn(1);

        var response =
                spyController.getComposition(EHR_ID.toString(), COMP_ID.toString(), "application/json", null, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // Get composition not found
    @Test
    void getCompositionNotFound() {
        when(mockCompositionService.retrieve(eq(EHR_ID), eq(COMP_ID), any())).thenReturn(Optional.empty());
        when(mockCompositionService.isDeleted(eq(EHR_ID), eq(COMP_ID), any())).thenReturn(false);

        assertThatThrownBy(() -> spyController.getComposition(
                        EHR_ID.toString(), COMP_ID.toString(), "application/json", null, null))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    // Update composition with prefer variations
    @ParameterizedTest
    @ValueSource(strings = {"", "return=minimal", "return=representation"})
    void updateComposition(String prefer) {
        String ifMatch = "\"" + COMP_ID + "::" + SYSTEM_ID + "::1\"";
        var composition = mock(Composition.class);
        when(mockCompositionService.buildComposition(any(), any(), any())).thenReturn(composition);
        when(mockCompositionService.update(eq(EHR_ID), any(), any())).thenReturn(Optional.of(COMP_ID));
        when(mockCompositionService.serialize(any(), any())).thenReturn(new StructuredString("{}", null));

        var response = spyController.updateComposition(
                EHR_ID.toString(),
                COMP_ID + "::" + SYSTEM_ID + "::1",
                "{}",
                "application/json",
                ifMatch,
                null,
                null,
                prefer);

        if (prefer.equals("return=representation")) {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        } else {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    // Delete composition
    @Test
    void deleteComposition() {
        String precedingVersionUid = COMP_ID + "::" + SYSTEM_ID + "::1";
        var response = spyController.deleteComposition(EHR_ID.toString(), precedingVersionUid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // EHR not modifiable
    @Test
    void createCompositionEhrNotModifiable() {
        doThrow(new org.ehrbase.api.exception.StateConflictException("EHR is not modifiable"))
                .when(mockEhrService)
                .checkEhrExistsAndIsModifiable(EHR_ID);

        assertThatThrownBy(() ->
                        spyController.createComposition(EHR_ID.toString(), "{}", "application/json", null, null, null))
                .isInstanceOf(org.ehrbase.api.exception.StateConflictException.class)
                .hasMessageContaining("not modifiable");
    }

    // Get composition by version UID (with ::system::version)
    @Test
    void getCompositionByVersionUid() {
        String versionUid = COMP_ID + "::" + SYSTEM_ID + "::2";
        var composition = mock(Composition.class);
        when(mockCompositionService.retrieve(eq(EHR_ID), eq(COMP_ID), eq(2))).thenReturn(Optional.of(composition));
        when(mockCompositionService.serialize(any(), any())).thenReturn(new StructuredString("{}", null));

        var response = spyController.getComposition(EHR_ID.toString(), versionUid, "application/json", null, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Get composition with format=FLAT
    @Test
    void getCompositionAsFlat() {
        var composition = mock(Composition.class);
        when(mockCompositionService.retrieve(eq(EHR_ID), eq(COMP_ID), any())).thenReturn(Optional.of(composition));
        when(mockCompositionService.serialize(any(), any()))
                .thenReturn(new StructuredString("{\"path/to/value\":42}", null));
        when(mockCompositionService.getLastVersionNumber(EHR_ID, COMP_ID)).thenReturn(1);

        var response = spyController.getComposition(EHR_ID.toString(), COMP_ID.toString(), null, "FLAT", null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
