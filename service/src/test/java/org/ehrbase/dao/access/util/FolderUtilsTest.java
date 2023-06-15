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
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.test_data.folder.FolderTestDataCanonicalJson;
import org.junit.Test;

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
}
