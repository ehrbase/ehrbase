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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.HierObjectId;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.repository.EhrFolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DirectoryServiceTest {

    private static final UUID EHR_ID = UUID.fromString("4c5a39d4-7562-42a2-9ddf-771e2a61417d");
    private static final String FOLDER_ID = "fc7e5443-eded-452c-a3cf-15c8d623e2b0";

    private final EhrService mockEhrService = mock();

    private final EhrFolderRepository mockEhrFolderRepository = mock();

    private DirectoryServiceImp service() {
        return new DirectoryServiceImp(() -> "test-system", mockEhrService, mockEhrFolderRepository);
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(mockEhrService, mockEhrFolderRepository);
    }

    private Folder folder(String name, Folder... subfolder) {
        Folder folder = new Folder();
        folder.setName(new DvText(name));
        folder.setUid(new HierObjectId(FOLDER_ID));
        folder.setFolders(Arrays.stream(subfolder).toList());
        return folder;
    }

    @Test
    void createFolder() {

        Folder folder = folder("test");
        doReturn(Optional.of(folder)).when(mockEhrFolderRepository).findHead(EHR_ID, 1);

        Folder created = service().create(EHR_ID, folder);
        assertThat(created).isSameAs(folder);

        verify(mockEhrService, times(1)).checkEhrExistsAndIsModifiable(EHR_ID);
    }

    @Test
    void createFolderAssignUUID() {

        Folder subFolderWithUID = folder("with-uid");
        subFolderWithUID.setUid(new HierObjectId("541f4a12-d28e-4886-a044-0838fa4352de"));
        Folder subFolderWithoutUID = folder("no-uid");
        subFolderWithoutUID.setUid(null);

        Folder folder = folder("root", subFolderWithUID, subFolderWithoutUID);
        doReturn(Optional.of(folder)).when(mockEhrFolderRepository).findHead(EHR_ID, 1);

        Folder created = service().create(EHR_ID, folder);
        assertThat(created.getFolders().get(0)).satisfies(f -> {
            assertThat(f.getName()).isEqualTo(new DvText("with-uid"));
            assertThat(f.getUid()).isEqualTo(new HierObjectId("541f4a12-d28e-4886-a044-0838fa4352de"));
        });
        assertThat(created.getFolders().get(1)).satisfies(f -> {
            assertThat(f.getName()).isEqualTo(new DvText("no-uid"));
            assertThat(f.getUid())
                    .isNotNull()
                    .isInstanceOf(HierObjectId.class)
                    .satisfies(uid -> assertDoesNotThrow(
                            () -> UUID.fromString(uid.getRoot().getValue())));
        });
    }

    @Test
    void createFolderConflict() {

        Folder folder = folder("conflict");
        UUID folderID = UUID.fromString(folder.getUid().getRoot().getValue());
        doReturn(true).when(mockEhrFolderRepository).folderUidExist(folderID);

        assertThatThrownBy(() -> service().create(EHR_ID, folder))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("FOLDER with uid %s already exist.".formatted(FOLDER_ID));
    }
}
