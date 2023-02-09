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

import com.nedap.archie.rm.directory.Folder;
import java.util.HashSet;
import java.util.Set;

public class FolderUtils {

    private FolderUtils() {}

    /**
     * Checks each sub folder level for conflicts. For this purpose for each sub folder level there will be a set
     * created that contains all names of the siblings as values. If at least one value could not be inserted it will
     * be identified as duplicate and will throw an IllegalArgumentException that results to a 400 error on the
     * controller layer.
     *
     * @param folder - Folder to check sub folders for
     */
    public static void checkSiblingNameConflicts(Folder folder) {

        if (folder.getFolders() != null && !folder.getFolders().isEmpty()) {
            Set<String> folderNames = new HashSet<>();

            folder.getFolders().forEach(subFolder -> {

                // A new entry in the set results to false if there is already a duplicate element existing
                if (!folderNames.add(subFolder.getNameAsString())) {
                    throw new IllegalArgumentException("Duplicate folder name " + subFolder.getNameAsString());
                } else {
                    // Check sub folder hierarchies as well for duplicates
                    checkSiblingNameConflicts(subFolder);
                }
            });
        }
    }
}
