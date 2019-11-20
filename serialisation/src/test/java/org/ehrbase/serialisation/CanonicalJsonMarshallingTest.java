package org.ehrbase.serialisation;

import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rm.datastructures.ItemTree;
import com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

public class CanonicalJsonMarshallingTest {

    @Test
    public void UnmarshalMultimedia() throws IOException {

        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/sample_data/multimedia.json")));

        CanonicalJson cut = new CanonicalJson();
        DvMultimedia dvMultimedia = cut.unmarshal(value, DvMultimedia.class);

        assertNotNull(dvMultimedia);
    }

    @Test
    public void UnmarshalMultimediaElement() throws IOException {

        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/sample_data/element_multimedia.json")));

        CanonicalJson cut = new CanonicalJson();
        Element element = cut.unmarshal(value, Element.class);

        assertNotNull(element);
    }

    @Test
    public void UnmarshalItemTree() throws IOException {

        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/sample_data/item_tree_with_multimedia.json")));

        CanonicalJson cut = new CanonicalJson();
        ItemTree itemTree = cut.unmarshal(value, ItemTree.class);

        assertNotNull(itemTree);
    }
}
