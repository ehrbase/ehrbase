/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.rest.openehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.generic.PartySelf;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.ehrbase.api.dto.EhrDto;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.BaseController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;

class OpenehrEhrControllerTest {

    private static final String CONTEXT_PATH = "https://test.ehr.controller/ehrbase/rest";

    private final EhrService mockEhrService = mock();

    private final OpenehrEhrController spyController =
            spy(new OpenehrEhrController(mockEhrService, () -> "test.ehr.controller"));

    @BeforeEach
    void setUp() {
        Mockito.reset(mockEhrService, spyController);
        doReturn(CONTEXT_PATH).when(spyController).getContextPath();
    }

    @AfterEach
    void tearDown() {
        // ensure the context is clean after each test
        RequestContextHolder.resetRequestAttributes();
    }

    private OpenehrEhrController controller() {
        return spyController;
    }

    private EhrService.EhrCreationResult createResult(UUID ehrId) {
        return new EhrService.EhrCreationResult(ehrId, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "return=minimal", "return=representation"})
    void createEhr(String prefer) {

        UUID ehrId = UUID.fromString("a6ddec4c-a68a-49ef-963e-3e0bc1970a28");
        runCreateTest(ehrId, null, prefer, () -> {
            when(mockEhrService.create(isNull(), any())).thenReturn(createResult(ehrId));
            return controller().createEhr("1.0.3", null, prefer, null);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "return=minimal", "return=representation"})
    void createEhrWithStatus(String prefer) {

        var ehrStatus = new EhrStatusDto(
                null,
                "openEHR-EHR-EHR_STATUS.generic.v1",
                new DvText("EHR Status"),
                null,
                null,
                new PartySelf(),
                true,
                true,
                null);
        UUID ehrId = UUID.fromString("a6ddec4c-a68a-49ef-963e-3e0bc1970a28");
        runCreateTest(ehrId, ehrStatus, prefer, () -> {
            when(mockEhrService.create(isNull(), any())).thenReturn(createResult(ehrId));
            return controller().createEhr("1.0.3", null, prefer, ehrStatus);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "return=minimal", "return=representation"})
    void createEhrWithId(String prefer) {

        UUID ehrId = UUID.fromString("a6ddec4c-a68a-49ef-963e-3e0bc1970a28");
        runCreateTest(ehrId, null, prefer, () -> {
            when(mockEhrService.create(eq(ehrId), any())).thenReturn(createResult(ehrId));
            return controller().createEhrWithId("1.0.3", null, prefer, ehrId.toString(), null);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "return=minimal", "return=representation"})
    void createEhrWithIdIdAndStatus(String prefer) {

        UUID ehrId = UUID.fromString("2eee20ea-67cc-449f-95bc-1dbdf6d3d0c1");
        var ehrStatus = new EhrStatusDto(
                null,
                "openEHR-EHR-EHR_STATUS.generic.v1",
                new DvText("EHR Status"),
                null,
                null,
                new PartySelf(),
                true,
                true,
                null);
        runCreateTest(ehrId, ehrStatus, prefer, () -> {
            when(mockEhrService.create(eq(ehrId), any())).thenReturn(createResult(ehrId));
            return controller().createEhrWithId("1.0.3", null, prefer, ehrId.toString(), ehrStatus);
        });
    }

    @Test
    void createEhrWithIdInvalidUUID() {

        OpenehrEhrController controller = controller();
        assertThatThrownBy(() -> controller.createEhrWithId("1.0.3", null, null, "invalid", null))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessage("EHR ID format not a UUID");
    }

    private void runCreateTest(
            UUID ehrId, EhrStatusDto ehrStatus, String prefer, Supplier<ResponseEntity<EhrDto>> creation) {

        when(mockEhrService.getEhrStatus(ehrId)).thenReturn(ehrStatus);

        var response = creation.get();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders()).containsEntry(HttpHeaders.LOCATION, List.of(CONTEXT_PATH + "/ehr/" + ehrId));

        var body = response.getBody();
        if (prefer.equals(BaseController.RETURN_REPRESENTATION)) {
            assertResponseDataBody(response, ehrId, ehrStatus);
        } else {
            assertThat(body).isNull();
        }
    }

    @Test
    void getEhrByIdInvalidUUID() {

        OpenehrEhrController controller = controller();
        assertThatThrownBy(() -> controller.getEhrById("not a uui"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("EHR not found, in fact, only UUID-type IDs are supported");
    }

    @Test
    void getEhrByIdNotExist() {

        when(mockEhrService.getEhrStatus(any()))
                .thenThrow(new ObjectNotFoundException(
                        "ehr", "No EHR found with given ID: 46e8518f-e9b7-45de-b214-1588466d71d6"));

        OpenehrEhrController controller = controller();
        assertThatThrownBy(() -> controller.getEhrById("46e8518f-e9b7-45de-b214-1588466d71d6"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("No EHR found with given ID: 46e8518f-e9b7-45de-b214-1588466d71d6");
    }

    @Test
    void getEhrBy() {

        UUID ehrId = UUID.fromString("0c1f9fce-05bd-4f6f-a558-fc27a2140795");
        var ehrStatus = new EhrStatusDto(
                null,
                "openEHR-EHR-EHR_STATUS.generic.v1",
                new DvText("EHR Status"),
                null,
                null,
                new PartySelf(),
                true,
                true,
                null);

        when(mockEhrService.getEhrStatus(ehrId)).thenReturn(ehrStatus);

        var response = controller().getEhrById(ehrId.toString());
        assertEhrResponseData(response, ehrId, ehrStatus);
    }

    @Test
    void getEhrBySubjectNotFound() {

        when(mockEhrService.findBySubject(any(), any())).thenReturn(Optional.empty());

        OpenehrEhrController controller = controller();
        assertThatThrownBy(() -> controller.getEhrBySubject("test_subject", "some:external:id"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("No EHR with supplied subject parameters found");
    }

    @Test
    void getEhrBySubject() {

        UUID ehrId = UUID.fromString("d2c04bbd-fbd5-4a39-ade3-848a336037ed");
        var ehrStatus = new EhrStatusDto(
                null,
                "openEHR-EHR-EHR_STATUS.generic.v1",
                new DvText("EHR Status"),
                null,
                null,
                new PartySelf(),
                true,
                true,
                null);

        when(mockEhrService.findBySubject("test_subject", "some:external:id")).thenReturn(Optional.of(ehrId));
        when(mockEhrService.getEhrStatus(ehrId)).thenReturn(ehrStatus);

        var response = controller().getEhrBySubject("test_subject", "some:external:id");
        assertEhrResponseData(response, ehrId, ehrStatus);
    }

    private static void assertEhrResponseData(ResponseEntity<EhrDto> response, UUID ehrId, EhrStatusDto ehrStatus) {

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders()).containsEntry(HttpHeaders.LOCATION, List.of(CONTEXT_PATH + "/ehr/" + ehrId));

        assertResponseDataBody(response, ehrId, ehrStatus);
    }

    private static void assertResponseDataBody(ResponseEntity<EhrDto> response, UUID ehrId, EhrStatusDto ehrStatus) {
        EhrDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.ehrId().getValue()).isEqualTo(ehrId.toString());
        assertThat(body.ehrStatus()).isSameAs(ehrStatus);
    }
}
