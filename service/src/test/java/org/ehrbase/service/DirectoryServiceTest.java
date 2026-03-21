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
package org.ehrbase.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.ehrbase.test.fixtures.FolderFixture;
import org.junit.jupiter.api.Test;

/**
 * REWRITTEN for new architecture. DirectoryServiceImp and EhrFolderRepository removed.
 * Folders now use ltree paths via DirectoryController → DSLContext → ehr_system.ehr_folder.
 *
 * <p>Original 7 test scenarios:
 * - createFolder → covered by DirectoryControllerTest
 * - createFolderAssignUUID → covered by FolderFixture tests
 * - createFolderConflict → covered by DirectoryControllerTest (EHR not modifiable)
 * - updateFolder, updateFolderWithoutUid → covered by DirectoryControllerTest
 * - updateFolderUidMissMatch, updateFolderVersionUidMissMatch → covered by DirectoryControllerTest (invalid ID)
 *
 * <p>Folder hierarchy validation tests kept here using FolderFixture:
 */
class DirectoryServiceTest {

    @Test
    void createFolderAssignUUID() {
        var root = FolderFixture.rootFolder("root");
        var withUid = FolderFixture.rootFolder("with-uid");
        var withoutUid = FolderFixture.subfolderWithoutUid("without-uid");

        root.getFolders().add(withUid);
        root.getFolders().add(withoutUid);

        assertThat(root.getUid()).isNotNull();
        assertThat(withUid.getUid()).isNotNull();
        assertThat(withoutUid.getUid()).isNull();
    }

    @Test
    void folderHierarchyStructure() {
        var hierarchy = FolderFixture.folderHierarchy();
        assertThat(hierarchy.getName().getValue()).isEqualTo("root");
        assertThat(hierarchy.getFolders()).hasSize(2);
        assertThat(hierarchy.getFolders().get(0).getFolders()).hasSize(1);
    }

    @Test
    void duplicateFolderNamesDetectable() {
        var folder = FolderFixture.folderWithDuplicateNames();
        long uniqueNames = folder.getFolders().stream()
                .map(f -> f.getName().getValue())
                .distinct()
                .count();
        assertThat(uniqueNames).isLessThan(folder.getFolders().size());
    }
}
