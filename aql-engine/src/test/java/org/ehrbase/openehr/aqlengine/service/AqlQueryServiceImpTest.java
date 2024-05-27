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
package org.ehrbase.openehr.aqlengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AqlQueryServiceImpTest {

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            SELECT e/ehr_status AS s FROM EHR e=>SELECT s AS s FROM EHR e CONTAINS EHR_STATUS s
            SELECT s/uid/value, e/ehr_status/subject/external_ref/id FROM EHR e CONTAINS COMPOSITION s WHERE e/ehr_status/is_modifiable = true=>SELECT s/uid/value, s1/subject/external_ref/id FROM EHR e CONTAINS (EHR_STATUS s1 AND COMPOSITION s) WHERE s1/is_modifiable = true
            """,
            delimiterString = "=>")
    void resolveEhrStatus(String srcAql, String expectedAql) {

        AqlQuery aqlQuery = AqlQueryParser.parse(srcAql);
        AqlQueryServiceImp.replaceEhrPaths(aqlQuery);
        assertThat(aqlQuery.render()).isEqualTo(expectedAql.replaceAll(" +", " "));
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            SELECT e/compositions AS c FROM EHR e=>SELECT c AS c FROM EHR e CONTAINS COMPOSITION c
            SELECT c/uid/value, e/compositions/uid/value FROM EHR e CONTAINS COMPOSITION c WHERE e/compositions/archetype_details/template_id/value = 'tpl.v0'=>SELECT c/uid/value, c1/uid/value FROM EHR e CONTAINS (COMPOSITION c1 AND COMPOSITION c) WHERE c1/archetype_details/template_id/value = 'tpl.v0'
            """,
            delimiterString = "=>")
    void resolveEhrCompositions(String srcAql, String expectedAql) {

        AqlQuery aqlQuery = AqlQueryParser.parse(srcAql);
        AqlQueryServiceImp.replaceEhrPaths(aqlQuery);
        assertThat(aqlQuery.render()).isEqualTo(expectedAql.replaceAll(" +", " "));
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
                5||10||Query contains a LIMIT clause, fetch and offset parameters must not be used
                5|20||40|Query contains a LIMIT clause, fetch and offset parameters must not be used
                5|||30|Query contains a LIMIT clause, fetch and offset parameters must not be used
                |||42|Query parameter for offset 42 provided without a fetch limit
            """,
            delimiterString = "|")
    void queryOffsetLimitRejected(
            String aqlLimit, String aqlOffset, String paramLimit, String paramOffset, String message) {

        assertThatThrownBy(() -> runQueryTest(aqlLimit, aqlOffset, paramLimit, paramOffset))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage(message);
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
                5|||
                5|15||
                ||20|
                ||20|25
            """,
            delimiterString = "|")
    void queryOffsetLimitAccepted(String aqlLimit, String aqlOffset, String paramLimit, String paramOffset) {
        runQueryTest(aqlLimit, aqlOffset, paramLimit, paramOffset);
    }

    private void runQueryTest(String aqlLimit, String aqlOffset, String paramLimit, String paramOffset) {
        // @format:off
        String query = "SELECT s FROM EHR_STATUS s %s %s".formatted(
                Optional.ofNullable(aqlLimit).filter(s -> !s.isEmpty()).map(s -> "LIMIT " + s).orElse(""),
                Optional.ofNullable(aqlOffset).filter(s -> !s.isEmpty()).map(s -> "OFFSET " + s).orElse("")
        );

        AqlQueryServiceImp.buildAqlQuery(new AqlQueryRequest(
                query,
                Map.of(),
                Optional.ofNullable(paramLimit).filter(StringUtils::isNotEmpty).map(Long::parseLong).orElse(null),
                Optional.ofNullable(paramOffset).filter(s -> !s.isEmpty()).map(Long::parseLong).orElse(null))
        );
        // @format:on
    }
}
