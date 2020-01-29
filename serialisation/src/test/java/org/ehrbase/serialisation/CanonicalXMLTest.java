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

package org.ehrbase.serialisation;

import org.ehrbase.test_data.composition.CompositionTestDataCanonicalXML;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.PartyRef;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class CanonicalXMLTest {

    @Test
    public void marshal() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalXML.ALL_TYPES.getStream(), UTF_8);

        CanonicalXML cut = new CanonicalXML();

        Composition composition = cut.unmarshal(value, Composition.class);

        String marshal = cut.marshal(composition);

        assertThat(marshal).isNotEmpty();
    }

    @Test
    public void unmarshal() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalXML.ALL_TYPES.getStream(), UTF_8);
        CanonicalXML cut = new CanonicalXML();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition).isNotNull();
        assertThat(composition.getArchetypeDetails().getTemplateId().getValue()).isEqualTo("test_all_types.en.v1");

    }

    @Test
    public void unmarshalWithDefaultSchema() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalXML.DIADEM_DEFAULT_SCHEMA.getStream(), UTF_8);
        CanonicalXML cut = new CanonicalXML();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition).isNotNull();
        //assertThat(composition.getArchetypeDetails().getTemplateId().getValue()).isEqualTo("test_all_types.en.v1");

    }

    @Test
    public void unmarshalWithDuplicatedSections() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalXML.REGISTRO_DE_ATENDIMENTO.getStream(), UTF_8);
        CanonicalXML cut = new CanonicalXML();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition).isNotNull();
        //assertThat(composition.getArchetypeDetails().getTemplateId().getValue()).isEqualTo("test_all_types.en.v1");

    }

    @Test
    public void marshalInline() {
        Folder folder = new Folder();
        folder.setName(new DvText("folder name"));
        folder.addItem(new PartyRef());
        CanonicalXML canonicalXML = new CanonicalXML();

        String inline = canonicalXML.marshalInline(folder, new QName(null, "folder"));
        System.out.println(inline);
    }
}