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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
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

class OpenehrEhrStatusControllerTest {

    private static final String CONTEXT_PATH = "https://test.ehr-status.controller/ehrbase/rest";

    private final EhrService mockEhrService = mock();

    private final OpenehrEhrStatusController spyController = spy(new OpenehrEhrStatusController(mockEhrService));

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

    private OpenehrEhrStatusController controller() {
        return spyController;
    }

    private EhrStatusDto ehrStatusDto(ObjectVersionId versionId, Boolean isQueryable, Boolean isModifiable) {
        return new EhrStatusDto(
                versionId,
                "openEHR-EHR-EHR_STATUS.generic.v1",
                new DvText("EHR Status"),
                null,
                null,
                new PartySelf(),
                isQueryable,
                isModifiable,
                null);
    }

    private OriginalVersion<EhrStatusDto> originalVersion(
            ObjectVersionId nextVersionId,
            ObjectVersionId currentVersionId,
            OffsetDateTime lastModified,
            EhrStatusDto ehrStatus) {
        OriginalVersion<EhrStatusDto> objectOriginalVersion = new OriginalVersion<>();
        objectOriginalVersion.setUid(nextVersionId);
        objectOriginalVersion.setPrecedingVersionUid(currentVersionId);
        objectOriginalVersion.setData(new EhrStatusDto(
                nextVersionId,
                ehrStatus.archetypeNodeId(),
                ehrStatus.name(),
                ehrStatus.archetypeDetails(),
                ehrStatus.feederAudit(),
                ehrStatus.subject(),
                ehrStatus.isQueryable(),
                ehrStatus.isModifiable(),
                ehrStatus.otherDetails()));

        AuditDetails auditDetails = new AuditDetails();
        auditDetails.setTimeCommitted(new DvDateTime(lastModified));
        objectOriginalVersion.setCommitAudit(auditDetails);

        return objectOriginalVersion;
    }

    @Test
    void getEhrStatusByVersionNotFound() {

        UUID ehrId = UUID.fromString("d83a16ae-2644-4706-8911-282772c10137");
        OpenehrEhrStatusController controller = controller();
        assertThatThrownBy(() -> controller.getEhrStatusByVersionId(
                        ehrId, "13a82993-a489-421a-ac88-5cec001bd58c::local-system::42"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Could not find EhrStatus[id=13a82993-a489-421a-ac88-5cec001bd58c, version=42]");
    }

    @Test
    void getEhrStatusVersionByTimeLatest() {

        OpenehrEhrStatusController controller = controller();
        runTestWithMockResult((ehrId, ehrStatusDto) -> {
            doReturn(ehrStatusDto.uid()).when(mockEhrService).getLatestVersionUidOfStatus(ehrId);
            return controller.getEhrStatusVersionByTime(ehrId, null);
        });
    }

    @Test
    void getEhrStatusVersionByTimeByTimestamp() {

        OpenehrEhrStatusController controller = controller();
        runTestWithMockResult((ehrId, ehrStatusDto) -> {
            doReturn(ehrStatusDto.uid())
                    .when(mockEhrService)
                    .getEhrStatusVersionByTimestamp(ehrId, OffsetDateTime.parse("2024-07-16T08:30:00Z"));
            return controller.getEhrStatusVersionByTime(ehrId, "2024-07-16T08:30:00Z");
        });
    }

    @Test
    void getEhrStatusByVersionIdInvalid() {

        UUID ehrId = UUID.fromString("d83a16ae-2644-4706-8911-282772c10137");
        OpenehrEhrStatusController controller = controller();

        assertThatThrownBy(() -> controller.getEhrStatusByVersionId(ehrId, "13a82993-a489-421a-ac88-5cec001bd58c"))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessage("VERSION UID parameter does not contain a version");
    }

    @Test
    void getEhrStatusByVersionId() {

        UUID ehrId = UUID.fromString("d83a16ae-2644-4706-8911-282772c10137");
        UUID ehrStatusId = UUID.fromString("13a82993-a489-421a-ac88-5cec001bd58c");
        ObjectVersionId versionId = new ObjectVersionId(ehrStatusId.toString(), "some-system", "42");
        OffsetDateTime lastModified = OffsetDateTime.parse("2024-07-16T13:15:00Z");

        EhrStatusDto ehrStatus = ehrStatusDto(versionId, true, false);

        doReturn(Optional.of(originalVersion(versionId, null, lastModified, ehrStatus)))
                .when(mockEhrService)
                .getEhrStatusAtVersion(ehrId, ehrStatusId, 42);
        ResponseEntity<EhrStatusDto> response = controller().getEhrStatusByVersionId(ehrId, versionId.getValue());

        assertEhrStatusResponseData(response, ehrStatus, ehrId, versionId, "Tue, 16 Jul 2024 13:15:00 GMT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "return=minimal", "return=representation"})
    void updateEhrStatus(String prefer) {

        UUID ehrId = UUID.fromString("d83a16ae-2644-4706-8911-282772c10137");
        UUID ehrStatusId = UUID.fromString("305eb2fd-c228-445c-ada7-5429d852fbb2");
        ObjectVersionId currentVersionId = new ObjectVersionId(ehrStatusId.toString(), "test.ehr.controller", "2");
        ObjectVersionId nextVersionId = new ObjectVersionId(ehrStatusId.toString(), "test.ehr.controller", "3");
        OffsetDateTime lastModified = OffsetDateTime.parse("2024-07-16T12:00:00Z");

        EhrStatusDto ehrStatus = ehrStatusDto(currentVersionId, true, true);

        doReturn(new EhrService.EhrResult(ehrId, nextVersionId, ehrStatus))
                .when(mockEhrService)
                .updateStatus(ehrId, ehrStatus, currentVersionId, null, null);
        doReturn(Optional.of(originalVersion(nextVersionId, currentVersionId, lastModified, ehrStatus)))
                .when(mockEhrService)
                .getEhrStatusAtVersion(ehrId, ehrStatusId, 3);

        ResponseEntity<EhrStatusDto> response =
                controller().updateEhrStatus(ehrId, currentVersionId.getValue(), prefer, ehrStatus);

        var body = response.getBody();
        if (prefer.equals(BaseController.RETURN_REPRESENTATION)) {
            assertEhrStatusResponseData(response, ehrStatus, ehrId, nextVersionId, "Tue, 16 Jul 2024 12:00:00 GMT");
        } else {
            assertResponse(HttpStatus.NO_CONTENT, response, ehrId, nextVersionId, "Tue, 16 Jul 2024 12:00:00 GMT");
            assertThat(body).isNull();
        }
    }

    private void runTestWithMockResult(BiFunction<UUID, EhrStatusDto, ResponseEntity<EhrStatusDto>> consumer) {

        UUID ehrId = UUID.fromString("7c927831-726e-4ad7-8b62-b078d80eb59a");
        UUID ehrStatusId = UUID.fromString("8c2152e8-10f7-4b9f-bc28-27421b8937e7");
        ObjectVersionId versionId = new ObjectVersionId(ehrStatusId.toString(), "some-system", "12");
        OffsetDateTime lastModified = OffsetDateTime.parse("2024-07-16T13:20:00Z");

        EhrStatusDto ehrStatus = ehrStatusDto(versionId, false, false);

        doReturn(Optional.of(originalVersion(versionId, null, lastModified, ehrStatus)))
                .when(mockEhrService)
                .getEhrStatusAtVersion(ehrId, ehrStatusId, 12);

        ResponseEntity<EhrStatusDto> response = consumer.apply(ehrId, ehrStatus);
        assertEhrStatusResponseData(response, ehrStatus, ehrId, versionId, "Tue, 16 Jul 2024 13:20:00 GMT");
    }

    private static void assertEhrStatusResponseData(
            ResponseEntity<EhrStatusDto> response,
            EhrStatusDto expectedEhrStatusDto,
            UUID ehrId,
            ObjectVersionId versionId,
            String lastModified) {

        assertResponse(HttpStatus.OK, response, ehrId, versionId, lastModified);
        assertEhrStatusResponseDataBody(response, expectedEhrStatusDto, versionId);
    }

    private static void assertResponse(
            HttpStatus status,
            ResponseEntity<EhrStatusDto> response,
            UUID ehrId,
            ObjectVersionId versionId,
            String lastModified) {

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getHeaders())
                .containsEntry(
                        HttpHeaders.LOCATION,
                        List.of(CONTEXT_PATH + "/ehr/" + ehrId + "/ehr_status/" + versionId.getValue()));
        assertThat(response.getHeaders())
                .containsEntry(HttpHeaders.ETAG, List.of("\"%s\"".formatted(versionId.getValue())));
        assertThat(response.getHeaders()).containsEntry(HttpHeaders.LAST_MODIFIED, List.of(lastModified));
    }

    private static void assertEhrStatusResponseDataBody(
            ResponseEntity<EhrStatusDto> response, EhrStatusDto expectedEhrStatusDto, ObjectVersionId versionId) {

        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.uid()).describedAs("Must have next version ID").isEqualTo(versionId);
        assertThat(body.archetypeNodeId())
                .describedAs("Must be default archetype for EHR_STATUS")
                .isEqualTo("openEHR-EHR-EHR_STATUS.generic.v1");
        assertThat(body.name()).describedAs("Must have default name").isEqualTo(new DvText("EHR Status"));
        assertThat(body.isQueryable()).describedAs("Invalid Queryable").isEqualTo(expectedEhrStatusDto.isQueryable());
        assertThat(body.isModifiable())
                .describedAs("Invalid Modifiable")
                .isEqualTo(expectedEhrStatusDto.isModifiable());
    }
}
