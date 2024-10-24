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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.ehrbase.jooq.pg.tables.records.EhrFolderVersionHistoryRecord;

class EhrFolderRepositoryTest
        extends AbstractVersionedObjectRepositoryUpdateTest<
                EhrFolderRepository, Folder, EhrFolderVersionHistoryRecord> {

    private static Folder folder(ObjectVersionId versionId) {
        Folder folder = new Folder();
        folder.setUid(versionId);
        return folder;
    }

    public EhrFolderRepositoryTest() {
        super(spy(new EhrFolderRepository(mock(), mock(), () -> SYSTEM_ID, OffsetDateTime::now)));
    }

    @Override
    protected Folder versionedObject(ObjectVersionId objectVersionId) {
        return folder(objectVersionId);
    }

    @Override
    protected String formatUpdateErrorNotExistMessage() {
        return "No FOLDER in ehr: %s".formatted(EHR_ID);
    }

    @Override
    protected EhrFolderVersionHistoryRecord versionRecord() {
        return new EhrFolderVersionHistoryRecord();
    }

    @Override
    protected void callUpdate(EhrFolderRepository repository, UUID ehrId, Folder versionedObject) {
        repository.update(ehrId, versionedObject, null, null, 1);
    }
}
