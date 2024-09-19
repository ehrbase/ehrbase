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
package org.ehrbase.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.UpdatableRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

@SuppressWarnings({"unchecked", "rawtypes", "java:S119" // VH is ok
})
public abstract class AbstractVersionedObjectRepositoryUpdateTest<
        R extends AbstractVersionedObjectRepository, O extends Locatable, VH extends UpdatableRecord> {

    protected static final UUID EHR_ID = UUID.fromString("8276b318-b6f9-411f-8443-8330b502a5a4");
    protected static final UUID VERSIONED_OBJECT_ID = UUID.fromString("5dcdbc78-8b62-4792-adaa-9ac4c41f39ab");
    protected static final String SYSTEM_ID = "test.versioned-object.repository";

    protected static ObjectVersionId objectVersionId(int version) {
        return objectVersionId(version, SYSTEM_ID);
    }

    protected static ObjectVersionId objectVersionId(int version, String systemId) {
        return new ObjectVersionId(VERSIONED_OBJECT_ID.toString(), systemId, Integer.toString(version));
    }

    protected static <T extends Record> Result<T> mockResult(T... records) {
        @SuppressWarnings("unchecked")
        Result<T> result = mock(Result.class);

        doReturn(records.length).when(result).size();
        if (records.length == 0) {
            doReturn(true).when(result).isEmpty();
        } else {
            doReturn(records[0]).when(result).getFirst();
        }
        return result;
    }

    protected VH versionRecord(Consumer<VH> block) {
        VH versionRecord = versionRecord();
        versionRecord.set(AbstractVersionedObjectRepository.VERSION_PROTOTYPE.SYS_VERSION, 1);
        versionRecord.set(AbstractVersionedObjectRepository.VERSION_PROTOTYPE.VO_ID, VERSIONED_OBJECT_ID);
        versionRecord.set(AbstractVersionedObjectRepository.VERSION_PROTOTYPE.SYS_PERIOD_LOWER, OffsetDateTime.now());
        block.accept(versionRecord);
        return versionRecord;
    }

    protected abstract O versionedObject(ObjectVersionId objectVersionId);

    protected abstract void callUpdate(R repository, UUID ehrId, O versionedObject);

    protected abstract VH versionRecord();

    protected abstract String formatUpdateErrorNotExistMessage();

    protected final R repository;

    public AbstractVersionedObjectRepositoryUpdateTest(R repository) {
        this.repository = repository;
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(repository);
    }

    @Test
    void updateErrorEhrNotExist() {

        O versionedObject = versionedObject(objectVersionId(1));
        doReturn(false).when(repository).hasEhr(EHR_ID);

        assertThatThrownBy(() -> callUpdate(repository, EHR_ID, versionedObject))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("EHR 8276b318-b6f9-411f-8443-8330b502a5a4 does not exist");
    }

    @Test
    void updateErrorNotExist() {

        O versionedObject = versionedObject(objectVersionId(1));

        doReturn(true).when(repository).hasEhr(EHR_ID);
        doReturn(mockResult()).when(repository).findVersionHeadRecords(any());
        doReturn(Optional.empty()).when(repository).findLatestHistoryRoot(any());

        assertThatThrownBy(() -> callUpdate(repository, EHR_ID, versionedObject))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage(formatUpdateErrorNotExistMessage());
    }

    @Test
    void updateErrorVoidMissmatch() {

        O versionedObject = versionedObject(objectVersionId(1));

        VH versionRecord = versionRecord(r -> {
            r.set(
                    AbstractVersionedObjectRepository.VERSION_PROTOTYPE.VO_ID,
                    UUID.fromString("fb97d05e-612a-42f1-888f-6e2ffa7f3290"));
        });

        doReturn(true).when(repository).hasEhr(EHR_ID);
        doReturn(mockResult(versionRecord)).when(repository).findVersionHeadRecords(any());

        assertThatThrownBy(() -> callUpdate(repository, EHR_ID, versionedObject))
                .isInstanceOf(PreconditionFailedException.class)
                .hasMessage("If-Match version_uid does not match uid");
    }

    @Test
    void updateFolderErrorSystemIdMissmatch() {

        O versionedObject = versionedObject(objectVersionId(1, "other-system"));
        VH versionRecord = versionRecord(r -> {});

        doReturn(true).when(repository).hasEhr(EHR_ID);
        doReturn(mockResult(versionRecord)).when(repository).findVersionHeadRecords(any());

        assertThatThrownBy(() -> callUpdate(repository, EHR_ID, versionedObject))
                .isInstanceOf(PreconditionFailedException.class)
                .hasMessage("If-Match version_uid does not match system id");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void updateFolderErrorVersionMatch(int version) {

        O versionedObject = versionedObject(objectVersionId(version));
        VH versionRecord = versionRecord(r -> {
            r.set(AbstractVersionedObjectRepository.VERSION_PROTOTYPE.SYS_VERSION, 1);
        });

        doReturn(true).when(repository).hasEhr(EHR_ID);
        doReturn(mockResult(versionRecord)).when(repository).findVersionHeadRecords(any());

        assertThatThrownBy(() -> callUpdate(repository, EHR_ID, versionedObject))
                .isInstanceOf(PreconditionFailedException.class)
                .hasMessage("If-Match version_uid does not match latest version");
    }

    @Test
    void updateFolderSucceed() {

        O versionedObject = versionedObject(objectVersionId(2));
        VH versionRecord = versionRecord(r -> {
            r.set(AbstractVersionedObjectRepository.VERSION_PROTOTYPE.SYS_VERSION, 1);
        });

        doReturn(true).when(repository).hasEhr(EHR_ID);
        doReturn(mockResult(versionRecord)).when(repository).findVersionHeadRecords(any());

        doNothing().when(repository).copyHeadToHistory(eq(versionRecord), any());
        doNothing().when(repository).deleteHead(any(), eq(1), any());
        doNothing().when(repository).commitHead(eq(EHR_ID), eq(versionedObject), any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> callUpdate(repository, EHR_ID, versionedObject));
    }
}
