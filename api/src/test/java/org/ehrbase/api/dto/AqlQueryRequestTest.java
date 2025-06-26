/*
 * Copyright (c) 2025 vitasystems GmbH.
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
package org.ehrbase.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.ehrbase.api.exception.IllegalAqlException;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.junit.jupiter.api.Test;

class AqlQueryRequestTest {

    @Test
    void parseAqlQueryRequest() {

        AqlQueryRequest request = AqlQueryRequest.parse("SELECT e/ehr_id/value FROM EHR e", Map.of(), null, null);
        assertThat(request.aqlQuery().render()).isEqualTo("SELECT e/ehr_id/value FROM EHR e");
        assertThat(request.parameters()).isEmpty();
        assertThat(request.fetch()).isNull();
        assertThat(request.offset()).isNull();
    }

    @Test
    void parseInvalidAql() {

        Map<String, Object> parameter = Map.of();
        Assertions.assertThatThrownBy(
                        () -> AqlQueryRequest.parse("SELECT invalid FROM INVALID i", parameter, null, null))
                .isInstanceOf(IllegalAqlException.class)
                .hasMessage(
                        "Could not parse AQL query: Cannot parse SELECT invalid FROM INVALID i: unknown FROM alias 'invalid'");
    }

    @Test
    void withLimitAndFetch() {

        AqlQueryRequest request = new AqlQueryRequest(new AqlQuery(), Map.of(), 10L, 25L);
        assertThat(request.parameters()).isEmpty();
        assertThat(request.fetch()).isEqualTo(10L);
        assertThat(request.offset()).isEqualTo(25L);
    }

    @Test
    void withXmlParamsAdjusted() {

        AqlQueryRequest request = new AqlQueryRequest(
                new AqlQuery(),
                Map.of(
                        "p_string", "some-string",
                        "p_xml_num", Map.of("type", "num", "", 42.12),
                        "p_xml_int", Map.of("type", "int", "", 11)
                        // "p_list": L

                        ),
                null,
                null);
        assertThat(request.parameters())
                .containsAllEntriesOf(Map.of("p_string", "some-string", "p_xml_num", 42.12, "p_xml_int", 11));
        assertThat(request.fetch()).isNull();
        assertThat(request.offset()).isNull();
    }

    @Test
    void withXmlParamsWithoutTypeAdjusted() {

        AqlQueryRequest request = new AqlQueryRequest(
                new AqlQuery(),
                Map.of(
                        "p_xml_num", Map.of("num", 42.12),
                        "p_xml_int", Map.of("int", 11)
                        // "p_list": L

                        ),
                null,
                null);
        assertThat(request.parameters()).containsAllEntriesOf(Map.of("p_xml_num", 42.12, "p_xml_int", 11));
        assertThat(request.fetch()).isNull();
        assertThat(request.offset()).isNull();
    }

    @Test
    void withXmlParamListsAdjusted() {

        AqlQueryRequest request = new AqlQueryRequest(
                new AqlQuery(),
                Map.of(
                        "p_xml_list", Map.of("", List.of("value_1", "value_2")),
                        "p_xml_list_alternative", List.of("some", "other", "value")),
                null,
                null);
        assertThat(request.parameters())
                .containsAllEntriesOf(Map.of(
                        "p_xml_list", List.of("value_1", "value_2"),
                        "p_xml_list_alternative", List.of("some", "other", "value")));
        assertThat(request.fetch()).isNull();
        assertThat(request.offset()).isNull();
    }
}
