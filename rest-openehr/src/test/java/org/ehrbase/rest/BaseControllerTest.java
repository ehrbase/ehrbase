/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class BaseControllerTest {

    private static final String UID = "305eb2fd-c228-445c-ada7-5429d852fbb2";

    private static final String NOT_A_VERSION_UID = "If-Match header [%s] is not a valid version uid";

    /**
     * Values that are a valid <code>If-Match</code>, in quoted (RFC 7232 ETag) or unquoted form.
     * The expected result is always the bare OBJECT_VERSION_ID.
     */
    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(
            delimiter = ';',
            value = {
                // unquoted
                "305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1 ; 305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1",
                // quoted, as emitted by the ETag response header
                "\"305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1\" ; 305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1",
                // creating_system_id may contain dots and dashes
                "305eb2fd-c228-445c-ada7-5429d852fbb2::some-system.example.org::42 ; 305eb2fd-c228-445c-ada7-5429d852fbb2::some-system.example.org::42",
                // a branched version_tree_id is a valid OBJECT_VERSION_ID shape
                "\"305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1.0.1\" ; 305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1.0.1"
            })
    void parseIfMatchHeaderValueAccepts(String ifMatch, String expected) {
        assertThat(BaseController.parseIfMatchHeaderValue(ifMatch)).isEqualTo(expected);
    }

    static Stream<Arguments> rejectedIfMatchValues() {
        return Stream.of(
                // no usable content
                Arguments.of(null, "If-Match header is missing or empty"),
                Arguments.of("", "If-Match header is missing or empty"),

                // weak validators are not acceptable for a state-changing request (RFC 7232 §3.1)
                Arguments.of(
                        "W/\"%s::local.ehrbase.org::1\"".formatted(UID),
                        "If-Match header [W/\"%s::local.ehrbase.org::1\"] must not be a weak validator".formatted(UID)),
                Arguments.of(
                        "W/%s::local.ehrbase.org::1".formatted(UID),
                        "If-Match header [W/%s::local.ehrbase.org::1] must not be a weak validator".formatted(UID)),

                // the wildcard cannot express "this specific version"
                Arguments.of("*", "If-Match header must reference a specific version, '*' is not supported"),
                Arguments.of("\"*\"", "If-Match header must reference a specific version, '*' is not supported"),

                // unbalanced or stray quotes
                Arguments.of(
                        "\"%s::local.ehrbase.org::1".formatted(UID),
                        NOT_A_VERSION_UID.formatted("\"%s::local.ehrbase.org::1".formatted(UID))),
                Arguments.of(
                        "%s::local.ehrbase.org::1\"".formatted(UID),
                        NOT_A_VERSION_UID.formatted("%s::local.ehrbase.org::1\"".formatted(UID))),
                Arguments.of(
                        "\"%s\"::local.ehrbase.org::1\"".formatted(UID),
                        NOT_A_VERSION_UID.formatted("\"%s\"::local.ehrbase.org::1\"".formatted(UID))),
                Arguments.of("\"", NOT_A_VERSION_UID.formatted("\"")),
                Arguments.of("\"\"", NOT_A_VERSION_UID.formatted("\"\"")),

                // a list of validators is not supported, only a single version
                Arguments.of(
                        "\"%s::local.ehrbase.org::1\", \"%s::local.ehrbase.org::2\"".formatted(UID, UID),
                        NOT_A_VERSION_UID.formatted(
                                "\"%s::local.ehrbase.org::1\", \"%s::local.ehrbase.org::2\"".formatted(UID, UID))),

                // not shaped like object_id::creating_system_id::version_tree_id
                Arguments.of(UID, NOT_A_VERSION_UID.formatted(UID)),
                Arguments.of(
                        "%s::local.ehrbase.org".formatted(UID),
                        NOT_A_VERSION_UID.formatted("%s::local.ehrbase.org".formatted(UID))),
                Arguments.of("::local.ehrbase.org::1", NOT_A_VERSION_UID.formatted("::local.ehrbase.org::1")),
                Arguments.of("\"::local.ehrbase.org::1\"", NOT_A_VERSION_UID.formatted("\"::local.ehrbase.org::1\"")),
                Arguments.of("%s::::1".formatted(UID), NOT_A_VERSION_UID.formatted("%s::::1".formatted(UID))),
                Arguments.of(
                        "%s::local.ehrbase.org::".formatted(UID),
                        NOT_A_VERSION_UID.formatted("%s::local.ehrbase.org::".formatted(UID))),
                Arguments.of(
                        "\"%s::local.ehrbase.org::\"".formatted(UID),
                        NOT_A_VERSION_UID.formatted("\"%s::local.ehrbase.org::\"".formatted(UID))),
                Arguments.of(
                        "%s::local.ehrbase.org::1::2".formatted(UID),
                        NOT_A_VERSION_UID.formatted("%s::local.ehrbase.org::1::2".formatted(UID))));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("rejectedIfMatchValues")
    void parseIfMatchHeaderValueRejects(String ifMatch, String expectedMessage) {
        assertThatThrownBy(() -> BaseController.parseIfMatchHeaderValue(ifMatch))
                .isInstanceOf(PreconditionFailedException.class)
                .hasMessage(expectedMessage);
    }
}
