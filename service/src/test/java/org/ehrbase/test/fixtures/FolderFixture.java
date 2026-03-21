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
package org.ehrbase.test.fixtures;

import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.HierObjectId;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Factory methods for creating Folder test data with ltree paths.
 */
public final class FolderFixture {

    private FolderFixture() {}

    /**
     * Creates a root folder with the given name.
     */
    public static Folder rootFolder(String name) {
        var folder = new Folder();
        folder.setUid(new HierObjectId(UUID.randomUUID().toString()));
        folder.setName(new DvText(name));
        folder.setArchetypeNodeId("openEHR-EHR-FOLDER.generic.v1");
        folder.setFolders(new ArrayList<>());
        return folder;
    }

    /**
     * Creates a folder hierarchy: root → child1 → grandchild, root → child2.
     */
    public static Folder folderHierarchy() {
        var root = rootFolder("root");
        var child1 = rootFolder("clinical");
        var child2 = rootFolder("administrative");
        var grandchild = rootFolder("lab_results");

        child1.getFolders().add(grandchild);
        root.getFolders().add(child1);
        root.getFolders().add(child2);
        return root;
    }

    /**
     * Creates a folder with duplicate sibling names (for conflict testing).
     */
    public static Folder folderWithDuplicateNames() {
        var root = rootFolder("root");
        root.getFolders().add(rootFolder("duplicate"));
        root.getFolders().add(rootFolder("duplicate"));
        return root;
    }

    /**
     * Creates a subfolder without UID (for auto-assign testing).
     */
    public static Folder subfolderWithoutUid(String name) {
        var folder = new Folder();
        folder.setName(new DvText(name));
        folder.setArchetypeNodeId("openEHR-EHR-FOLDER.generic.v1");
        folder.setFolders(new ArrayList<>());
        return folder;
    }
}
