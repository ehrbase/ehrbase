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
import com.nedap.archie.rm.composition.AdminEntry;
import com.nedap.archie.rm.composition.ContentItem;
import com.nedap.archie.rm.composition.Evaluation;
import com.nedap.archie.rm.datastructures.Element;

import com.nedap.archie.rm.datastructures.ItemStructure;

import com.nedap.archie.rm.datastructures.History;
import com.nedap.archie.rm.datastructures.PointEvent;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.DvInterval;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.ehrbase.test_data.composition.CompositionTestDataCanonicalJson;
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

    private static CompositionTestDataCanonicalXML[] canonicals = {
            CompositionTestDataCanonicalXML.DIADEM,
            CompositionTestDataCanonicalXML.ALL_TYPES_FIXED,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_ACTION,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_ADMIN_ENTRY,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_EVALUATION,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_OBSERVATION_DEMO,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_OBSERVATION_PULSE,
            CompositionTestDataCanonicalXML.RIPPLE_COMFORMANCE_INSTRUCTION,
            CompositionTestDataCanonicalXML.RIPPLE_CONFORMANCE_FULL,
            CompositionTestDataCanonicalXML.ALL_TYPES_NO_CONTENT
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
            Composition object = new CanonicalJson().unmarshal(converted,Composition.class);

            assertNotNull(object);

            //check if encoded/decode carry the same name
            assertThat(composition.getName().getValue()).isEqualTo(object.getName().getValue());

            String interpreted = new CanonicalXML().marshal(object);

            assertNotNull(interpreted);
        }
    }

    @Test
    public void testDBDecodeFullComposition() throws Exception {

        String db_encoded = IOUtils.resourceToString("/composition/canonical_json/full_composition.json", UTF_8);
        assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson(null);

        //see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        assertNotNull(object);

        String interpreted = new CanonicalXML().marshal(object);

        assertNotNull(interpreted);
    }

    @Test
    public void unmarshal_from_js_composition() throws IOException {
        String marshal = IOUtils.resourceToString("/composition/canonical_json/rawdb_composition.json", UTF_8);
        JsonElement converted = new LightRawJsonEncoder(marshal).encodeContentAsJson(null);
        Object object = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        assertNotNull(object);
    }

    @Test
    public void unmarshal_from_js_composition_observation_events() throws IOException {
        String marshal = IOUtils.resourceToString("/composition/canonical_json/rawdb_composition_observation_event.json", UTF_8);
        JsonElement converted = new LightRawJsonEncoder(marshal).encodeContentAsJson(null);
        Object object = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        assertNotNull(object);
    }

    @Test
    public void unmarshal_from_js_composition_observation_events_data() throws IOException {
        String marshal = IOUtils.resourceToString("/composition/canonical_json/rawdb_composition_observation_event_item.json", UTF_8);
        JsonElement converted = new LightRawJsonEncoder(marshal).encodeContentAsJson(null);
        Object object = new CanonicalJson().unmarshal(converted.toString(), PointEvent.class);

        assertNotNull(object);
    }

    @Test
    public void unmarshal_from_js_events_data_as_array() throws IOException {
        String marshal = IOUtils.resourceToString("/composition/canonical_json/rawdb_returning_array.json", UTF_8);
        JsonElement converted = new LightRawJsonEncoder(marshal).encodeContentAsJson(null);
        Object object = new CanonicalJson().unmarshal(converted.toString(), PointEvent.class);

        assertNotNull(object);
    }

    @Test
    public void unmarshal_from_js_history() throws IOException {
        String marshal = IOUtils.resourceToString("/composition/canonical_json/rawdb_composition_history.json", UTF_8);
        JsonElement converted = new LightRawJsonEncoder(marshal).encodeContentAsJson(null);
        Object object = new CanonicalJson().unmarshal(converted.toString(), History.class);

        assertNotNull(object);
    }

    @Test
    public void compositionEncodingCycleWithDuplicateSections() throws Exception {
        Composition composition = new CanonicalXML().unmarshal(IOUtils.toString(CompositionTestDataCanonicalXML.REGISTRO_DE_ATENDIMENTO.getStream(), UTF_8),Composition.class);

        assertNotNull(composition);

        int sectionOccurrences = composition.getContent().size();

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        //see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted,Composition.class);

        assertNotNull(object);

        //check if encoded/decode carry the same name
        assertEquals(sectionOccurrences, object.getContent().size());

        String interpreted = new CanonicalXML().marshal(object);

        assertNotNull(interpreted);
    }

    @Test
    public void compositionEncodingNoContentXML() throws Exception {
        Composition composition = new CanonicalXML().unmarshal(IOUtils.toString(CompositionTestDataCanonicalXML.ALL_TYPES_NO_CONTENT.getStream(), UTF_8),Composition.class);

        assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        assertNotNull(converted);

        //see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted,Composition.class);

        assertNotNull(object);

        String interpreted = new CanonicalXML().marshal(object);

        assertNotNull(interpreted);
    }

    @Test
    public void compositionEncodingNoContentJSON() throws Exception {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.LABORATORY_REPORT_NO_CONTENT.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);

        assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        assertNotNull(converted);

        //see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted,Composition.class);

        assertNotNull(object);

        String interpreted = new CanonicalXML().marshal(object);

        assertNotNull(interpreted);
    }

    @Test
    public void testDBDecodeDvIntervalCompositeClass() throws Exception {

        String db_encoded = IOUtils.resourceToString("/composition/canonical_json/composition_with_dvinterval_composite.json", UTF_8);
        assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson(null);

        //see if this can be interpreted by Archie
        Composition composition = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        //hack the composition to figure out what is the format of DvInterval...
        ((DvInterval)((Element)((AdminEntry)composition.getContent().get(0)).getData().getItems().get(0)).getValue()).setLower(new DvDateTime("2019-11-22T00:00+01:00"));
        ((DvInterval)((Element)((AdminEntry)composition.getContent().get(0)).getData().getItems().get(0)).getValue()).setUpper(new DvDateTime("2019-12-22T00:00+01:00"));

        assertNotNull(composition);

        String toJson  = new CanonicalJson().marshal(composition);

        String interpreted = new CanonicalXML().marshal(composition);

        assertNotNull(interpreted);
    }

    @Test
    public void testDvIntervalRoundTrip() throws IOException {
        Composition composition = new CanonicalJson().unmarshal(IOUtils.resourceToString("/composition/canonical_json/simple_composition_dvinterval.json", UTF_8),Composition.class);

        assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson("composition");

        //see if this can be interpreted by Archie
        Composition composition2 = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        assertNotNull(composition2);
    }

    @Test
    public void decodeOtherDetailsWithArchetypeNodeIdAndName(){
        String dbEncoded = "{\"/name\": [{\"value\": \"family group\"}], \"/$CLASS$\": [\"ItemTree\"], \"/items[at0001]\": [{\"/name\": [{\"value\": \"family group id\"}], \"/value\": {\"id\": \"55175056\", \"type\": \"FAMILY_GROUP_ID\", \"issuer\": \"MoH\", \"assigner\": \"MoH\"}, \"/$PATH$\": \"/items[openEHR-EHR-ITEM_TREE.fake.v1 and name/value='family group']/items[at0001]\", \"/$CLASS$\": \"DvIdentifier\"}], \"/archetype_node_id\": [\"openEHR-EHR-ITEM_TREE.fake.v1\"]}";

        ItemStructure converted = new RawJson().unmarshal(dbEncoded, ItemStructure.class);

        assertNotNull(converted);
    }

    @Test
    public void decodeOtherDetailsFailing(){
        String dbEncoded = "{\n" +
                "    \"/name\": {\n" +
                "        \"value\": \"family group\"\n" +
                "    },\n" +
                "    \"/$CLASS$\": \"ItemTree\",\n" +
                "    \"/archetype_node_id\": \"openEHR-EHR-ITEM_TREE.fake.v1\",\n" +
                "    \"/items[openEHR-EHR-ITEM_TREE.fake.v1 and name/value='family group']\": {\n" +
                "        \"/name\": [\n" +
                "            {\n" +
                "                \"value\": \"family group id\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"/value\": {\n" +
                "            \"id\": \"55175056\",\n" +
                "            \"type\": \"FAMILY_GROUP_ID\",\n" +
                "            \"issuer\": \"MoH\",\n" +
                "            \"assigner\": \"MoH\"\n" +
                "        },\n" +
                "        \"/$PATH$\": \"/items[openEHR-EHR-ITEM_TREE.fake.v1 and name/value='family group']/items[at0001]\",\n" +
                "        \"/$CLASS$\": \"DvIdentifier\"\n" +
                "    }\n" +
                "}";

        ItemStructure converted = new RawJson().unmarshal(dbEncoded, ItemStructure.class);

        assertNotNull(converted);
    }
}
