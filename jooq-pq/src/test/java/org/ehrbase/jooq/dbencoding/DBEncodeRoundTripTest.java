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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nedap.archie.rm.composition.Composition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.ehrbase.jooq.dbencoding.rawjson.LightRawJsonEncoder;
import org.ehrbase.jooq.dbencoding.templateprovider.TestDataTemplateProvider;
import org.ehrbase.openehr.sdk.serialisation.RMDataFormat;
import org.ehrbase.openehr.sdk.serialisation.flatencoding.FlatFormat;
import org.ehrbase.openehr.sdk.serialisation.flatencoding.FlatJasonProvider;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.ArchieObjectMapperProvider;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.test_data.composition.CompositionTestDataSimSDTJson;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DBEncodeRoundTripTest {

    class TestCase {
        long id;
        CompositionTestDataSimSDTJson simSDTJson;
        String templateId;
        String[] missing;
        String[] extra;

        public TestCase(
                long id,
                CompositionTestDataSimSDTJson simSDTJson,
                String templateId,
                String[] missing,
                String[] extra) {
            this.id = id;
            this.simSDTJson = simSDTJson;
            this.templateId = templateId;
            this.missing = missing;
            this.extra = extra;
        }
    }

    @Test
    public void testRoundTrip() throws IOException {

        List<TestCase> testCaseList = new ArrayList<>();

        testCaseList.add(new TestCase(
                1,
                CompositionTestDataSimSDTJson.ALL_TYPES,
                "test_all_types.en.v1",
                new String[] {
                    "Missing path: test_all_types/test_all_types:0/identifier|id, value: 55175056",
                    "Missing path: test_all_types/test_all_types:0/proportion_any|type, value: 1"
                },
                new String[] {
                    "Extra path: test_all_types/_uid, value: bb015b91-68c0-460f-a458-8ef06869b53c::ehrbase.org::1",
                    "Extra path: test_all_types/language|code, value: en",
                    "Extra path: test_all_types/language|terminology, value: ISO_639-1",
                    "Extra path: test_all_types/territory|code, value: UY",
                    "Extra path: test_all_types/territory|terminology, value: ISO_3166-1",
                    "Extra path: test_all_types/context/start_time, value: 2019-01-14T18:36:49.294Z",
                    "Extra path: test_all_types/context/setting|code, value: 229",
                    "Extra path: test_all_types/context/setting|value, value: primary nursing care",
                    "Extra path: test_all_types/context/setting|terminology, value: openehr",
                    "Extra path: test_all_types/test_all_types:0/identifier, value: 55175056",
                    "Extra path: test_all_types/test_all_types:0/proportion_any|type, value: 1.0",
                    // this element is held into a ITEM_SINGLE which is not supported
                    "Extra path: test_all_types/test_all_types3:0/section_2/test_all_types:0/count_3, value: 123",
                    "Extra path: test_all_types/category|code, value: 433",
                    "Extra path: test_all_types/category|value, value: event",
                    "Extra path: test_all_types/category|terminology, value: openehr",
                    "Extra path: test_all_types/composer|id, value: b7c07d35-fa06-4280-8e65-eabdfbe64fdc",
                    "Extra path: test_all_types/composer|id_namespace, value: DEMOGRAPHIC",
                    "Extra path: test_all_types/composer|id_scheme, value: test",
                    "Extra path: test_all_types/composer|name, value: Dr. House"
                }));

        testCaseList.add(
                new TestCase(2, CompositionTestDataSimSDTJson.CORONA, "Corona_Anamnese", new String[] {}, new String[] {
                    "Extra path: bericht/_uid, value: 93a018f1-ad95-4d52-bb8f-0f64d7f7cce6::ehrbase.org::1",
                    "Extra path: bericht/language|code, value: de",
                    "Extra path: bericht/language|terminology, value: ISO_639-1",
                    "Extra path: bericht/territory|code, value: DE",
                    "Extra path: bericht/territory|terminology, value: ISO_3166-1",
                    "Extra path: bericht/context/start_time, value: 2020-05-11T22:53:12.039139+02:00",
                    "Extra path: bericht/context/setting|code, value: 238",
                    "Extra path: bericht/context/setting|value, value: other care",
                    "Extra path: bericht/context/setting|terminology, value: openehr",
                    "Extra path: bericht/category|code, value: 433",
                    "Extra path: bericht/category|value, value: event",
                    "Extra path: bericht/category|terminology, value: openehr",
                    "Extra path: bericht/composer|name, value: birger.haarbrandt@plri.de"
                }));

        testCaseList.add(new TestCase(
                3,
                CompositionTestDataSimSDTJson.ALTERNATIVE_EVENTS,
                "AlternativeEvents",
                new String[] {
                    "Missing path: bericht/körpergewicht:0/birth_en/gewicht|magnitude, value: 30.0",
                    "Missing path: bericht/körpergewicht:0/any_event_en:1/gewicht|magnitude, value: 60.0",
                    "Missing path: bericht/körpergewicht:0/any_event_en:2/gewicht|magnitude, value: 61.0",
                    "Missing path: bericht/körpergewicht:0/any_event_en:0/gewicht|magnitude, value: 55.0",
                    "Missing path: bericht/körpergewicht:0/any_event_en:3/gewicht|magnitude, value: 62.0"
                },
                new String[] {
                    "Extra path: bericht/_uid, value: 655ab9fb-9454-4540-a52c-83ce4bdf765d::ehrbase.org::1",
                    "Extra path: bericht/language|code, value: en",
                    "Extra path: bericht/language|terminology, value: ISO_639-1",
                    "Extra path: bericht/territory|code, value: DE",
                    "Extra path: bericht/territory|terminology, value: ISO_3166-1",
                    "Extra path: bericht/context/start_time, value: 2010-11-02T12:00:00Z",
                    "Extra path: bericht/context/setting|code, value: 235",
                    "Extra path: bericht/context/setting|value, value: complementary health care",
                    "Extra path: bericht/context/setting|terminology, value: openehr",
                    "Extra path: bericht/körpergewicht:0/any_event_en:0/gewicht|magnitude, value: 55",
                    "Extra path: bericht/körpergewicht:0/any_event_en:1/gewicht|magnitude, value: 60",
                    "Extra path: bericht/körpergewicht:0/any_event_en:2/gewicht|magnitude, value: 61",
                    "Extra path: bericht/körpergewicht:0/any_event_en:3/gewicht|magnitude, value: 62",
                    "Extra path: bericht/körpergewicht:0/birth_en/gewicht|magnitude, value: 30",
                    "Extra path: bericht/category|code, value: 433",
                    "Extra path: bericht/category|value, value: event",
                    "Extra path: bericht/category|terminology, value: openehr",
                    "Extra path: bericht/composer|name, value: Test"
                }));

        testCaseList.add(new TestCase(
                4,
                CompositionTestDataSimSDTJson.MULTI_OCCURRENCE,
                "ehrbase_multi_occurrence.de.v1",
                new String[] {
                    "Missing path: encounter/body_temperature:0/any_event:0/temperature|magnitude, value: 22.0",
                    "Missing path: encounter/body_temperature:0/any_event:1/temperature|magnitude, value: 11.0",
                    "Missing path: encounter/body_temperature:1/any_event:0/temperature|magnitude, value: 22.0",
                    "Missing path: encounter/body_temperature:1/any_event:1/temperature|magnitude, value: 11.0"
                },
                new String[] {
                    "Extra path: encounter/_uid, value: 95705e9e-d658-4e60-8e42-240db4478179::ehrbase.org::1",
                    "Extra path: encounter/language|code, value: de",
                    "Extra path: encounter/language|terminology, value: ISO_639-1",
                    "Extra path: encounter/territory|code, value: DE",
                    "Extra path: encounter/territory|terminology, value: ISO_3166-1",
                    "Extra path: encounter/context/start_time, value: 2020-10-06T13:30:34.314872+02:00",
                    "Extra path: encounter/context/setting|code, value: 236",
                    "Extra path: encounter/context/setting|value, value: dental care",
                    "Extra path: encounter/context/setting|terminology, value: openehr",
                    "Extra path: encounter/category|code, value: 433",
                    "Extra path: encounter/category|value, value: event",
                    "Extra path: encounter/category|terminology, value: openehr",
                    "Extra path: encounter/composer|name, value: Test",
                    "Extra path: encounter/context/_end_time, value: 2020-10-06T13:30:34.317875+02:00",
                    "Extra path: encounter/body_temperature:0/any_event:0/temperature|magnitude, value: 22",
                    "Extra path: encounter/body_temperature:0/any_event:1/temperature|magnitude, value: 11",
                    "Extra path: encounter/body_temperature:1/any_event:0/temperature|magnitude, value: 22",
                    "Extra path: encounter/body_temperature:1/any_event:1/temperature|magnitude, value: 11"
                }));

        SoftAssertions softly = new SoftAssertions();

        for (TestCase testCase : testCaseList) {
            checkTestCase(testCase, softly);
        }

        softly.assertAll();
    }

    public void checkTestCase(TestCase testCase, SoftAssertions softly) throws IOException {

        String value = IOUtils.toString(testCase.simSDTJson.getStream(), UTF_8);
        RMDataFormat flatJson = new FlatJasonProvider(new TestDataTemplateProvider())
                .buildFlatJson(FlatFormat.SIM_SDT, testCase.templateId);

        Composition composition = flatJson.unmarshal(value);
        Assertions.assertThat(composition).isNotNull();

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);

        assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        assertNotNull(converted);

        Composition actual = new CanonicalJson().unmarshal(converted, Composition.class);

        String actualFlat = flatJson.marshal(actual);

        List<String> errors = compere(actualFlat, value);

        softly.assertThat(errors)
                .filteredOn(s -> s.startsWith("Missing"))
                .as("Test Case %s", testCase.id)
                .containsExactlyInAnyOrder(testCase.missing);

        String[] extra = {
            "Extra path: test_all_types/test_all_types:0/identifier, value: 55175056",
            "Extra path: test_all_types/test_all_types:0/proportion_any|type, value: 1.0"
        };
        softly.assertThat(errors)
                .filteredOn(s -> s.startsWith("Extra"))
                .as("Test Case %s", testCase.id)
                .containsExactlyInAnyOrder(testCase.extra);
    }

    // the following are subset of the above for debugging purpose, hence the ignore directive.
    @Test
    @Disabled
    public void testActivityRoundTrip() throws IOException {
        String value = new String(
                Files.readAllBytes(Paths.get("src/test/resources/sample_data/test_all_types_activity_flat.json")));
        RMDataFormat flatJson = new FlatJasonProvider(new TestDataTemplateProvider())
                .buildFlatJson(FlatFormat.SIM_SDT, "test_all_types.en.v1");

        Composition composition = flatJson.unmarshal(value);
        Assertions.assertThat(composition).isNotNull();

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);

        assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        assertNotNull(converted);

        Composition actual = new CanonicalJson().unmarshal(converted, Composition.class);

        String actualFlat = flatJson.marshal(actual);

        assertNotNull(actualFlat);
    }

    @Test
    @Disabled
    public void testIsmTransitionRoundTrip() throws IOException {
        String value = new String(Files.readAllBytes(
                Paths.get("src/test/resources/sample_data/test_all_types_ism_transition_flat.json")));
        RMDataFormat flatJson = new FlatJasonProvider(new TestDataTemplateProvider())
                .buildFlatJson(FlatFormat.SIM_SDT, "test_all_types.en.v1");

        Composition composition = flatJson.unmarshal(value);
        Assertions.assertThat(composition).isNotNull();

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);

        assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        assertNotNull(converted);

        Composition actual = new CanonicalJson().unmarshal(converted, Composition.class);

        String actualFlat = flatJson.marshal(actual);

        assertNotNull(actualFlat);
    }

    @Test
    @Disabled
    public void testCountRoundTrip() throws IOException {
        String value = new String(
                Files.readAllBytes(Paths.get("src/test/resources/sample_data/test_all_type_count_flat.json")));
        RMDataFormat flatJson = new FlatJasonProvider(new TestDataTemplateProvider())
                .buildFlatJson(FlatFormat.SIM_SDT, "test_all_types.en.v1");

        Composition composition = flatJson.unmarshal(value);
        Assertions.assertThat(composition).isNotNull();

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);

        assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        assertNotNull(converted);

        Composition actual = new CanonicalJson().unmarshal(converted, Composition.class);

        String actualFlat = flatJson.marshal(actual);

        assertNotNull(actualFlat);
    }

    @Test
    @Disabled
    public void testMathFunctionRoundTrip() throws IOException {
        String value = new String(
                Files.readAllBytes(Paths.get("src/test/resources/sample_data/corona_math_function_flat.json")));
        RMDataFormat flatJson = new FlatJasonProvider(new TestDataTemplateProvider())
                .buildFlatJson(FlatFormat.SIM_SDT, "Corona_Anamnese");

        Composition composition = flatJson.unmarshal(value);
        Assertions.assertThat(composition).isNotNull();

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);

        assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        assertNotNull(converted);

        Composition actual = new CanonicalJson().unmarshal(converted, Composition.class);

        String actualFlat = flatJson.marshal(actual);

        assertNotNull(actualFlat);
    }

    @Test
    @Disabled
    public void testAlternativeEventsRoundTrip() throws IOException {
        String value = new String(
                Files.readAllBytes(Paths.get("src/test/resources/sample_data/alternative_events_reduced_flat.json")));
        RMDataFormat flatJson = new FlatJasonProvider(new TestDataTemplateProvider())
                .buildFlatJson(FlatFormat.SIM_SDT, "AlternativeEvents");

        Composition composition = flatJson.unmarshal(value);
        Assertions.assertThat(composition).isNotNull();

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);

        assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        assertNotNull(converted);

        Composition actual = new CanonicalJson().unmarshal(converted, Composition.class);

        String actualFlat = flatJson.marshal(actual);

        assertNotNull(actualFlat);
    }

    public static List<String> compere(String actualJson, String expectedJson) throws JsonProcessingException {
        List<String> errors = new ArrayList<>();
        ObjectMapper objectMapper = ArchieObjectMapperProvider.getObjectMapper();

        Map<String, Object> actual = objectMapper.readValue(actualJson, Map.class);
        Map<String, Object> expected = objectMapper.readValue(expectedJson, Map.class);

        actual.forEach((key, value) -> {
            if (!expected.containsKey(key) || !expected.get(key).equals(value)) {
                errors.add(String.format("Missing path: %s, value: %s", key, value));
            }
        });

        expected.forEach((key, value) -> {
            if (!actual.containsKey(key) || !actual.get(key).equals(value)) {
                errors.add(String.format("Extra path: %s, value: %s", key, value));
            }
        });

        return errors;
    }
}
