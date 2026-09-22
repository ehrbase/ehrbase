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

import org.ehrbase.api.exception.PreconditionFailedException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class BaseControllerTest {

    @ParameterizedTest
    @CsvSource(delimiter = ';', textBlock = """
        305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1        ; 305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1
        "305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1"      ; 305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1
        305eb2fd-c228-445c-ada7-5429d852fbb2::some-system.example.org::42 ; 305eb2fd-c228-445c-ada7-5429d852fbb2::some-system.example.org::42
        "305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1.0.1"  ; 305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1.0.1
        """)
    void parseIfMatchHeaderValueAccepts(String ifMatch, String expected) {
        assertThat(BaseController.parseIfMatchHeaderValue(ifMatch)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void parseIfMatchHeaderValueRejectsMissing(String ifMatch) {
        assertRejected(ifMatch, "If-Match header is missing or empty");
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', textBlock = """
        W/"305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1"
        W/305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1
        """)
    void parseIfMatchHeaderValueRejectsWeakValidator(String ifMatch) {
        assertRejected(ifMatch, "If-Match header [%s] must not be a weak validator".formatted(ifMatch));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', textBlock = """
        *
        "*"
        """)
    void parseIfMatchHeaderValueRejectsWildcard(String ifMatch) {
        assertRejected(ifMatch, "If-Match header must reference a specific version, '*' is not supported");
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', textBlock = """
        "305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1
        305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1"
        "305eb2fd-c228-445c-ada7-5429d852fbb2"::local.ehrbase.org::1"
        "
        ""
        "305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1", "305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::2"
        305eb2fd-c228-445c-ada7-5429d852fbb2
        305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org
        ::local.ehrbase.org::1
        "::local.ehrbase.org::1"
        305eb2fd-c228-445c-ada7-5429d852fbb2::::1
        305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::
        "305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::"
        305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1::2
        "305eb2fd-c228-445c-ada7-5429d852fbb2::local.ehrbase.org::1::2"
        """)
    void parseIfMatchHeaderValueRejectsMalformedVersionUid(String ifMatch) {
        assertRejected(ifMatch, BaseController.IF_MATCH_NOT_A_VERSION_UID.formatted(ifMatch));
    }

    private static void assertRejected(String ifMatch, String expectedMessage) {
        assertThatThrownBy(() -> BaseController.parseIfMatchHeaderValue(ifMatch))
                .isInstanceOf(PreconditionFailedException.class)
                .hasMessage(expectedMessage);
    }
}
