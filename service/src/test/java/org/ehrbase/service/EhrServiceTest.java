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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.PartyRef;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.repository.AuditDetailsTargetType;
import org.ehrbase.repository.CompositionRepository;
import org.ehrbase.repository.ContributionRepository;
import org.ehrbase.repository.EhrFolderRepository;
import org.ehrbase.repository.EhrRepository;
import org.ehrbase.repository.experimental.ItemTagRepository;
import org.ehrbase.service.maping.EhrStatusMapper;
import org.ehrbase.test.assertions.EhrStatusAssert;
import org.jooq.Record2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;

class EhrServiceTest {

    private final SystemService systemService = () -> "test-ehr-service";

    private final ValidationService validationService = mock("Mock Validation Service");
    private final EhrFolderRepository ehrFolderRepository = mock("Mock Repository");
    private final CompositionRepository compositionRepository = mock("Mock Composition Repository");
    private final ContributionRepository contributionRepository = mock("Mock Contribution Repository");

    private final EhrRepository ehrRepository = mock("Mock Ehr Repository");
    private final ItemTagRepository itemTagRepository = mock("Mock Item Tag Repository");

    private final EhrService spyEhrService = spy(new EhrServiceImp(
            validationService,
            systemService,
            ehrFolderRepository,
            compositionRepository,
            contributionRepository,
            ehrRepository,
            itemTagRepository));

    @BeforeEach
    void setUp() {
        Mockito.reset(
                validationService, ehrFolderRepository, compositionRepository, contributionRepository, ehrRepository);
    }

    private EhrService service() {

        return spyEhrService;
    }

    private static EhrStatusDto ehrStatusDto() {
        return ehrStatusDto(null, null);
    }

    private static EhrStatusDto ehrStatusDto(UIDBasedId id, PartySelf subject) {
        return new EhrStatusDto(
                id,
                "openEHR-EHR-EHR_STATUS.generic.v1",
                new DvText("EHR Status"),
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

    @Test
    void createDefaults() {

        UUID ehrId = UUID.fromString("64aa777e-942c-45c0-97c9-835a5371025a");

        CreationResult result = runCreateEhr(service(), ehrId, null);

        verify(validationService, never()).check(any(EhrStatusDto.class));

        assertThat(result.ehrId).isEqualTo(ehrId);
        EhrStatusAssert.assertThat(result.ehrStatus)
                .hasIdRoot()
                .hasIdExtension("test-ehr-service::1")
                .isEqualToIgnoreId(EhrStatusMapper.fromDto(ehrStatusDto(null, new PartySelf())));
    }

    @Test
    void createWithEhrStatus() {

        UUID ehrId = UUID.fromString("ac7979e1-c84b-4a3a-affb-716d8651c37d");
        EhrStatusDto ehrStatusDto = ehrStatusDto(
                new HierObjectId("49995d4b-2cff-445c-ae38-579919007a72"),
                new PartySelf(new PartyRef(new HierObjectId("42"), "some:external_id", "my_type")));
        CreationResult result = runCreateEhr(service(), ehrId, ehrStatusDto);

        verify(validationService, times(1)).check(any(EhrStatusDto.class));

        assertThat(result.ehrId).isEqualTo(ehrId);
        EhrStatusAssert.assertThat(result.ehrStatus)
                .hasIdRoot()
                .hasIdExtension("test-ehr-service::1")
                .isEqualToIgnoreId(EhrStatusMapper.fromDto(ehrStatusDto));
    }

    @Test
    void createWithEhrStatusIdReplaced() {

        UUID ehrId = UUID.fromString("64aa777e-942c-45c0-97c9-835a5371025a");
        EhrStatusDto ehrStatusDto = ehrStatusDto(new HierObjectId("invalid"), null);
        CreationResult result = runCreateEhr(service(), ehrId, ehrStatusDto);

        assertThat(UUID.fromString(result.ehrStatus.getUid().getRoot().getValue()))
                .isNotNull();
    }

    @Test
    void createWithEhrStatusErrorConflict() {

        UUID ehrId = UUID.fromString("35ac68ba-7147-455c-adbc-c31f1faa675b");
        EhrStatusDto ehrStatusDto = ehrStatusDto(new HierObjectId("invalid"), null);
        CreationResult result = runCreateEhr(service(), ehrId, ehrStatusDto);

        assertThat(UUID.fromString(result.ehrStatus.getUid().getRoot().getValue()))
                .isNotNull();
    }

    @Test
    void createWithEhrStatusErrorPartyExist() {

        UUID ehrId = UUID.fromString("73d7cf5c-03b3-4b57-b689-d7f6be579049");
        EhrStatusDto ehrStatusDto = ehrStatusDto(
                new HierObjectId("20b5cb7a-4431-4524-96ea-56f80bc00496"),
                new PartySelf(new PartyRef(new HierObjectId("42"), "some:namespace", "some_type")));

        doThrow(new DuplicateKeyException("\"ehr_status_subject_idx\""))
                .when(ehrRepository)
                .commit(
                        ArgumentMatchers.eq(ehrId),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any());

        assertThatThrownBy(() -> runCreateEhr(service(), ehrId, ehrStatusDto))
                .isInstanceOf(StateConflictException.class)
                .hasMessage(
                        "Supplied partyId[42] is used by a different EHR in the same partyNamespace[some:namespace].");
    }

    private record CreationResult(UUID ehrId, EhrStatus ehrStatus) {}

    private CreationResult runCreateEhr(EhrService service, UUID ehrId, EhrStatusDto ehrStatusDto) {

        ArgumentCaptor<EhrStatus> captor = ArgumentCaptor.forClass(EhrStatus.class);
        UUID createdEhrId = service.create(ehrId, ehrStatusDto).ehrId();

        verify(ehrRepository, times(1)).commit(eq(createdEhrId), captor.capture(), isNull(), isNull());
        return new CreationResult(
                createdEhrId, captor.getAllValues().stream().findFirst().orElseThrow());
    }

    @Test
    void getEhrStatusErrorEhrNotFound() {

        UUID ehrId = UUID.fromString("ce3a8b60-cfba-4081-8583-8113d12a6118");

        EhrService service = service();
        doReturn(true).when(ehrRepository).hasEhr(ehrId);

        assertThatThrownBy(() -> service.getEhrStatus(ehrId))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("No EHR found with given ID: ce3a8b60-cfba-4081-8583-8113d12a6118");
    }

    @Test
    void getEhrStatusErrorHeadNotFound() {

        UUID ehrId = UUID.fromString("ce3a8b60-cfba-4081-8583-8113d12a6118");

        EhrService service = service();
        doReturn(false).when(ehrRepository).hasEhr(ehrId);
        doReturn(Optional.empty()).when(ehrRepository).findHead(ehrId);

        assertThatThrownBy(() -> service.getEhrStatus(ehrId))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("No EHR found with given ID: ce3a8b60-cfba-4081-8583-8113d12a6118");
    }

    @Test
    void getEhrStatus() {

        UUID ehrId = UUID.fromString("ce3a8b60-cfba-4081-8583-8113d12a6118");
        EhrStatusDto ehrStatusDto = ehrStatusDto();

        EhrService service = service();
        doReturn(true).when(ehrRepository).hasEhr(ehrId);
        doReturn(Optional.of(EhrStatusMapper.fromDto(ehrStatusDto)))
                .when(ehrRepository)
                .findHead(ehrId);

        EhrStatusDto ehrStatus = service.getEhrStatus(ehrId).status();
        assertThat(ehrStatus).isEqualTo(ehrStatusDto);
    }

    @Test
    void getEhrStatusAtVersionEhrNotFound() {

        UUID ehrId = UUID.fromString("d783d2f0-0686-4dc0-a04e-0c7272687952");
        UUID statusId = UUID.fromString("b08d2dca-f79b-480a-a2fa-f1b1dd997667");

        EhrService service = service();
        doReturn(false).when(ehrRepository).hasEhr(ehrId);

        assertThatThrownBy(() -> service.getEhrStatusAtVersion(ehrId, statusId, 2))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("No EHR found with given ID: d783d2f0-0686-4dc0-a04e-0c7272687952");
    }

    @Test
    void getEhrStatusAtVersionNotFound() {

        UUID ehrId = UUID.fromString("d783d2f0-0686-4dc0-a04e-0c7272687952");
        UUID statusId = UUID.fromString("1990b6e5-7d26-4bc6-9f54-0cf7f0b65855");

        EhrService service = service();
        doReturn(true).when(ehrRepository).hasEhr(ehrId);

        assertThat(service.getEhrStatusAtVersion(ehrId, statusId, 3)).isEmpty();
    }

    @Test
    void getEhrStatusAtVersion() {

        UUID ehrId = UUID.fromString("d783d2f0-0686-4dc0-a04e-0c7272687952");
        UUID statusId = UUID.fromString("d4bab064-23a9-44e5-a7a2-5e62ff00b651");
        EhrStatusDto ehrStatusDto = ehrStatusDto();
        OriginalVersion<EhrStatus> expectedVersion = originalVersion(statusId, EhrStatusMapper.fromDto(ehrStatusDto));
        EhrService service = service();

        doReturn(true).when(ehrRepository).hasEhr(ehrId);
        doReturn(Optional.of(expectedVersion)).when(ehrRepository).getOriginalVersionStatus(ehrId, statusId, 5);

        OriginalVersion<EhrStatusDto> originalVersion =
                service.getEhrStatusAtVersion(ehrId, statusId, 5).orElseThrow();
        assertThat(originalVersion.getUid()).isEqualTo(expectedVersion.getUid());
        assertThat(originalVersion.getData()).isEqualTo(ehrStatusDto);
    }

    @Test
    void updateStatusErrorEhrNotFound() {

        UUID ehrId = UUID.fromString("ccf560ea-06dd-4c0b-815f-89b076de674a");
        ObjectVersionId ifMatch = new ObjectVersionId("10a0ec8e-c459-4a28-bad4-fbdc03593ac1", "some-system", "7");
        EhrStatusDto ehrStatusDto = ehrStatusDto(ifMatch, null);
        EhrService service = service();

        doThrow(new ObjectNotFoundException("EHR", "Test"))
                .when(ehrRepository)
                .update(eq(ehrId), any(), isNull(), isNull());

        assertThatThrownBy(() -> service.updateStatus(ehrId, ehrStatusDto, ifMatch, null, null))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void updateStatus() {

        UUID ehrId = UUID.fromString("ccf560ea-06dd-4c0b-815f-89b076de674a");
        ObjectVersionId ifMatch = new ObjectVersionId("10a0ec8e-c459-4a28-bad4-fbdc03593ac1", "some-system", "7");
        EhrStatusDto ehrStatusDto = ehrStatusDto(ifMatch, null);
        EhrService service = service();

        doReturn(true).when(ehrRepository).hasEhr(ehrId);

        EhrService.EhrResult ehrResult = service.updateStatus(ehrId, ehrStatusDto, ifMatch, null, null);
        ObjectVersionId versionId = ehrResult.statusVersionId();

        verify(validationService, times(1)).check(ehrStatusDto);
        verify(ehrRepository, times(1)).update(eq(ehrId), any(), isNull(), isNull());

        assertThat(versionId.getObjectId()).isEqualTo(ifMatch.getObjectId());
        assertThat(versionId.getVersionTreeId().getValue()).isEqualTo("8");
    }

    @Test
    void deleteUnmodifiableEhr() {
        UUID ehrId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        doReturn(null).when(ehrRepository).fetchIsModifiable(ehrId);
        assertThatThrownBy(() -> service().deleteEhr(ehrId))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("EHR with id %s not found".formatted(ehrId));

        doReturn(false).when(ehrRepository).fetchIsModifiable(ehrId);
        assertThatThrownBy(() -> service().deleteEhr(ehrId))
                .isInstanceOf(StateConflictException.class)
                .hasMessageContaining("EHR with id %s does not allow modification".formatted(ehrId));
    }

    @Test
    void deleteEhrWithoutCompositionsOrFolder() {
        UUID ehrId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID contributionId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID statusId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID auditId = UUID.fromString("66666666-6666-6666-6666-666666666666");

        EhrStatus ehrStatus = EhrStatusMapper.fromDto(
                ehrStatusDto(new ObjectVersionId(statusId.toString(), "test-ehr-service", "1"), new PartySelf()));

        doReturn(true).when(ehrRepository).fetchIsModifiable(ehrId);
        doReturn(contributionId).when(contributionRepository).createDefault(eq(ehrId), any(), any());
        doReturn(auditId).when(contributionRepository).createDefaultAudit(any(), any());
        doReturn(Collections.emptyList()).when(compositionRepository).findAllHeadVersionsForEhr(ehrId);
        doReturn(Optional.empty()).when(ehrFolderRepository).findHead(ehrId, 1);
        doReturn(Optional.of(ehrStatus)).when(ehrRepository).findHead(ehrId);

        UUID result = service().deleteEhr(ehrId);

        assertThat(result).isEqualTo(contributionId);

        verify(compositionRepository, never()).delete(any(), any(), anyInt(), any(), any());
        verify(ehrFolderRepository, never()).delete(any(), any(), anyInt(), anyInt(), any(), any());
        ArgumentCaptor<EhrStatus> statusCaptor = ArgumentCaptor.forClass(EhrStatus.class);
        verify(ehrRepository).update(eq(ehrId), statusCaptor.capture(), eq(contributionId), eq(auditId));

        EhrStatus updatedStatus = statusCaptor.getValue();
        assertThat(updatedStatus.isQueryable()).isFalse();
        assertThat(updatedStatus.isModifiable()).isFalse();
        assertThat(updatedStatus.getUid().toString()).contains("::2");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteEhrWithCompositionsAndFolder() {
        UUID ehrId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID contributionId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID statusId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID comp1Id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID comp2Id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID folderId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID compAuditId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID folderAuditId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        UUID statusAuditId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        EhrStatus ehrStatus = EhrStatusMapper.fromDto(
                ehrStatusDto(new ObjectVersionId(statusId.toString(), "test-ehr-service", "1"), new PartySelf()));

        Record2<UUID, Integer> comp1 = mock(Record2.class);
        doReturn(comp1Id).when(comp1).value1();
        doReturn(1).when(comp1).value2();

        Record2<UUID, Integer> comp2 = mock(Record2.class);
        doReturn(comp2Id).when(comp2).value1();
        doReturn(2).when(comp2).value2();

        Folder folder = new Folder();
        folder.setUid(new ObjectVersionId(folderId.toString(), "test-ehr-service", "1"));
        folder.setName(new DvText("root"));
        folder.setArchetypeNodeId("openEHR-EHR-FOLDER.generic.v1");

        doReturn(true).when(ehrRepository).fetchIsModifiable(ehrId);
        doReturn(contributionId).when(contributionRepository).createDefault(eq(ehrId), any(), any());
        doReturn(compAuditId)
                .when(contributionRepository)
                .createDefaultAudit(eq(ContributionChangeType.deleted), eq(AuditDetailsTargetType.COMPOSITION));
        doReturn(folderAuditId)
                .when(contributionRepository)
                .createDefaultAudit(eq(ContributionChangeType.deleted), eq(AuditDetailsTargetType.EHR_FOLDER));
        doReturn(statusAuditId)
                .when(contributionRepository)
                .createDefaultAudit(eq(ContributionChangeType.modification), eq(AuditDetailsTargetType.EHR_STATUS));

        doReturn(List.of(comp1, comp2)).when(compositionRepository).findAllHeadVersionsForEhr(ehrId);
        doReturn(Optional.of(folder)).when(ehrFolderRepository).findHead(ehrId, 1);
        doReturn(Optional.of(ehrStatus)).when(ehrRepository).findHead(ehrId);

        UUID result = service().deleteEhr(ehrId);

        assertThat(result).isEqualTo(contributionId);

        verify(compositionRepository).delete(ehrId, comp1Id, 1, contributionId, compAuditId);
        verify(compositionRepository).delete(ehrId, comp2Id, 2, contributionId, compAuditId);
        verify(ehrFolderRepository).delete(ehrId, folderId, 1, 1, contributionId, folderAuditId);

        ArgumentCaptor<EhrStatus> statusCaptor = ArgumentCaptor.forClass(EhrStatus.class);
        verify(ehrRepository).update(eq(ehrId), statusCaptor.capture(), eq(contributionId), eq(statusAuditId));

        EhrStatus updatedStatus = statusCaptor.getValue();
        assertThat(updatedStatus.isQueryable()).isFalse();
        assertThat(updatedStatus.isModifiable()).isFalse();
    }
}
