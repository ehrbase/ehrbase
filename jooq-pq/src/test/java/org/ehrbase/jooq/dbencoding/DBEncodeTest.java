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
import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import com.nedap.archie.rm.archetyped.FeederAudit;
import com.nedap.archie.rm.composition.AdminEntry;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.composition.Instruction;
import com.nedap.archie.rm.composition.Section;
import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rm.datastructures.History;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datastructures.ItemTree;
import com.nedap.archie.rm.datastructures.PointEvent;
import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.datavalues.quantity.DvInterval;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.ehrbase.jooq.dbencoding.attributes.FeederAuditAttributes;
import org.ehrbase.jooq.dbencoding.rawjson.LightRawJsonEncoder;
import org.ehrbase.jooq.dbencoding.rmobject.FeederAuditEncoding;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.serialisation.xmlencoding.CanonicalXML;
import org.ehrbase.openehr.sdk.test_data.composition.CompositionTestDataCanonicalJson;
import org.ehrbase.openehr.sdk.test_data.composition.CompositionTestDataCanonicalXML;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        for (CompositionTestDataCanonicalXML compositionTestDataCanonicalXML : canonicals) {
            Composition composition = new CanonicalXML()
                    .unmarshal(IOUtils.toString(compositionTestDataCanonicalXML.getStream(), UTF_8), Composition.class);

            Assertions.assertNotNull(composition);

            CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

            String db_encoded = compositionSerializerRawJson.dbEncode(composition);
            Assertions.assertNotNull(db_encoded);

            String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

            // see if this can be interpreted by Archie
            Composition object = new CanonicalJson().unmarshal(converted, Composition.class);

            Assertions.assertNotNull(object);

            // check if encoded/decode carry the same name
            assertThat(composition.getName().getValue())
                    .isEqualTo(object.getName().getValue());

            String interpreted = new CanonicalXML().marshal(object);

            Assertions.assertNotNull(interpreted);
        }
    }

    @Test
    public void testDBDecodeFullComposition() throws Exception {

        String db_encoded = IOUtils.resourceToString("/composition/canonical_json/full_composition.json", UTF_8);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson(null);

        // see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(object);

        String interpreted = new CanonicalXML().marshal(object);

        Assertions.assertNotNull(interpreted);
    }

    @Test
    public void unmarshal_from_js_composition() throws IOException {
        String marshal = IOUtils.resourceToString("/composition/canonical_json/rawdb_composition.json", UTF_8);
        JsonElement converted = new LightRawJsonEncoder(marshal).encodeContentAsJson(null);
        Object object = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(object);
    }

    @Test
    public void unmarshal_from_js_composition_observation_events() throws IOException {
        String marshal =
                IOUtils.resourceToString("/composition/canonical_json/rawdb_composition_observation_event.json", UTF_8);
        JsonElement converted = new LightRawJsonEncoder(marshal).encodeContentAsJson(null);
        Object object = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(object);
    }

    @Test
    public void unmarshal_from_js_composition_observation_events_data() throws IOException {
        String marshal = IOUtils.resourceToString(
                "/composition/canonical_json/rawdb_composition_observation_event_item.json", UTF_8);
        JsonElement converted = new LightRawJsonEncoder(marshal).encodeContentAsJson(null);
        Object object = new CanonicalJson().unmarshal(converted.toString(), PointEvent.class);

        Assertions.assertNotNull(object);
    }

    @Test
    public void unmarshal_from_js_events_data_as_array() throws IOException {
        String marshal = IOUtils.resourceToString("/composition/canonical_json/rawdb_returning_array.json", UTF_8);
        JsonElement converted = new LightRawJsonEncoder(marshal).encodeContentAsJson(null);
        Object object = new CanonicalJson().unmarshal(converted.toString(), PointEvent.class);

        Assertions.assertNotNull(object);
    }

    @Test
    public void unmarshal_from_js_history() throws IOException {
        String marshal = IOUtils.resourceToString("/composition/canonical_json/rawdb_composition_history.json", UTF_8);
        JsonElement converted = new LightRawJsonEncoder(marshal).encodeContentAsJson(null);
        Object object = new CanonicalJson().unmarshal(converted.toString(), History.class);

        Assertions.assertNotNull(object);
    }

    @Test
    public void compositionEncodingCycleWithDuplicateSections() throws Exception {
        Composition composition = new CanonicalXML()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalXML.REGISTRO_DE_ATENDIMENTO.getStream(), UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        int sectionOccurrences = composition.getContent().size();

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        // see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted, Composition.class);

        Assertions.assertNotNull(object);

        // check if encoded/decode carry the same name
        Assertions.assertEquals(sectionOccurrences, object.getContent().size());

        String interpreted = new CanonicalXML().marshal(object);

        Assertions.assertNotNull(interpreted);
    }

    @Test
    public void compositionEncodingNoContentXML() throws Exception {
        Composition composition = new CanonicalXML()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalXML.ALL_TYPES_NO_CONTENT.getStream(), UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        Assertions.assertNotNull(converted);

        // see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted, Composition.class);

        Assertions.assertNotNull(object);

        String interpreted = new CanonicalXML().marshal(object);

        Assertions.assertNotNull(interpreted);
    }

    @Test
    public void compositionEncodingNoContentJSON() throws Exception {
        String value =
                IOUtils.toString(CompositionTestDataCanonicalJson.LABORATORY_REPORT_NO_CONTENT.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        Assertions.assertNotNull(converted);

        // see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted, Composition.class);

        Assertions.assertNotNull(object);

        String interpreted = new CanonicalXML().marshal(object);

        Assertions.assertNotNull(interpreted);
    }

    @Test
    public void testDBDecodeDvIntervalCompositeClass() throws Exception {

        String db_encoded = IOUtils.resourceToString(
                "/composition/canonical_json/composition_with_dvinterval_composite.json", UTF_8);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson(null);

        // see if this can be interpreted by Archie
        Composition composition = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        // hack the composition to figure out what is the format of DvInterval...
        ((DvInterval) ((Element) ((AdminEntry) composition.getContent().get(0))
                                .getData()
                                .getItems()
                                .get(0))
                        .getValue())
                .setLower(new DvDateTime("2019-11-22T00:00+01:00"));
        ((DvInterval) ((Element) ((AdminEntry) composition.getContent().get(0))
                                .getData()
                                .getItems()
                                .get(0))
                        .getValue())
                .setUpper(new DvDateTime("2019-12-22T00:00+01:00"));

        Assertions.assertNotNull(composition);

        String toJson = new CanonicalJson().marshal(composition);

        String interpreted = new CanonicalXML().marshal(composition);

        Assertions.assertNotNull(interpreted);
    }

    @Test
    public void testDvIntervalRoundTrip() throws IOException {
        Composition composition = new CanonicalJson()
                .unmarshal(
                        IOUtils.resourceToString(
                                "/composition/canonical_json/simple_composition_dvinterval.json", UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson("composition");

        // see if this can be interpreted by Archie
        Composition composition2 = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(composition2);
    }

    @Test
    public void testDateTimeEncodeDecode() throws IOException {
        Composition composition = new CanonicalJson()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalJson.DATE_TIME_TESTS.getStream(), UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson("composition");

        // see if this can be interpreted by Archie
        Composition composition2 = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(composition2);

        String dvtestPrefix =
                "/content[openEHR-EHR-OBSERVATION.test_all_types.v1]/data[at0001]/events[at0002]/data[at0003]";

        Assertions.assertEquals(
                "2019-01-28",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0010.1]/value/value")
                        .get(0)
                        .toString());
        Assertions.assertEquals(
                "2019-01-28T10:00",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0010.2]/value/value")
                        .get(0)
                        .toString());
        Assertions.assertEquals(
                "2019-01-28T10:00+07:00",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0010.21]/value/value")
                        .get(0)
                        .toString());
        Assertions.assertEquals(
                "2019-01",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0010.3]/value/value")
                        .get(0)
                        .toString());
        Assertions.assertEquals(
                "2019",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0010.4]/value/value")
                        .get(0)
                        .toString());
        Assertions.assertEquals(
                "2019-01-28T21:22:49.427+07:00",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0011]/value/value")
                        .get(0)
                        .toString());
        Assertions.assertEquals(
                "18:36:49",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0012.1]/value/value")
                        .get(0)
                        .toString());
        Assertions.assertEquals(
                "18:36",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0012.2]/value/value")
                        .get(0)
                        .toString());
        Assertions.assertEquals(
                "18:00",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0012.3]/value/value")
                        .get(0)
                        .toString());
        Assertions.assertEquals(
                "18:36+07:00",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0012.4]/value/value")
                        .get(0)
                        .toString());
    }

    @Test
    public void testDurationEncodeDecode() throws IOException {
        Composition composition = new CanonicalJson()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalJson.DURATION_TESTS.getStream(), UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson("composition");

        // see if this can be interpreted by Archie
        Composition composition2 = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(composition2);

        String dvtestPrefix =
                "/content[openEHR-EHR-OBSERVATION.test_all_types.v1]/data[at0001]/events[at0002]/data[at0003]";

        Assertions.assertEquals(
                "P12DT23H51M59S",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0010.1]/value/value")
                        .get(0)
                        .toString());
        Assertions.assertEquals(
                "P10Y1M12DT23H51M59S",
                composition2
                        .itemsAtPath(dvtestPrefix + "/items[at0010.2]/value/value")
                        .get(0)
                        .toString());
        // not yet working as of 12.10.20
        //        assertEquals("-P10Y10DT12H20S",
        // composition2.itemsAtPath(dvtestPrefix+"/items[at0010.3]/value/value").get(0).toString());

    }

    @Test
    public void testNestedLanguageSubjectPartyIdentified() throws IOException {
        Composition composition = new CanonicalJson()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalJson.SUBJECT_PARTY_IDENTIFIED.getStream(), UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson("composition");

        // see if this can be interpreted by Archie
        Composition composition2 = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(composition2);

        Assertions.assertEquals(
                "1",
                composition2
                        .itemsAtPath(
                                "/content[openEHR-EHR-SECTION.allgemeine_angaben.v1]/items[openEHR-EHR-EVALUATION.problem_diagnosis_covid.v1]/subject/external_ref/id/value")
                        .get(0)
                        .toString());
    }

    @Test
    public void testNestedLanguageSubjectPartySelf() throws IOException {
        Composition composition = new CanonicalJson()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalJson.SUBJECT_PARTY_SELF.getStream(), UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson("composition");

        // see if this can be interpreted by Archie
        Composition composition2 = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(composition2);

        Assertions.assertEquals(
                "PartySelf",
                composition2
                        .itemsAtPath(
                                "/content[openEHR-EHR-SECTION.allgemeine_angaben.v1]/items[openEHR-EHR-EVALUATION.problem_diagnosis_covid.v1]/subject")
                        .get(0)
                        .getClass()
                        .getSimpleName());
    }

    @Test
    public void testNestedLanguage() throws IOException {
        Composition composition = new CanonicalJson()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalJson.SUBJECT_PARTY_SELF.getStream(), UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson("composition");

        // see if this can be interpreted by Archie
        Composition composition2 = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(composition2);

        Assertions.assertEquals(
                "de",
                composition2
                        .itemsAtPath(
                                "/content[openEHR-EHR-SECTION.allgemeine_angaben.v1]/items[openEHR-EHR-EVALUATION.problem_diagnosis_covid.v1]/language/code_string")
                        .get(0)
                        .toString());
    }

    @Test
    public void testNestedLanguageSubjectPartyRelated() throws IOException {
        Composition composition = new CanonicalJson()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalJson.SUBJECT_PARTY_RELATED.getStream(), UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson("composition");

        // see if this can be interpreted by Archie
        Composition composition2 = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(composition2);

        Assertions.assertEquals(
                "someone",
                composition2
                        .itemsAtPath(
                                "/content[openEHR-EHR-SECTION.allgemeine_angaben.v1]/items[openEHR-EHR-EVALUATION.problem_diagnosis_covid.v1]/subject/relationship/value")
                        .get(0)
                        .toString());
    }

    @Test
    public void testNestedProvider() throws IOException {
        Composition composition = new CanonicalJson()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalJson.NESTED_PROVIDER.getStream(), UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson("composition");

        // see if this can be interpreted by Archie
        Composition composition2 = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(composition2);

        Assertions.assertEquals(
                "zzzz",
                composition2
                        .itemsAtPath(
                                "/content[openEHR-EHR-SECTION.allgemeine_angaben.v1]/items[openEHR-EHR-EVALUATION.problem_diagnosis_covid.v1]/provider/external_ref/id/value")
                        .get(0)
                        .toString());
    }

    @Test
    public void testNestedEncoding() throws IOException {
        Composition composition = new CanonicalJson()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalJson.SUBJECT_PARTY_SELF.getStream(), UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson("composition");

        // see if this can be interpreted by Archie
        Composition composition2 = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(composition2);

        Assertions.assertEquals(
                "UTF-8",
                composition2
                        .itemsAtPath(
                                "/content[openEHR-EHR-SECTION.allgemeine_angaben.v1]/items[openEHR-EHR-EVALUATION.problem_diagnosis_covid.v1]/encoding/code_string")
                        .get(0)
                        .toString());
    }

    @Test
    public void decodeOtherDetailsWithArchetypeNodeIdAndName() {
        String dbEncoded =
                "{\"/name\": [{\"value\": \"family group\"}], \"/$CLASS$\": [\"ItemTree\"], \"/items[at0001]\": [{\"/name\": [{\"value\": \"family group id\"}], \"/value\": {\"id\": \"55175056\", \"type\": \"FAMILY_GROUP_ID\", \"issuer\": \"MoH\", \"assigner\": \"MoH\"}, \"/$PATH$\": \"/items[openEHR-EHR-ITEM_TREE.fake.v1 and name/value='family group']/items[at0001]\", \"/$CLASS$\": \"DvIdentifier\"}], \"/archetype_node_id\": [\"openEHR-EHR-ITEM_TREE.fake.v1\"]}";

        ItemStructure converted = new RawJson().unmarshal(dbEncoded, ItemStructure.class);

        Assertions.assertNotNull(converted);
    }

    @Test
    public void decodeOtherDetailsFailing() {
        String dbEncoded = "{\n" + "    \"/name\": {\n"
                + "        \"value\": \"family group\"\n"
                + "    },\n"
                + "    \"/$CLASS$\": \"ItemTree\",\n"
                + "    \"/archetype_node_id\": \"openEHR-EHR-ITEM_TREE.fake.v1\",\n"
                + "    \"/items[openEHR-EHR-ITEM_TREE.fake.v1 and name/value='family group']\": {\n"
                + "        \"/name\": [\n"
                + "            {\n"
                + "                \"value\": \"family group id\"\n"
                + "            }\n"
                + "        ],\n"
                + "        \"/value\": {\n"
                + "            \"id\": \"55175056\",\n"
                + "            \"type\": \"FAMILY_GROUP_ID\",\n"
                + "            \"issuer\": \"MoH\",\n"
                + "            \"assigner\": \"MoH\"\n"
                + "        },\n"
                + "        \"/$PATH$\": \"/items[openEHR-EHR-ITEM_TREE.fake.v1 and name/value='family group']/items[at0001]\",\n"
                + "        \"/$CLASS$\": \"DvIdentifier\"\n"
                + "    }\n"
                + "}";

        ItemStructure converted = new RawJson().unmarshal(dbEncoded, ItemStructure.class);

        Assertions.assertNotNull(converted);
    }

    @Test
    public void compositionEncodingNamedItemTree() throws Exception {
        String value =
                IOUtils.toString(CompositionTestDataCanonicalJson.MINIMAL_EVAL_NAMED_ITEM_TREE.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        // check that ITEM_TREE name is serialized
        Assertions.assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        Assertions.assertNotNull(converted);

        // see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted, Composition.class);

        Assertions.assertEquals(
                "1234",
                object.itemsAtPath(
                                "/content[openEHR-EHR-EVALUATION.minimal.v1]/data[at0001]/name/defining_code/code_string")
                        .get(0)
                        .toString());

        Assertions.assertNotNull(object);

        String interpreted = new CanonicalXML().marshal(object);

        Assertions.assertNotNull(interpreted);
    }

    @Test
    public void compositionFeederAudit() throws Exception {
        String value = IOUtils.toString(CompositionTestDataCanonicalXML.FEEDER_AUDIT.getStream(), UTF_8);
        CanonicalXML cut = new CanonicalXML();
        Composition composition = cut.unmarshal(value, Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        // check that ITEM_TREE name is serialized
        Assertions.assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        Assertions.assertNotNull(converted);

        // see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted, Composition.class);

        // check the feeder_audit values in evaluation
        Assertions.assertEquals(
                "EMIS",
                object.itemsAtPath(
                                "/content[openEHR-EHR-SECTION.allergies_adverse_reactions_rcp.v1]/items[openEHR-EHR-EVALUATION.adverse_reaction_risk.v1]/feeder_audit/originating_system_audit/system_id")
                        .get(0)
                        .toString());

        Assertions.assertNotNull(object);

        String interpreted = new CanonicalXML().marshal(object);

        Assertions.assertNotNull(interpreted);
    }

    @Test
    public void testEncodeDecodeFeederAudit() {
        String jsonFeederAudit = "{\n" + "              \"originating_system_item_ids\": [\n"
                + "                {\n"
                + "                  \"issuer\": \"EMIS\",\n"
                + "                  \"assigner\": \"EMIS\",\n"
                + "                  \"id\": \"123456\",\n"
                + "                  \"type\": \"FHIR\"\n"
                + "                }\n"
                + "              ],\n"
                + "              \"feeder_system_item_ids\": [],\n"
                + "              \"originating_system_audit\": {\n"
                + "                \"system_id\": \"EMIS\",\n"
                + "                \"time\": {\n"
                + "                  \"value\": \"2016-12-20T00:11:02.518+02:00\",\n"
                + "                  \"epoch_offset\": 1.482185462E9\n"
                + "                }\n"
                + "              }\n"
                + "            }";

        CanonicalJson cut = new CanonicalJson();
        FeederAudit feederAudit = cut.unmarshal(jsonFeederAudit, FeederAudit.class);

        String encodedToDB = new FeederAuditEncoding().toDB(feederAudit);

        FeederAudit decodedFromDB = new FeederAuditEncoding().fromDB(encodedToDB);

        Assertions.assertEquals(
                feederAudit.getOriginatingSystemAudit().getSystemId(),
                decodedFromDB.getOriginatingSystemAudit().getSystemId());
    }

    @Test
    public void testEncodeDecodeFeederAuditWithOriginalContent() {
        String jsonFeederAudit = " {\n" + "    \"_type\" : \"FEEDER_AUDIT\",\n"
                + "    \"original_content\" : {\n"
                + "      \"_type\" : \"DV_PARSABLE\",\n"
                + "      \"value\" : \"{\\\"resourceType\\\":\\\"Observation\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"https://www.medizininformatik-initiative.de/fhir/core/StructureDefinition/ObservationLab\\\"]},\\\"identifier\\\":[{\\\"type\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://terminology.hl7.org/CodeSystem/v2-0203\\\",\\\"code\\\":\\\"OBI\\\"}]},\\\"system\\\":\\\"https://diz.mii.de/fhir/core/NamingSystem/test-lab-results\\\",\\\"value\\\":\\\"59826-8_1234567890\\\",\\\"assigner\\\":{\\\"identifier\\\":{\\\"system\\\":\\\"https://www.medizininformatik-initiative.de/fhir/core/NamingSystem/org-identifier\\\",\\\"value\\\":\\\"DIZ-ID\\\"}}}],\\\"status\\\":\\\"final\\\",\\\"category\\\":[{\\\"coding\\\":[{\\\"system\\\":\\\"http://loinc.org\\\",\\\"code\\\":\\\"26436-6\\\"},{\\\"system\\\":\\\"http://terminology.hl7.org/CodeSystem/observation-category\\\",\\\"code\\\":\\\"laboratory\\\"}]}],\\\"code\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://loinc.org\\\",\\\"code\\\":\\\"59826-8\\\",\\\"display\\\":\\\"Creatinine [Moles/volume] in Blood\\\"}],\\\"text\\\":\\\"Kreatinin\\\"},\\\"subject\\\":{\\\"reference\\\":\\\"Patient/679e3fc3-cc9a-4e04-bc81-cb4d4a7e8e1c\\\"},\\\"encounter\\\":{\\\"reference\\\":\\\"Encounter/555\\\"},\\\"effectiveDateTime\\\":\\\"2018-11-20T12:05:00+01:00\\\",\\\"issued\\\":\\\"2018-03-11T10:28:00+01:00\\\",\\\"performer\\\":[{\\\"reference\\\":\\\"Organization/7772\\\",\\\"display\\\":\\\"Zentrallabor des IKCL\\\"}],\\\"valueQuantity\\\":{\\\"value\\\":72,\\\"unit\\\":\\\"..mol/l\\\",\\\"system\\\":\\\"http://unitsofmeasure.org\\\",\\\"code\\\":\\\"umol/L\\\"},\\\"interpretation\\\":[{\\\"coding\\\":[{\\\"system\\\":\\\"http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation\\\",\\\"code\\\":\\\"N\\\"}]}],\\\"referenceRange\\\":[{\\\"low\\\":{\\\"value\\\":72},\\\"high\\\":{\\\"value\\\":127},\\\"type\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://terminology.hl7.org/CodeSystem/referencerange-meaning\\\",\\\"code\\\":\\\"normal\\\",\\\"display\\\":\\\"Normal Range\\\"}]}}]}\",\n"
                + "      \"formalism\" : \"application/json\"\n"
                + "    },\n"
                + "    \"originating_system_audit\" : {\n"
                + "      \"_type\" : \"FEEDER_AUDIT_DETAILS\",\n"
                + "      \"system_id\" : \"FHIR-bridge\"\n"
                + "    }\n"
                + "  }";

        CanonicalJson cut = new CanonicalJson();
        FeederAudit feederAudit = cut.unmarshal(jsonFeederAudit, FeederAudit.class);

        String encodedToDB = new FeederAuditEncoding().toDB(feederAudit);

        FeederAudit decodedFromDB = new FeederAuditEncoding().fromDB(encodedToDB);

        Assertions.assertEquals(
                feederAudit.getOriginatingSystemAudit().getSystemId(),
                decodedFromDB.getOriginatingSystemAudit().getSystemId());
    }

    @Test
    public void testEncodeTimeAsJson() {
        String fromDB =
                "{\"/value\": {\"value\": \"2020-04-02T12:00Z\", \"epoch_offset\": 1585828800}, \"/$CLASS$\": \"DvDateTime\"}";

        JsonElement converted = new LightRawJsonEncoder(fromDB).encodeContentAsJson("value");

        Assertions.assertNotNull(converted);
    }

    @Test
    public void testEncodeDvTextAsJson() {
        String fromDB = "{\n" + "                      \"/$CLASS$\": \"DvText\",\n"
                + "                      \"/$PATH$\": \"/items[at0041]/data[at0003]/events[at0002 and name/value\\u003d\\u0027Point in time\\u0027]/data[at0001]/content[openEHR-EHR-OBSERVATION.yhscn_diadem_assessment.v0 and name/value\\u003d\\u0027YHSCN - DiADeM assessment\\u0027]\",\n"
                + "                      \"/name\": [\n"
                + "                        {\n"
                + "                          \"value\": \"Blood test recommendation\"\n"
                + "                        }\n"
                + "                      ],\n"
                + "                      \"/value\": {\n"
                + "                        \"value\": \"Consider Referral for Blood Test\",\n"
                + "                        \"_type\": \"DV_TEXT\"\n"
                + "                      }\n"
                + "                    }";

        JsonElement converted = new LightRawJsonEncoder(fromDB).encodeContentAsJson("value");

        assertThat(converted.getAsJsonObject().get("_type").getAsString()).isEqualTo("DV_TEXT");

        fromDB = "{\n" + "                      \"/$CLASS$\": \"DvCodedText\",\n"
                + "                      \"/$PATH$\": \"/items[at0009]/data[at0003]/events[at0002 and name/value\\u003d\\u0027Point in time\\u0027]/data[at0001]/content[openEHR-EHR-OBSERVATION.yhscn_diadem_assessment.v0 and name/value\\u003d\\u0027YHSCN - DiADeM assessment\\u0027]\",\n"
                + "                      \"/name\": [\n"
                + "                        {\n"
                + "                          \"value\": \"Exclusion criteria\"\n"
                + "                        }\n"
                + "                      ],\n"
                + "                      \"/value\": {\n"
                + "                        \"value\": \"True\",\n"
                + "                        \"_type\": \"DV_CODED_TEXT\",\n"
                + "                        \"definingCode\": {\n"
                + "                          \"codeString\": \"at0014\",\n"
                + "                          \"terminologyId\": {\n"
                + "                            \"name\": \"local\",\n"
                + "                            \"value\": \"local\",\n"
                + "                            \"_type\": \"TERMINOLOGY_ID\"\n"
                + "                          },\n"
                + "                          \"_type\": \"CODE_PHRASE\"\n"
                + "                        }\n"
                + "                      }\n"
                + "                    }";

        converted = new LightRawJsonEncoder(fromDB).encodeContentAsJson("value");

        assertThat(converted.getAsJsonObject().get("_type").getAsString()).isEqualTo("DV_CODED_TEXT");
    }

    @Test
    public void testDBDecodeIssue350() throws Exception {

        String db_encoded =
                new String(Files.readAllBytes(Paths.get("src/test/resources/sample_data/bug350_missing_data.json")));
        Assertions.assertNotNull(db_encoded);

        // see if this can be interpreted by Archie
        Composition object = new RawJson().unmarshal(db_encoded, Composition.class);

        Assertions.assertEquals(
                8,
                ((Section) object.itemsAtPath("/content[openEHR-EHR-SECTION.respect_headings.v0]")
                                .get(0))
                        .getItems()
                        .size());

        String interpreted = new CanonicalXML().marshal(object);

        Assertions.assertNotNull(interpreted);
    }

    @Test
    public void compositionEncodingArchetypeDetails() throws Exception {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.DEMO_VITALS.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        // check that ITEM_TREE name is serialized
        Assertions.assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        Assertions.assertNotNull(converted);

        // see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted, Composition.class);

        Assertions.assertTrue(object.itemsAtPath(
                                "/content[openEHR-EHR-SECTION.ispek_dialog.v1]/items[openEHR-EHR-OBSERVATION.body_temperature-zn.v1]/archetype_details/archetype_id")
                        .size()
                > 0);

        Assertions.assertEquals(
                "openEHR-EHR-OBSERVATION.body_temperature-zn.v1",
                object.itemsAtPath(
                                "/content[openEHR-EHR-SECTION.ispek_dialog.v1]/items[openEHR-EHR-OBSERVATION.body_temperature-zn.v1]/archetype_details/archetype_id")
                        .get(0)
                        .toString());

        Assertions.assertNotNull(object);
    }

    @Test
    public void compositionEncodingFeederAuditDetails() throws Exception {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.FEEDER_AUDIT_DETAILS.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);

        // check compo
        Assertions.assertNotNull(composition);
        Assertions.assertNotNull(composition.getFeederAudit().getFeederSystemAudit());
        // other details
        Assertions.assertNotNull(
                composition.getFeederAudit().getFeederSystemAudit().getOtherDetails());
        Assertions.assertEquals(
                "family group",
                composition
                        .getFeederAudit()
                        .getFeederSystemAudit()
                        .getOtherDetails()
                        .getName()
                        .getValue());
        Assertions.assertTrue(
                composition.getFeederAudit().getFeederSystemAudit().getOtherDetails() instanceof ItemTree);
        Assertions.assertEquals(
                1,
                composition
                        .getFeederAudit()
                        .getFeederSystemAudit()
                        .getOtherDetails()
                        .getItems()
                        .size());
        Assertions.assertTrue(
                composition
                                .getFeederAudit()
                                .getFeederSystemAudit()
                                .getOtherDetails()
                                .getItems()
                                .get(0)
                        instanceof Element);
        Assertions.assertTrue(
                ((Element) composition
                                        .getFeederAudit()
                                        .getFeederSystemAudit()
                                        .getOtherDetails()
                                        .getItems()
                                        .get(0))
                                .getValue()
                        instanceof DvIdentifier);
        // version id
        Assertions.assertNotNull(
                composition.getFeederAudit().getFeederSystemAudit().getVersionId());
        Assertions.assertEquals(
                "final", composition.getFeederAudit().getFeederSystemAudit().getVersionId());

        // DB encode other details
        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();
        String dbEncoded = compositionSerializerRawJson.dbEncode(
                composition.getFeederAudit().getFeederSystemAudit().getOtherDetails());
        Assertions.assertNotNull(dbEncoded);

        // Convert encoded string into map to write to DB
        Map<String, Object> asMap = new LightRawJsonEncoder(dbEncoded).encodeOtherDetailsAsMap();
        Assertions.assertNotNull(asMap);
        Assertions.assertEquals(4, asMap.size());
        Assertions.assertNotNull(asMap.get("/items[at0001]"));

        // Attribute mapping and correct archetype node id path in naming
        Map<String, Object> map = new FeederAuditAttributes(composition.getFeederAudit()).toMap();
        Assertions.assertNotNull(map);
        Assertions.assertNotNull(map.get("feeder_system_audit"));
        Map<String, Object> feederMap = (Map) map.get("feeder_system_audit");
        Assertions.assertNotNull(feederMap);
        Assertions.assertNotNull(feederMap.get("other_details[openEHR-EHR-ITEM_TREE.generic.v1]"));
        Assertions.assertEquals(
                4, ((Map<String, Object>) feederMap.get("other_details[openEHR-EHR-ITEM_TREE.generic.v1]")).size());
    }

    @Test
    public void compositionEncodingEmptyProtocol() throws Exception {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.GECCO_PERSONENDATEN.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        Assertions.assertNotNull(converted);

        // see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted, Composition.class);

        Assertions.assertNotNull(object);

        String interpreted = new CanonicalXML().marshal(object);

        Assertions.assertNotNull(interpreted);
    }

    @Test
    public void testDBEncodeDecodeInstruction() throws IOException {
        Composition composition = new CanonicalJson()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalJson.NESTED_EN_V1.getStream(), UTF_8),
                        Composition.class);

        CompositionSerializer compositionSerializer = new CompositionSerializer();
        String encoded = compositionSerializer.dbEncode(composition);

        Assertions.assertTrue(encoded.contains(CompositionSerializer.TAG_EXPIRY_TIME));

        String json = new LightRawJsonEncoder(encoded).encodeCompositionAsString();
        Composition result = new CanonicalJson().unmarshal(json, Composition.class);
        DvDateTime expiryTime =
                ((Instruction) ((Section) result.getContent().get(0)).getItems().get(0)).getExpiryTime();

        Assertions.assertNotNull(expiryTime);
        Assertions.assertEquals(OffsetDateTime.parse("2021-05-18T13:13:09.780+03:00"), expiryTime.getValue());
    }

    @Test
    public void testOtherParticipationsPartyRef() throws IOException {
        Composition composition = new CanonicalJson()
                .unmarshal(
                        IOUtils.toString(CompositionTestDataCanonicalJson.OTHER_PARTICIPATIONS.getStream(), UTF_8),
                        Composition.class);

        Assertions.assertNotNull(composition);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson("composition");

        // see if this can be interpreted by Archie
        Composition composition2 = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(composition2);

        Assertions.assertEquals(
                "PERSON",
                composition2
                        .itemsAtPath(
                                "/content[openEHR-EHR-ACTION.minimal.v1]/other_participations/performer/external_ref/type")
                        .get(0));
    }
}
