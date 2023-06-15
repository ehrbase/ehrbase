/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.dbencoding;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datastructures.ItemTree;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.serialisation.xmlencoding.CanonicalXML;
import org.ehrbase.openehr.sdk.test_data.composition.CompositionTestDataCanonicalJson;
import org.ehrbase.openehr.sdk.test_data.composition.CompositionTestDataCanonicalXML;
import org.ehrbase.openehr.sdk.test_data.item_structure.ItemStruktureTestDataCanonicalJson;
import org.junit.jupiter.api.Test;

public class RawJsonTest {

    @Test
    public void marshal() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalXML.DIADEM.getStream(), UTF_8);

        CanonicalXML canonicalXML = new CanonicalXML();

        Composition composition = canonicalXML.unmarshal(value, Composition.class);

        RawJson cut = new RawJson();

        String marshal = cut.marshal(composition);

        Assertions.assertThat(marshal).isNotEmpty();
    }

    @Test
    public void testMarshalItemStructure() throws IOException {

        String value = IOUtils.toString(ItemStruktureTestDataCanonicalJson.SIMPLE_EHR_OTHER_Details.getStream(), UTF_8);

        CanonicalJson canonicalJson = new CanonicalJson();

        ItemStructure itemTree = canonicalJson.unmarshal(value, ItemStructure.class);

        RawJson cut = new RawJson();

        String marshal = cut.marshal(itemTree);

        Assertions.assertThat(marshal).isNotEmpty();
    }

    @Test
    public void testUnmarshalItemStructure() throws IOException {

        String value = IOUtils.toString(ItemStruktureTestDataCanonicalJson.SIMPLE_EHR_OTHER_Details.getStream(), UTF_8);

        CanonicalJson canonicalJson = new CanonicalJson();

        ItemTree itemTree = canonicalJson.unmarshal(value, ItemTree.class);

        RawJson cut = new RawJson();

        String marshal = cut.marshal(itemTree);

        ItemTree actual = cut.unmarshal(marshal, ItemTree.class);

        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.getItems()).size().isEqualTo(3);
    }

    @Test
    public void unmarshal() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalXML.DIADEM.getStream(), UTF_8);

        CanonicalXML canonicalXML = new CanonicalXML();

        Composition composition = canonicalXML.unmarshal(value, Composition.class);

        RawJson cut = new RawJson();

        String marshal = cut.marshal(composition);

        Composition actual = cut.unmarshal(marshal, Composition.class);

        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(composition.getName().getValue()).isEqualTo("DiADeM Assessment");
    }

    @Test
    public void unmarshal2() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalXML.ALL_TYPES_FIXED.getStream(), UTF_8);

        CanonicalXML canonicalXML = new CanonicalXML();

        Composition composition = canonicalXML.unmarshal(value, Composition.class);

        RawJson cut = new RawJson();

        String marshal = cut.marshal(composition);

        Composition actual = cut.unmarshal(marshal, Composition.class);

        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(composition.getName().getValue()).isEqualTo("Test all types");
    }

    @Test
    public void marshallEmptyState() throws Exception {
        String json = IOUtils.toString(CompositionTestDataCanonicalJson.GECCO_LABORBEFUND.getStream(), UTF_8);
        Composition composition = new CanonicalJson().unmarshal(json, Composition.class);

        RawJson rawJson = new RawJson();
        String actual = rawJson.marshal(composition);

        Assertions.assertThat(actual).isNotNull();
    }
}
