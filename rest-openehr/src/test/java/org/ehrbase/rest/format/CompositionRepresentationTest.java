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
package org.ehrbase.rest.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.rest.openehr.format.CompositionRepresentation;
import org.ehrbase.rest.openehr.format.OpenEHRMediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;

class CompositionRepresentationTest {

    @Test
    void acceptsMediaTypeApplicationXML() {

        assertSame(
                CompositionRepresentation.XML,
                CompositionRepresentation.selectFromMediaTypeWithFormat(MediaType.APPLICATION_XML, null));
    }

    @Test
    void failsForMediaTypeApplicationXHTML() {

        assertFailsWith(
                "Only compositions in XML or JSON are supported at the moment",
                NotAcceptableException.class,
                () -> assertSame(
                        CompositionRepresentation.XML,
                        CompositionRepresentation.selectFromMediaTypeWithFormat(
                                MediaType.APPLICATION_XHTML_XML, null)));
    }

    @Test
    void failsForMediaTypeApplicationXML_FLAT() {

        assertFailsWith(
                "Only composition format [XML] is supported at the moment for type [application/xml]",
                NotAcceptableException.class,
                () -> assertSame(
                        CompositionRepresentation.XML,
                        CompositionRepresentation.selectFromMediaTypeWithFormat(
                                MediaType.APPLICATION_XML, CompositionFormat.FLAT)));
    }

    @Test
    void failsForMediaTypApplicationeXML_STRUCTURED() {

        assertFailsWith(
                "Only composition format [XML] is supported at the moment for type [application/xml]",
                NotAcceptableException.class,
                () -> assertSame(
                        CompositionRepresentation.XML,
                        CompositionRepresentation.selectFromMediaTypeWithFormat(
                                MediaType.APPLICATION_XML, CompositionFormat.STRUCTURED)));
    }

    @ParameterizedTest
    @EnumSource(
            value = CompositionFormat.class,
            names = {"RAW", "EXPANDED", "ECISFLAT"})
    void failsForMediaTypeApplicationXMLUnsupportedFormats(CompositionFormat format) {

        assertFailsWith(
                "Format %s not supported".formatted(format.name()),
                InvalidApiParameterException.class,
                () -> assertSame(
                        CompositionRepresentation.XML,
                        CompositionRepresentation.selectFromMediaTypeWithFormat(MediaType.APPLICATION_XML, format)));
    }

    @Test
    void acceptsMediaTypeApplicationJSON() {

        assertSame(
                CompositionRepresentation.JSON,
                CompositionRepresentation.selectFromMediaTypeWithFormat(MediaType.APPLICATION_JSON, null));
    }

    @Test
    void acceptsMediaTypeApplicationJSON_FLAT() {

        assertSame(
                CompositionRepresentation.JSON_FLAT,
                CompositionRepresentation.selectFromMediaTypeWithFormat(
                        MediaType.APPLICATION_JSON, CompositionFormat.FLAT));
    }

    @Test
    void acceptsMediaTypeApplicationJSON_STUCTURED() {

        assertSame(
                CompositionRepresentation.JSON_STRUCTURED,
                CompositionRepresentation.selectFromMediaTypeWithFormat(
                        MediaType.APPLICATION_JSON, CompositionFormat.STRUCTURED));
    }

    @Test
    void failsForMediaTypeApplicationNDJSON() {

        assertFailsWith(
                "Only compositions in XML or JSON are supported at the moment",
                NotAcceptableException.class,
                () -> assertSame(
                        CompositionRepresentation.JSON,
                        CompositionRepresentation.selectFromMediaTypeWithFormat(MediaType.APPLICATION_NDJSON, null)));
    }

    @ParameterizedTest
    @EnumSource(
            value = CompositionFormat.class,
            names = {"RAW", "EXPANDED", "ECISFLAT"})
    void failsForMediaTypeApplicationJSONUnsupportedFormats(CompositionFormat format) {

        assertFailsWith(
                "Format %s not supported".formatted(format.name()),
                InvalidApiParameterException.class,
                () -> assertSame(
                        CompositionRepresentation.JSON,
                        CompositionRepresentation.selectFromMediaTypeWithFormat(MediaType.APPLICATION_JSON, format)));
    }

    @Test
    void acceptsMediaTypeApplicationWtFlatJSON() {

        assertSame(
                CompositionRepresentation.JSON_FLAT,
                CompositionRepresentation.selectFromMediaTypeWithFormat(
                        OpenEHRMediaType.APPLICATION_WT_FLAT_SCHEMA_JSON, null));
    }

    @Test
    void acceptsMediaTypeApplicationWtStructuredJSON() {

        assertSame(
                CompositionRepresentation.JSON_STRUCTURED,
                CompositionRepresentation.selectFromMediaTypeWithFormat(
                        OpenEHRMediaType.APPLICATION_WT_STRUCTURED_SCHEMA_JSON, null));
    }

    // Simple assert lambda that ensures an exception is raised

    @FunctionalInterface
    private interface Check {

        void invoke() throws Exception;
    }

    private static <T extends Exception> void assertFailsWith(String message, Class<T> type, Check check) {

        try {
            check.invoke();
            fail("Expected to fail with [%s] but succeeded".formatted(type.getSimpleName()));
        } catch (Exception e) {
            assertEquals(type, e.getClass(), "Exception type not expected");
            assertEquals(message, e.getMessage());
        }
    }
}
