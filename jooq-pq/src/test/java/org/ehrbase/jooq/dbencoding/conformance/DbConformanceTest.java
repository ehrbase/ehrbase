/*
 * Copyright (c) 2021 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.dbencoding.conformance;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nedap.archie.rm.composition.Composition;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.api.SoftAssertions;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.rawjson.LightRawJsonEncoder;
import org.ehrbase.jooq.dbencoding.templateprovider.TestDataTemplateProvider;
import org.ehrbase.openehr.sdk.serialisation.RMDataFormat;
import org.ehrbase.openehr.sdk.serialisation.flatencoding.FlatFormat;
import org.ehrbase.openehr.sdk.serialisation.flatencoding.FlatJasonProvider;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.ArchieObjectMapperProvider;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.test_data.composition.CompositionTestDataConformanceSDTJson;
import org.ehrbase.openehr.sdk.validation.CompositionValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DbConformanceTest {

    private static final TestDataTemplateProvider TEMPLATE_PROVIDER = new TestDataTemplateProvider();

    private static List<Arguments> testRoundTripArguments() {
        List<Arguments> arguments = new ArrayList<>();

        Arrays.stream(CompositionTestDataConformanceSDTJson.values()).forEach(test -> {
            switch (test) {
                case EHRBASE_CONFORMANCE_DATA_TYPES_DV_DURATION:
                    arguments.add(Arguments.of(test, new String[] {}, new String[] {}, new String[] {
                        // see https://github.com/openEHR/archie/issues/379
                        "/content[openEHR-EHR-SECTION.conformance_section.v0, 1]/items[openEHR-EHR-OBSERVATION.conformance_observation.v0, 1]/data[at0001]/events[at0002, 1]/data[at0003]/items[at0018, 1]/value",
                        "/content[openEHR-EHR-SECTION.conformance_section.v0, 1]/items[openEHR-EHR-OBSERVATION.conformance_observation.v0, 1]/data[at0001]/events[at0002, 1]/data[at0003]/items[at0018, 1]/value/other_reference_ranges[1]/range/interval",
                        "/content[openEHR-EHR-SECTION.conformance_section.v0, 1]/items[openEHR-EHR-OBSERVATION.conformance_observation.v0, 1]/data[at0001]/events[at0002, 1]/data[at0003]/items[at0018, 1]/value/normal_range/interval"
                    }));
                    break;

                default:
                    arguments.add(Arguments.of(test, new String[] {}, new String[] {}, new String[] {}));
            }
        });

        return arguments;
    }

    @ParameterizedTest
    @MethodSource("testRoundTripArguments")
    void testRoundTrip(
            CompositionTestDataConformanceSDTJson testData,
            String[] expectedMissing,
            String[] expectedExtra,
            String[] expectedValidationErrorPath)
            throws IOException {

        String templateId = testData.getTemplate().getTemplateId();

        RMDataFormat cut = new FlatJasonProvider(TEMPLATE_PROVIDER).buildFlatJson(FlatFormat.SIM_SDT, templateId);

        String flat = IOUtils.toString(testData.getStream(), StandardCharsets.UTF_8);
        Composition composition = cut.unmarshal(flat);

        CompositionSerializer compositionSerializerRawJson = new CompositionSerializer();

        String db_encoded = compositionSerializerRawJson.dbEncode(composition);

        assertNotNull(db_encoded);

        String converted = new LightRawJsonEncoder(db_encoded).encodeCompositionAsString();

        assertNotNull(converted);

        Composition actual = new CanonicalJson().unmarshal(converted, Composition.class);

        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(actual).isNotNull();

        CompositionValidator rmObjectValidator = new CompositionValidator();

        softAssertions
                .assertThat(rmObjectValidator.validate(
                        actual, TEMPLATE_PROVIDER.buildIntrospect(templateId).orElseThrow()))
                // are stored in deducted tables and thus are not serialised
                .filteredOn(d -> !List.of("/composer", "/language", "/category", "/territory")
                        .contains(d.getAqlPath()))
                // archetype information for composition stored in a deducted  table and thus is not serialised
                .filteredOn(d -> !(Objects.equals("/", d.getAqlPath())
                        && d.getMessage().equals("Invariant Is_archetype_root failed on type COMPOSITION")))
                .filteredOn(d -> !ArrayUtils.contains(expectedValidationErrorPath, d.getAqlPath()))
                .isEmpty();

        String actualString = cut.marshal(actual);

        String expected = IOUtils.toString(testData.getStream(), StandardCharsets.UTF_8);

        List<String> errors = compere(actualString, expected);

        softAssertions
                .assertThat(errors)
                .filteredOn(s -> s.startsWith("Missing"))
                .containsExactlyInAnyOrder(expectedMissing);

        softAssertions
                .assertThat(errors)
                // are stored in deducted tables and thus are not serialised
                .filteredOn(s -> !List.of(
                                "Extra path: conformance-ehrbase.de.v0/_uid, value: 6e3a9506-b81c-4d74-a37f-1464fb7106b2::ehrbase.org::1",
                                "Extra path: conformance-ehrbase.de.v0/language|code, value: en",
                                "Extra path: conformance-ehrbase.de.v0/language|terminology, value: ISO_639-1",
                                "Extra path: conformance-ehrbase.de.v0/territory|code, value: US",
                                "Extra path: conformance-ehrbase.de.v0/territory|terminology, value: ISO_3166-1",
                                "Extra path: conformance-ehrbase.de.v0/category|code, value: 433",
                                "Extra path: conformance-ehrbase.de.v0/category|value, value: event",
                                "Extra path: conformance-ehrbase.de.v0/category|terminology, value: openehr",
                                "Extra path: conformance-ehrbase.de.v0/context/_health_care_facility|id, value: 9091",
                                "Extra path: conformance-ehrbase.de.v0/context/_health_care_facility|id_scheme, value: HOSPITAL-NS",
                                "Extra path: conformance-ehrbase.de.v0/context/_health_care_facility|id_namespace, value: HOSPITAL-NS",
                                "Extra path: conformance-ehrbase.de.v0/context/_health_care_facility|name, value: Hospital",
                                "Extra path: conformance-ehrbase.de.v0/context/_health_care_facility/_identifier:0|id, value: 122",
                                "Extra path: conformance-ehrbase.de.v0/context/_health_care_facility/_identifier:0|issuer, value: issuer",
                                "Extra path: conformance-ehrbase.de.v0/context/_health_care_facility/_identifier:0|assigner, value: assigner",
                                "Extra path: conformance-ehrbase.de.v0/context/_health_care_facility/_identifier:0|type, value: type",
                                "Extra path: conformance-ehrbase.de.v0/context/start_time, value: 2021-12-21T14:19:31.649613+01:00",
                                "Extra path: conformance-ehrbase.de.v0/context/_end_time, value: 2021-12-21T15:19:31.649613+01:00",
                                "Extra path: conformance-ehrbase.de.v0/context/_location, value: microbiology lab 2",
                                "Extra path: conformance-ehrbase.de.v0/context/setting|code, value: 238",
                                "Extra path: conformance-ehrbase.de.v0/context/setting|value, value: other care",
                                "Extra path: conformance-ehrbase.de.v0/context/setting|terminology, value: openehr",
                                "Extra path: ctx/composer_self, value: true",
                                "Extra path: conformance-ehrbase.de.v0/composer|name, value: Silvia Blake",
                                "Extra path: conformance-ehrbase.de.v0/composer|id, value: 1234-5678",
                                "Extra path: conformance-ehrbase.de.v0/composer|id_scheme, value: UUID",
                                "Extra path: conformance-ehrbase.de.v0/composer|id_namespace, value: EHR.NETWORK",
                                "Extra path: conformance-ehrbase.de.v0/composer/_identifier:0|id, value: 122",
                                "Extra path: conformance-ehrbase.de.v0/composer/_identifier:0|issuer, value: issuer",
                                "Extra path: conformance-ehrbase.de.v0/composer/_identifier:0|assigner, value: assigner",
                                "Extra path: conformance-ehrbase.de.v0/composer/_identifier:0|type, value: type",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0|function, value: requester",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0|mode, value: face-to-face communication",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0|name, value: Dr. Marcus Johnson",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0|id, value: 199",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0|id_scheme, value: HOSPITAL-NS",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0|id_namespace, value: HOSPITAL-NS",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0|identifiers_assigner:0, value: assigner",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0|identifiers_issuer:0, value: issuer",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0|identifiers_type:0, value: type",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0|identifiers_id:0, value: 122",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0/relationship|code, value: 10",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0/relationship|value, value: mother",
                                "Extra path: conformance-ehrbase.de.v0/context/_participation:0/relationship|terminology, value: openehr",
                                "Extra path: conformance-ehrbase.de.v0/context/_health_care_facility/relationship|code, value: 10",
                                "Extra path: conformance-ehrbase.de.v0/context/_health_care_facility/relationship|value, value: mother",
                                "Extra path: conformance-ehrbase.de.v0/context/_health_care_facility/relationship|terminology, value: openehr",
                                "Extra path: conformance-ehrbase.de.v0/composer/relationship|code, value: 10",
                                "Extra path: conformance-ehrbase.de.v0/composer/relationship|value, value: mother",
                                "Extra path: conformance-ehrbase.de.v0/composer/relationship|terminology, value: openehr",
                                "Extra path: conformance-ehrbase.de.v0/_link:0|type, value: problem",
                                "Extra path: conformance-ehrbase.de.v0/_link:0|meaning, value: problem related note",
                                "Extra path: conformance-ehrbase.de.v0/_link:0|target, value: ehr://ehr.network/347a5490-55ee-4da9-b91a-9bba710f730e")
                        .contains(s))
                .filteredOn(s -> s.startsWith("Extra"))
                .containsExactlyInAnyOrder(expectedExtra);

        softAssertions.assertAll();
    }

    private List<String> compere(String actualJson, String expectedJson) throws JsonProcessingException {
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
