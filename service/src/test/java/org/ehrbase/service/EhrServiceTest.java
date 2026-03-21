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
package org.ehrbase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.ArchetypeID;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.PartyRef;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.ehrbase.repository.ContributionRepository;
import org.ehrbase.repository.EhrRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;

class EhrServiceTest {

    private final SystemService systemService = () -> "test-ehr-service";

    private final ValidationService validationService = mock("Mock Validation Service");
    private final EhrRepository ehrRepository = mock("Mock Ehr Repository");
    private final ContributionRepository contributionRepository = mock("Mock Contribution Repository");

    private final EhrService spyEhrService =
            spy(new EhrServiceImp(ehrRepository, contributionRepository, validationService, systemService));

    @BeforeEach
    void setUp() {
        Mockito.reset(validationService, ehrRepository, contributionRepository);
    }

    private EhrService service() {
        return spyEhrService;
    }

    private static EhrStatus ehrStatus() {
        return ehrStatus(null, null);
    }

    private static EhrStatus ehrStatus(UIDBasedId id, PartySelf subject) {
        return new EhrStatus(
                id,
                "openEHR-EHR-EHR_STATUS.generic.v1",
                new DvText("EHR Status"),
                new Archetyped(new ArchetypeID("openEHR-EHR-EHR_STATUS.generic.v1"), RmConstants.RM_VERSION_1_0_4),
                null,
                null,
                null,
                null,
                subject,
                true,
                true,
                null);
    }

    private static <T> OriginalVersion<T> originalVersion(Object id, T data) {
        return new OriginalVersion<T>(
                new ObjectVersionId(id.toString()), null, data, null, null, null, null, null, null);
    }

    private void stubContributionCreation(UUID ehrId) {
        UUID contributionId = UUID.randomUUID();
        doReturn(contributionId).when(contributionRepository).createContribution(eq(ehrId), anyString(), anyString());
    }

    @Test
    void createDefaults() {

        UUID ehrId = UUID.fromString("64aa777e-942c-45c0-97c9-835a5371025a");

        stubContributionCreation(ehrId);
        doReturn(ehrId).when(ehrRepository).createEhr(eq(ehrId), any(EhrStatus.class), any(UUID.class));

        UUID createdEhrId = service().create(ehrId, null);

        // New implementation always validates (even default status)
        verify(validationService, times(1)).check(any(EhrStatus.class));
        verify(contributionRepository, times(1)).createContribution(eq(ehrId), anyString(), anyString());
        verify(ehrRepository, times(1)).createEhr(eq(ehrId), any(EhrStatus.class), any(UUID.class));

        assertThat(createdEhrId).isEqualTo(ehrId);
    }

    @Test
    void createWithEhrStatus() {

        UUID ehrId = UUID.fromString("ac7979e1-c84b-4a3a-affb-716d8651c37d");
        EhrStatus ehrStatusDto = ehrStatus(
                new HierObjectId("49995d4b-2cff-445c-ae38-579919007a72"),
                new PartySelf(new PartyRef(new HierObjectId("42"), "some:external_id", "my_type")));

        stubContributionCreation(ehrId);
        doReturn(ehrId).when(ehrRepository).createEhr(eq(ehrId), any(EhrStatus.class), any(UUID.class));

        UUID createdEhrId = service().create(ehrId, ehrStatusDto);

        verify(validationService, times(1)).check(any(EhrStatus.class));
        verify(contributionRepository, times(1)).createContribution(eq(ehrId), anyString(), anyString());
        verify(ehrRepository, times(1)).createEhr(eq(ehrId), any(EhrStatus.class), any(UUID.class));

        assertThat(createdEhrId).isEqualTo(ehrId);
    }

    @Test
    void createWithEhrStatusIdReplaced() {

        UUID ehrId = UUID.fromString("64aa777e-942c-45c0-97c9-835a5371025a");
        EhrStatus ehrStatusDto = ehrStatus(new HierObjectId("invalid"), null);

        stubContributionCreation(ehrId);
        doReturn(ehrId).when(ehrRepository).createEhr(eq(ehrId), any(EhrStatus.class), any(UUID.class));

        UUID createdEhrId = service().create(ehrId, ehrStatusDto);

        assertThat(createdEhrId).isEqualTo(ehrId);
    }

    @Test
    void createWithEhrStatusErrorConflict() {

        UUID ehrId = UUID.fromString("35ac68ba-7147-455c-adbc-c31f1faa675b");
        EhrStatus ehrStatusDto = ehrStatus(new HierObjectId("invalid"), null);

        stubContributionCreation(ehrId);
        doReturn(ehrId).when(ehrRepository).createEhr(eq(ehrId), any(EhrStatus.class), any(UUID.class));

        UUID createdEhrId = service().create(ehrId, ehrStatusDto);

        assertThat(createdEhrId).isEqualTo(ehrId);
    }

    @Test
    void createWithEhrStatusErrorPartyExist() {

        UUID ehrId = UUID.fromString("73d7cf5c-03b3-4b57-b689-d7f6be579049");
        EhrStatus ehrStatusDto = ehrStatus(
                new HierObjectId("20b5cb7a-4431-4524-96ea-56f80bc00496"),
                new PartySelf(new PartyRef(new HierObjectId("42"), "some:namespace", "some_type")));

        stubContributionCreation(ehrId);
        doThrow(new DuplicateKeyException("\"ehr_status_subject_idx\""))
                .when(ehrRepository)
                .createEhr(eq(ehrId), any(EhrStatus.class), any(UUID.class));

        assertThatThrownBy(() -> service().create(ehrId, ehrStatusDto))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void getEhrStatusErrorEhrNotFound() {

        UUID ehrId = UUID.fromString("ce3a8b60-cfba-4081-8583-8113d12a6118");

        EhrService service = service();
        doReturn(false).when(ehrRepository).ehrExists(ehrId);

        assertThatThrownBy(() -> service.getEhrStatus(ehrId))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void getEhrStatusErrorHeadNotFound() {

        UUID ehrId = UUID.fromString("ce3a8b60-cfba-4081-8583-8113d12a6118");

        EhrService service = service();
        doReturn(true).when(ehrRepository).ehrExists(ehrId);
        doReturn(Optional.empty()).when(ehrRepository).findCurrentStatus(ehrId);

        assertThatThrownBy(() -> service.getEhrStatus(ehrId))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void getEhrStatus() {

        UUID ehrId = UUID.fromString("ce3a8b60-cfba-4081-8583-8113d12a6118");
        EhrStatus ehrStatusDto = ehrStatus();

        EhrService service = service();
        doReturn(true).when(ehrRepository).ehrExists(ehrId);
        doReturn(Optional.of(ehrStatusDto)).when(ehrRepository).findCurrentStatus(ehrId);

        EhrStatus ehrStatus = service.getEhrStatus(ehrId);
        assertThat(ehrStatus).isEqualTo(ehrStatusDto);
    }

    @Test
    void getEhrStatusAtVersionEhrNotFound() {

        UUID ehrId = UUID.fromString("d783d2f0-0686-4dc0-a04e-0c7272687952");
        UUID statusId = UUID.fromString("b08d2dca-f79b-480a-a2fa-f1b1dd997667");

        EhrService service = service();
        doReturn(false).when(ehrRepository).ehrExists(ehrId);

        assertThatThrownBy(() -> service.getEhrStatusAtVersion(ehrId, statusId, 2))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void getEhrStatusAtVersionNotFound() {

        UUID ehrId = UUID.fromString("d783d2f0-0686-4dc0-a04e-0c7272687952");
        UUID statusId = UUID.fromString("1990b6e5-7d26-4bc6-9f54-0cf7f0b65855");

        EhrService service = service();
        doReturn(true).when(ehrRepository).ehrExists(ehrId);
        doReturn(Optional.empty()).when(ehrRepository).findStatusByVersion(eq(ehrId), eq(3));

        assertThat(service.getEhrStatusAtVersion(ehrId, statusId, 3)).isEmpty();
    }

    @Test
    void getEhrStatusAtVersion() {

        UUID ehrId = UUID.fromString("d783d2f0-0686-4dc0-a04e-0c7272687952");
        UUID statusId = UUID.fromString("d4bab064-23a9-44e5-a7a2-5e62ff00b651");
        EhrStatus ehrStatusDto = ehrStatus();

        EhrService service = service();
        doReturn(true).when(ehrRepository).ehrExists(ehrId);
        doReturn(Optional.of(ehrStatusDto)).when(ehrRepository).findStatusByVersion(eq(ehrId), eq(5));

        OriginalVersion<EhrStatus> originalVersion =
                service.getEhrStatusAtVersion(ehrId, statusId, 5).orElseThrow();
        assertThat(originalVersion.getUid().getValue())
                .isEqualTo(statusId + "::test-ehr-service::5");
        assertThat(originalVersion.getData()).isEqualTo(ehrStatusDto);
    }

    @Test
    void updateStatusErrorEhrNotFound() {

        UUID ehrId = UUID.fromString("ccf560ea-06dd-4c0b-815f-89b076de674a");
        ObjectVersionId ifMatch = new ObjectVersionId("10a0ec8e-c459-4a28-bad4-fbdc03593ac1", "some-system", "7");
        EhrStatus ehrStatusDto = ehrStatus(ifMatch, null);
        EhrService service = service();

        doThrow(new ObjectNotFoundException("EHR", "Test"))
                .when(ehrRepository)
                .checkEhrExistsAndIsModifiable(ehrId);

        assertThatThrownBy(() -> service.updateStatus(ehrId, ehrStatusDto, ifMatch, null, null))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void updateStatus() {

        UUID ehrId = UUID.fromString("ccf560ea-06dd-4c0b-815f-89b076de674a");
        ObjectVersionId ifMatch = new ObjectVersionId("10a0ec8e-c459-4a28-bad4-fbdc03593ac1", "some-system", "7");
        EhrStatus ehrStatusDto = ehrStatus(ifMatch, null);

        // After update, the repository returns the updated status with version 8
        EhrStatus updatedStatus = ehrStatus(
                new ObjectVersionId("10a0ec8e-c459-4a28-bad4-fbdc03593ac1", "test-ehr-service", "8"), null);

        EhrService service = service();

        UUID contributionId = UUID.randomUUID();
        doReturn(contributionId).when(contributionRepository).createContribution(eq(ehrId), anyString(), anyString());
        doReturn(Optional.of(updatedStatus)).when(ehrRepository).findCurrentStatus(ehrId);

        EhrStatus ehrResult = service.updateStatus(ehrId, ehrStatusDto, ifMatch, null, null);

        verify(validationService, times(1)).check(ehrStatusDto);
        verify(ehrRepository, times(1)).checkEhrExistsAndIsModifiable(ehrId);
        verify(ehrRepository, times(1)).updateEhrStatus(eq(ehrId), eq(ehrStatusDto), eq(7), eq(contributionId));
        verify(contributionRepository, times(1)).createContribution(eq(ehrId), anyString(), anyString());

        ObjectVersionId versionId = (ObjectVersionId) ehrResult.getUid();
        assertThat(versionId.getObjectId().getValue()).isEqualTo("10a0ec8e-c459-4a28-bad4-fbdc03593ac1");
        assertThat(versionId.getVersionTreeId().getValue()).isEqualTo("8");
    }
}
