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

import com.google.gson.JsonElement;
import org.ehrbase.test_data.composition.CompositionTestDataCanonicalXML;
import com.nedap.archie.rm.composition.Composition;
import org.ehrbase.ehr.encode.rawjson.LightRawJsonEncoder;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class DBEncodeTest {

    static CompositionTestDataCanonicalXML[] canonicals = {
            CompositionTestDataCanonicalXML.DIADEM,
            CompositionTestDataCanonicalXML.ALL_TYPES_FIXED,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_ACTION,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_ADMIN_ENTRY,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_EVALUATION,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_OBSERVATION_DEMO,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_OBSERVATION_PULSE,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_INSTRUCTION,
            CompositionTestDataCanonicalXML.RIPPLE_CONFORMANCE_FULL
    };

    /**
     * Encode an RM composition content part into its JSON DB equivalent (which is normally stored as a jsonb type)
     * then convert this JSON DB structure into a RAW (canonical) JSON that can be interpreted by ARCHIE.
     * Test the transformation render a valid RM composition.
     * NB. At this stage, the tests are fairly basic, what is needed is an actual comparison between the original
     * composition and the resulting one after the transformation cycle.
     *
     * @throws Exception
     */
    @Test
    public void testDBEncodeDecode() throws Exception {
        for (CompositionTestDataCanonicalXML compositionTestDataCanonicalXML: canonicals) {
            Composition composition = new CanonicalXML().unmarshal(IOUtils.toString(compositionTestDataCanonicalXML.getStream(), UTF_8),Composition.class);

            assertNotNull(composition);

            CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

            String db_encoded = compositionSerializerRawJson.dbEncode(composition);
            assertNotNull(db_encoded);

            String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

            //see if this can be interpreted by Archie
            Object object = new CanonicalJson().unmarshal(converted,Composition.class);

            assertTrue(object instanceof Composition);

            //check if encoded/decode carry the same name
            assertThat(composition.getName().getValue()).isEqualTo(((Composition) object).getName().getValue());

            String interpreted = new CanonicalXML().marshal((Composition) object);

            assertNotNull(interpreted);
        }
    }

    @Test
    public void testDBDecodeFullComposition() throws Exception {

        String db_encoded = IOUtils.resourceToString("/composition/canonical_json/full_composition.json", UTF_8);
        assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson(null);

        //see if this can be interpreted by Archie
        Object object = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        assertTrue(object instanceof Composition);

        String interpreted = new CanonicalXML().marshal((Composition) object);

        assertNotNull(interpreted);
    }

    @Test
    public void unmarshal_from_js_composition() throws IOException {
        String marshal = IOUtils.resourceToString("/composition/canonical_json/rawdb_composition.json", UTF_8);
        JsonElement converted = new LightRawJsonEncoder(marshal).encodeContentAsJson(null);
        Object object = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        assertTrue(object instanceof Composition);
    }
}
