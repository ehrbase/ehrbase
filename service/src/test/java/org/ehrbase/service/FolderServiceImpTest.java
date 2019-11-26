package org.ehrbase.service;

import com.nedap.archie.rm.directory.Folder;
import org.apache.commons.io.IOUtils;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.definitions.StructuredString;
import org.ehrbase.api.definitions.StructuredStringFormat;
import org.ehrbase.serialisation.CanonicalJson;
import org.ehrbase.test_data.folder.FolderTestDataCanonicalJson;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class FolderServiceImpTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public CacheRule cacheRule = new CacheRule();

    private FolderServiceImp folderService;
    private CanonicalJson cut = new CanonicalJson();

    @Before
    public void setUp() throws Exception {
        KnowledgeCacheService knowledgeCache = KnowledgeCacheHelper
                .buildKnowledgeCache(testFolder, cacheRule);
        this.folderService = new FolderServiceImp(knowledgeCache, null, KnowledgeCacheHelper.buildServerConfig());
    }

    @Ignore("the tested example contains the empty attributes serialized but the serialization does not print them. Decide about behaviour. " +
            "The RM does not constraint occurrences 1..1 so serialization can be empty for these attributes/collections.")
    @Test
    public void serializesFolderToJson() throws IOException {

        String value = IOUtils.toString(
                FolderTestDataCanonicalJson.SIMPLE_EMPTY_FOLDER.getStream(),
                UTF_8
        );
        Folder folder = cut.unmarshal(value, Folder.class);

        StructuredString result = folderService.serialize(folder, StructuredStringFormat.JSON);

        assertThat(result).isNotNull();
        assertThat(result.getFormat()).isEqualTo(StructuredStringFormat.JSON);
        assertThat(result.getValue()).isEqualToIgnoringWhitespace(value);

    }

    @Test
    @Ignore
    public void serializesFolderToXML() throws IOException {

        String value = IOUtils.toString(
                FolderTestDataCanonicalJson.SIMPLE_EMPTY_FOLDER.getStream(),
                UTF_8
        );
       Folder folder = cut.unmarshal(value, Folder.class);

       StructuredString result = folderService.serialize(folder, StructuredStringFormat.XML);
        StructuredString result2 = folderService.serialize(folder, StructuredStringFormat.XML);

       assertThat(result).isNotNull();
       assertThat(result.getFormat()).isEqualTo(StructuredStringFormat.XML);
       assertThat(result.getValue()).isEqualToIgnoringWhitespace ("<folder xsi:type=\"FOLDER\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><name><value>Simple empty folder</value></name></folder>");
    }
}
