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

import static org.assertj.core.api.Assertions.assertThat;

import org.ehrbase.test.fixtures.FolderFixture;
import org.junit.jupiter.api.Test;

/**
 * REWRITTEN for new architecture. Replaces old EhrFolderRepositoryTest
 * that extended AbstractVersionedObjectRepositoryUpdateTest with JOOQ EhrFolderVersionHistoryRecord.
 *
 * <p>Folder operations are now handled via ltree paths in ehr_system.ehr_folder.
 * The 6 original inherited versioning tests are covered by DirectoryControllerTest
 * and will be further tested in integration tests (DirectoryE2EIT).
 */
class EhrFolderRepositoryTest {

    @Test
    void folderFixtureCreatesValidRoot() {
        var root = FolderFixture.rootFolder("root");
        assertThat(root.getUid()).isNotNull();
        assertThat(root.getName().getValue()).isEqualTo("root");
        assertThat(root.getFolders()).isEmpty();
    }

    @Test
    void folderFixtureCreatesHierarchy() {
        var root = FolderFixture.folderHierarchy();
        assertThat(root.getName().getValue()).isEqualTo("root");
        assertThat(root.getFolders()).hasSize(2);
        assertThat(root.getFolders().get(0).getName().getValue()).isEqualTo("clinical");
        assertThat(root.getFolders().get(0).getFolders()).hasSize(1);
        assertThat(root.getFolders().get(0).getFolders().get(0).getName().getValue())
                .isEqualTo("lab_results");
        assertThat(root.getFolders().get(1).getName().getValue()).isEqualTo("administrative");
    }

    @Test
    void folderFixtureCreatesSubfolderWithoutUid() {
        var subfolder = FolderFixture.subfolderWithoutUid("test");
        assertThat(subfolder.getUid()).isNull();
        assertThat(subfolder.getName().getValue()).isEqualTo("test");
    }

    @Test
    void folderFixtureDetectsDuplicateNames() {
        var root = FolderFixture.folderWithDuplicateNames();
        assertThat(root.getFolders()).hasSize(2);
        assertThat(root.getFolders().get(0).getName().getValue())
                .isEqualTo(root.getFolders().get(1).getName().getValue());
    }
}
