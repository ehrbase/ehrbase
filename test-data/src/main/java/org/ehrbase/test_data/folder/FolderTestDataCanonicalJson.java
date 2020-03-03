/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.test_data.folder;

import java.io.InputStream;

public enum FolderTestDataCanonicalJson {

    SIMPLE_EMPTY_FOLDER("A simple empty folder with no sub folders", "simple_empty_folder.json"),
    FLAT_FOLDER_INSERT("A flat folder structure for testing simple inserts.", "flat_folder_insert.json"),
    NESTED_FOLDER("A folder with muliple nested folders", "nested_folder.json"),
    FOLDER_WITH_DUPLICATE_NAMES("Folder with two folders on the same sibling level with same names", "duplicate_folder_names.json"),
    FOLDER_WITHOUT_DUPLICATE_NAMES("Folder with two sub folders that have different names", "folder_without_duplicates.json");

    private final String description;
    private final String filename;

    FolderTestDataCanonicalJson(String description, String filename) {
        this.description = description;
        this.filename = filename;
    }

    public InputStream getStream() {
        return getClass().getResourceAsStream("/folder/canonical_json/" + filename);
    }
}
