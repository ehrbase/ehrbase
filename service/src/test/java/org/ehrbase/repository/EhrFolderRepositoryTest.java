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

import com.nedap.archie.rm.datastructures.ItemSingle;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.jooq.pg.tables.records.EhrFolderVersionHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.EhrStatusVersionHistoryRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class EhrFolderRepositoryTest {

    private static final UUID EHR_ID = UUID.fromString("8276b318-b6f9-411f-8443-8330b502a5a4");
    private static final UUID EHR_FOLDER_ID = UUID.fromString("5dcdbc78-8b62-4792-adaa-9ac4c41f39ab");

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2024-09-12T12:00:00Z");

    private final EhrFolderRepository repository =
            spy(new EhrFolderRepository(mock(), mock(), () -> "test.system", () -> NOW));

    @BeforeEach
    void setUp() {
        Mockito.reset(repository);
    }

    private static Folder folder(ObjectVersionId versionId) {
        return new Folder(
                versionId,
                "openEHR-EHR-EHR_STATUS.generic.v1",
                new DvText("EHR Status"),
                new ItemSingle(),
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void updateFolderErrorEhrNotExist() {

        ObjectVersionId folderVoid = new ObjectVersionId(EHR_FOLDER_ID.toString(), "test.folder.repository", "2");
        Folder folder = folder(folderVoid);

        EhrStatusVersionHistoryRecord versionRecord = new EhrStatusVersionHistoryRecord();
        versionRecord.setSysVersion(2);
        versionRecord.setVoId(EHR_FOLDER_ID);
        doReturn(Optional.empty()).when(repository).findVersionHeadRecord(EHR_ID, 1);

        var message = assertThrows(
                        ObjectNotFoundException.class, () -> repository.update(EHR_ID, folder, null, null, 1))
                .getMessage();
        assertEquals("EHR 8276b318-b6f9-411f-8443-8330b502a5a4 does not exist", message);
    }

    @Test
    void updateEhrStatusErrorVoidMissmatch() {

        ObjectVersionId invalidEhrStatusVoid =
                new ObjectVersionId("fb97d05e-612a-42f1-888f-6e2ffa7f3290", "test.folder.repository", "2");
        Folder folder = folder(invalidEhrStatusVoid);

        EhrFolderVersionHistoryRecord versionRecord = new EhrFolderVersionHistoryRecord();
        versionRecord.setSysVersion(2);
        versionRecord.setVoId(EHR_FOLDER_ID);
        doReturn(Optional.of(versionRecord)).when(repository).findVersionHeadRecord(EHR_ID, 1);

        var message = assertThrows(
                        PreconditionFailedException.class, () -> repository.update(EHR_ID, folder, null, null, 1))
                .getMessage();
        assertEquals(
                "No FOLDER exist for If-Match version_uid fb97d05e-612a-42f1-888f-6e2ffa7f3290::test.folder.repository::2",
                message);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void updateEhrStatusErrorVersionMatch(Integer version) {

        ObjectVersionId ehrStatusVoid =
                new ObjectVersionId(EHR_FOLDER_ID.toString(), "test.folder.repository", version.toString());
        Folder folder = folder(ehrStatusVoid);

        EhrFolderVersionHistoryRecord versionRecord = new EhrFolderVersionHistoryRecord();
        versionRecord.setSysVersion(1);
        versionRecord.setVoId(EHR_FOLDER_ID);
        doReturn(Optional.of(versionRecord)).when(repository).findVersionHeadRecord(EHR_ID, 1);

        var message = assertThrows(
                        PreconditionFailedException.class, () -> repository.update(EHR_ID, folder, null, null, 1))
                .getMessage();
        assertEquals("If-Match version_uid does not match latest version", message);
    }

    @Test
    void updateEhrStatusSucceed() {

        ObjectVersionId ehrStatusVoid = new ObjectVersionId(EHR_FOLDER_ID.toString(), "test.ehr.repository", "2");
        Folder folder = folder(ehrStatusVoid);

        EhrFolderVersionHistoryRecord versionRecord = new EhrFolderVersionHistoryRecord();
        versionRecord.setSysVersion(1);
        versionRecord.setVoId(EHR_FOLDER_ID);

        doReturn(Optional.of(versionRecord)).when(repository).findVersionHeadRecord(EHR_ID, 1);
        doNothing()
                .when(repository)
                .update(
                        eq(EHR_ID),
                        eq(folder),
                        any(),
                        any(),
                        eq(null),
                        eq(null),
                        any(),
                        any(),
                        eq("No Directory in ehr: %s".formatted(EHR_ID)));

        assertDoesNotThrow(() -> repository.update(EHR_ID, folder, null, null, 1));
    }
}
