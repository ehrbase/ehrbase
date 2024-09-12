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
package org.ehrbase.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.jooq.pg.tables.records.EhrStatusVersionHistoryRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class EhrRepositoryTest {

    private static final UUID EHR_ID = UUID.fromString("e262b9f8-fa03-48e6-a5e4-0a43f67a6439");
    private static final UUID EHR_STATUS_ID = UUID.fromString("2a1d68a6-fbf8-44c5-8a81-732edf73d103");

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2024-09-12T12:00:00Z");

    private final EhrRepository repository = spy(new EhrRepository(mock(), mock(), () -> "test.system", () -> NOW));

    @BeforeEach
    void setUp() {
        Mockito.reset(repository);
    }

    private static EhrStatus ehrStatus(ObjectVersionId versionId) {
        return new EhrStatus(
                versionId,
                "openEHR-EHR-EHR_STATUS.generic.v1",
                new DvText("EHR Status"),
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                true,
                null);
    }

    @Test
    void updateEhrStatusErrorEhrNotExist() {

        ObjectVersionId ehrStatusVoid = new ObjectVersionId(EHR_STATUS_ID.toString(), "test.ehr.repository", "2");
        EhrStatus ehrStatus = ehrStatus(ehrStatusVoid);

        EhrStatusVersionHistoryRecord versionRecord = new EhrStatusVersionHistoryRecord();
        versionRecord.setSysVersion(2);
        versionRecord.setVoId(EHR_STATUS_ID);
        doReturn(Optional.empty()).when(repository).findVersionHeadRecord(EHR_ID);

        var message = assertThrows(
                        ObjectNotFoundException.class, () -> repository.update(EHR_ID, ehrStatus, null, null))
                .getMessage();
        assertEquals("EHR e262b9f8-fa03-48e6-a5e4-0a43f67a6439 does not exist", message);
    }

    @Test
    void updateEhrStatusErrorVoidMissmatch() {

        ObjectVersionId invalidEhrStatusVoid =
                new ObjectVersionId("12c78941-f7cb-4b73-83b2-49f230d82923", "test.ehr.repository", "2");
        EhrStatus ehrStatus = ehrStatus(invalidEhrStatusVoid);

        EhrStatusVersionHistoryRecord versionRecord = new EhrStatusVersionHistoryRecord();
        versionRecord.setSysVersion(2);
        versionRecord.setVoId(EHR_STATUS_ID);
        doReturn(Optional.of(versionRecord)).when(repository).findVersionHeadRecord(EHR_ID);

        var message = assertThrows(
                        PreconditionFailedException.class, () -> repository.update(EHR_ID, ehrStatus, null, null))
                .getMessage();
        assertEquals(
                "No EHR_STATUS exist for If-Match version_uid 12c78941-f7cb-4b73-83b2-49f230d82923::test.ehr.repository::2",
                message);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void updateEhrStatusErrorVersionMatch(Integer version) {

        ObjectVersionId ehrStatusVoid =
                new ObjectVersionId(EHR_STATUS_ID.toString(), "test.ehr.repository", version.toString());
        EhrStatus ehrStatus = ehrStatus(ehrStatusVoid);

        EhrStatusVersionHistoryRecord versionRecord = new EhrStatusVersionHistoryRecord();
        versionRecord.setSysVersion(1);
        versionRecord.setVoId(EHR_STATUS_ID);
        doReturn(Optional.of(versionRecord)).when(repository).findVersionHeadRecord(EHR_ID);

        var message = assertThrows(
                        PreconditionFailedException.class, () -> repository.update(EHR_ID, ehrStatus, null, null))
                .getMessage();
        assertEquals("If-Match version_uid does not match latest version", message);
    }

    @Test
    void updateEhrStatusSucceed() {

        ObjectVersionId ehrStatusVoid = new ObjectVersionId(EHR_STATUS_ID.toString(), "test.ehr.repository", "2");
        EhrStatus ehrStatus = ehrStatus(ehrStatusVoid);

        EhrStatusVersionHistoryRecord versionRecord = new EhrStatusVersionHistoryRecord();
        versionRecord.setSysVersion(1);
        versionRecord.setVoId(EHR_STATUS_ID);

        doReturn(Optional.of(versionRecord)).when(repository).findVersionHeadRecord(EHR_ID);
        doNothing().when(repository).copyHeadToHistory(versionRecord, NOW);
        doNothing().when(repository).deleteHead(any(), eq(1), any());
        doNothing().when(repository).commitHead(eq(EHR_ID), eq(ehrStatus), any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> repository.update(EHR_ID, ehrStatus, null, null));
    }
}
