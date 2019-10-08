package org.ehrbase.serialisation;

import org.ehrbase.test_data.folder.FolderTestDataCanonicalJson;
import org.ehrbase.test_data.folder.FolderTestDataCanonicalXML;
import com.nedap.archie.rm.directory.Folder;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class FolderSerializerTest {

    @Test
    public void marshalBasicJsonFolder() throws IOException {

        String value = IOUtils.toString(
                FolderTestDataCanonicalJson.SIMPLE_EMPTY_FOLDER.getStream(),
                UTF_8
        );
        CanonicalJson canonicalJson = new CanonicalJson();
        Folder folder = canonicalJson.unmarshal(value, Folder.class);

        String marshalled = canonicalJson.marshal(folder);

        assertThat(marshalled).isNotEmpty();
        assertThat(marshalled).containsSubsequence("\"_type\"", "\"FOLDER\"");
        assertThat(marshalled).containsSubsequence("\"name\"", "{", "\"_type\"", "\"DV_TEXT\"", "\"value\"", "\"Simple empty folder\"" );
    }

    @Test
    public void unmarshalBasicJsonFolder() throws IOException {

        String value = IOUtils.toString(
                FolderTestDataCanonicalJson.SIMPLE_EMPTY_FOLDER.getStream(),
                UTF_8
        );

        CanonicalJson canonicalJson = new CanonicalJson();
        Folder folder = canonicalJson.unmarshal(value, Folder.class);

        assertThat(folder.getNameAsString()).isEqualTo("Simple empty folder");
    }

    @Test
    @Ignore("Possible bug at Archie with missing XMLRootElement annotation for folders.")
    public void marshalBasicXmlFolder() throws IOException {

        String value = IOUtils.toString(
                FolderTestDataCanonicalXML.SIMPLE_EMPTY_FOLDER.getStrean(),
                UTF_8
        );

        CanonicalXML canonicalXML = new CanonicalXML();
        Folder folder = canonicalXML.unmarshal(value, Folder.class);

        String marshalled = canonicalXML.marshal(folder);

        assertThat(marshalled).containsSubsequence("<name>", "<value>", "Simple empty folder", "</value>", "</name>");
    }
}
