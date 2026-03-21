/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.rest.api.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.ehrbase.api.exception.NotAcceptableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Migrated from CompositionRepresentationTest (12 tests) — tests format negotiation logic.
 */
class FormatNegotiatorTest {

    // Migrated: acceptsMediaTypeApplicationJSON
    @Test
    void resolveInputJson() {
        assertThat(FormatNegotiator.resolveInput("application/json", null)).isEqualTo(FormatNegotiator.Format.CANONICAL_JSON);
    }

    // Migrated: acceptsMediaTypeApplicationXML
    @Test
    void resolveInputXml() {
        assertThat(FormatNegotiator.resolveInput("application/xml", null)).isEqualTo(FormatNegotiator.Format.CANONICAL_XML);
    }

    // Migrated: acceptsMediaTypeApplicationJSON_FLAT
    @Test
    void resolveInputFlat() {
        assertThat(FormatNegotiator.resolveInput("application/openehr.wt.flat+json", null)).isEqualTo(FormatNegotiator.Format.FLAT);
    }

    // Migrated: acceptsMediaTypeApplicationJSON_STRUCTURED
    @Test
    void resolveInputStructured() {
        assertThat(FormatNegotiator.resolveInput("application/openehr.wt.structured+json", null)).isEqualTo(FormatNegotiator.Format.STRUCTURED);
    }

    // Migrated: format query param overrides content type
    @Test
    void formatParamOverridesContentType() {
        assertThat(FormatNegotiator.resolveInput("application/xml", "JSON")).isEqualTo(FormatNegotiator.Format.CANONICAL_JSON);
        assertThat(FormatNegotiator.resolveInput("application/json", "XML")).isEqualTo(FormatNegotiator.Format.CANONICAL_XML);
        assertThat(FormatNegotiator.resolveInput("application/json", "FLAT")).isEqualTo(FormatNegotiator.Format.FLAT);
        assertThat(FormatNegotiator.resolveInput("application/json", "STRUCTURED")).isEqualTo(FormatNegotiator.Format.STRUCTURED);
    }

    // Migrated: failsForMediaTypeApplicationXMLUnsupportedFormats + failsForMediaTypeApplicationJSONUnsupportedFormats
    @ParameterizedTest
    @ValueSource(strings = {"RAW", "EXPANDED", "ECISFLAT", "unsupported"})
    void resolveInputUnsupportedFormat(String format) {
        assertThatThrownBy(() -> FormatNegotiator.resolveInput("application/json", format))
                .isInstanceOf(NotAcceptableException.class)
                .hasMessageContaining("Unsupported format");
    }

    // Migrated: resolveOutput defaults to JSON when null or wildcard
    @Test
    void resolveOutputDefaultsToJson() {
        assertThat(FormatNegotiator.resolveOutput(null, null)).isEqualTo(FormatNegotiator.Format.CANONICAL_JSON);
        assertThat(FormatNegotiator.resolveOutput("*/*", null)).isEqualTo(FormatNegotiator.Format.CANONICAL_JSON);
    }

    // Migrated: resolveOutput from accept header
    @ParameterizedTest
    @CsvSource({
        "application/json,CANONICAL_JSON",
        "application/xml,CANONICAL_XML",
        "application/openehr.wt.flat+json,FLAT",
        "application/openehr.wt.structured+json,STRUCTURED"
    })
    void resolveOutputFromAccept(String accept, String expected) {
        assertThat(FormatNegotiator.resolveOutput(accept, null)).isEqualTo(FormatNegotiator.Format.valueOf(expected));
    }

    // Migrated: format param overrides accept header
    @Test
    void resolveOutputFormatParamOverrides() {
        assertThat(FormatNegotiator.resolveOutput("application/xml", "JSON")).isEqualTo(FormatNegotiator.Format.CANONICAL_JSON);
        assertThat(FormatNegotiator.resolveOutput("application/json", "FLAT")).isEqualTo(FormatNegotiator.Format.FLAT);
    }

    // Migrated: case-insensitive format param
    @Test
    void formatParamCaseInsensitive() {
        assertThat(FormatNegotiator.resolveInput("application/json", "json")).isEqualTo(FormatNegotiator.Format.CANONICAL_JSON);
        assertThat(FormatNegotiator.resolveInput("application/json", "Xml")).isEqualTo(FormatNegotiator.Format.CANONICAL_XML);
        assertThat(FormatNegotiator.resolveInput("application/json", "flat")).isEqualTo(FormatNegotiator.Format.FLAT);
    }

    // NEW: null content type defaults to JSON
    @Test
    void resolveInputNullContentType() {
        assertThat(FormatNegotiator.resolveInput(null, null)).isEqualTo(FormatNegotiator.Format.CANONICAL_JSON);
    }

    // NEW: blank format param is ignored
    @Test
    void blankFormatParamIgnored() {
        assertThat(FormatNegotiator.resolveInput("application/xml", "")).isEqualTo(FormatNegotiator.Format.CANONICAL_XML);
        assertThat(FormatNegotiator.resolveInput("application/xml", "  ")).isEqualTo(FormatNegotiator.Format.CANONICAL_XML);
    }

    // NEW: Format enum values
    @Test
    void formatMediaTypes() {
        assertThat(FormatNegotiator.Format.CANONICAL_JSON.paramValue()).isEqualTo("JSON");
        assertThat(FormatNegotiator.Format.CANONICAL_XML.paramValue()).isEqualTo("XML");
        assertThat(FormatNegotiator.Format.FLAT.paramValue()).isEqualTo("FLAT");
        assertThat(FormatNegotiator.Format.STRUCTURED.paramValue()).isEqualTo("STRUCTURED");
    }
}
