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
package org.ehrbase.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.nedap.archie.rm.directory.Folder;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.ehrbase.response.ehrscape.StructuredString;
import org.ehrbase.response.ehrscape.StructuredStringFormat;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.test_data.folder.FolderTestDataCanonicalJson;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FolderServiceImpTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public CacheRule cacheRule = new CacheRule();

    private FolderServiceImp folderService;
    private CanonicalJson cut = new CanonicalJson();

    @Before
    public void setUp() throws Exception {
        KnowledgeCacheService knowledgeCache = KnowledgeCacheHelper.buildKnowledgeCache(testFolder, cacheRule);
        this.folderService =
                new FolderServiceImp(knowledgeCache, null, KnowledgeCacheHelper.buildServerConfig(), null, null);
    }

    @Ignore(
            "the tested example contains the empty attributes serialized but the serialization does not print them. Decide about behaviour. "
                    + "The RM does not constraint occurrences 1..1 so serialization can be empty for these attributes/collections.")
    @Test
    public void serializesFolderToJson() throws IOException {

        String value = IOUtils.toString(FolderTestDataCanonicalJson.SIMPLE_EMPTY_FOLDER.getStream(), UTF_8);
        Folder folder = cut.unmarshal(value, Folder.class);

        StructuredString result = folderService.serialize(folder, StructuredStringFormat.JSON);

        assertThat(result).isNotNull();
        assertThat(result.getFormat()).isEqualTo(StructuredStringFormat.JSON);
        assertThat(result.getValue()).isEqualToIgnoringWhitespace(value);
    }

    @Test
    @Ignore
    public void serializesFolderToXML() throws IOException {

        String value = IOUtils.toString(FolderTestDataCanonicalJson.SIMPLE_EMPTY_FOLDER.getStream(), UTF_8);
        Folder folder = cut.unmarshal(value, Folder.class);

        StructuredString result = folderService.serialize(folder, StructuredStringFormat.XML);
        StructuredString result2 = folderService.serialize(folder, StructuredStringFormat.XML);

        assertThat(result).isNotNull();
        assertThat(result.getFormat()).isEqualTo(StructuredStringFormat.XML);
        assertThat(result.getValue())
                .isEqualToIgnoringWhitespace(
                        "<folder xsi:type=\"FOLDER\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><name><value>Simple empty folder</value></name></folder>");
    }
}
