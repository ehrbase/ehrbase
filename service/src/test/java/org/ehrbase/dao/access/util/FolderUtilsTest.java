/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.dao.access.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.test_data.folder.FolderTestDataCanonicalJson;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

public class FolderUtilsTest {

    CanonicalJson canonicalJson = new CanonicalJson();

    @Test(expected = IllegalArgumentException.class)
    public void detectsDuplicateFolderNames() throws IOException {

        String value = IOUtils.toString(FolderTestDataCanonicalJson.FOLDER_WITH_DUPLICATE_NAMES.getStream(), UTF_8);
        Folder testFolder = canonicalJson.unmarshal(value, Folder.class);

        FolderUtils.checkSiblingNameConflicts(testFolder);
    }

    @Test
    public void acceptsFoldersWithoutConflicts() throws IOException {

        String value = IOUtils.toString(FolderTestDataCanonicalJson.FOLDER_WITHOUT_DUPLICATE_NAMES.getStream(), UTF_8);
        Folder testFolder = canonicalJson.unmarshal(value, Folder.class);

        FolderUtils.checkSiblingNameConflicts(testFolder);
    }

    @Test
    public void doesAnySubFolderIdMatch() {
        I_FolderAccess currentDir = Mockito.mock(I_FolderAccess.class);
        I_FolderAccess sub1 = Mockito.mock(I_FolderAccess.class);
        I_FolderAccess sub2 = Mockito.mock(I_FolderAccess.class);
        I_FolderAccess sub21 = Mockito.mock(I_FolderAccess.class);

        UUID sub21Uuid = UUID.randomUUID();
        UUID sub2Uuid = UUID.randomUUID();
        UUID sub1Uuid = UUID.randomUUID();
        Mockito.when(sub21.getFolderId()).thenReturn(sub21Uuid);
        Mockito.when(sub21.getSubfoldersList()).thenReturn(Map.of());
        Mockito.when(sub2.getFolderId()).thenReturn(sub2Uuid);
        Mockito.when(sub2.getSubfoldersList()).thenReturn(Map.of(sub21Uuid, sub21));
        Mockito.when(sub1.getFolderId()).thenReturn(sub1Uuid);
        Mockito.when(sub1.getSubfoldersList()).thenReturn(Map.of());
        Mockito.when(currentDir.getFolderId()).thenReturn(UUID.randomUUID());
        Mockito.when(currentDir.getSubfoldersList()).thenReturn(Map.of(sub1Uuid, sub1, sub2Uuid, sub2));

        // Make sure we can find all folders
        Assertions.assertTrue(FolderUtils.doesAnyIdInFolderStructureMatch(
                currentDir, new ObjectVersionId(currentDir.getFolderId().toString())));
        Assertions.assertTrue(FolderUtils.doesAnyIdInFolderStructureMatch(
                currentDir, new ObjectVersionId(sub1.getFolderId().toString())));
        Assertions.assertTrue(FolderUtils.doesAnyIdInFolderStructureMatch(
                currentDir, new ObjectVersionId(sub2.getFolderId().toString())));
        Assertions.assertTrue(FolderUtils.doesAnyIdInFolderStructureMatch(
                currentDir, new ObjectVersionId(sub21.getFolderId().toString())));

        // a random UUID should return false
        Assertions.assertFalse(FolderUtils.doesAnyIdInFolderStructureMatch(
                currentDir, new ObjectVersionId(UUID.randomUUID().toString())));
        Assertions.assertFalse(FolderUtils.doesAnyIdInFolderStructureMatch(
                null, new ObjectVersionId(UUID.randomUUID().toString())));
        Assertions.assertFalse(FolderUtils.doesAnyIdInFolderStructureMatch(currentDir, null));
    }
}
