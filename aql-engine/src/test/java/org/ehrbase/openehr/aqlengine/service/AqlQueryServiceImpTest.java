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
                5||10||REJECT||||Query contains a LIMIT clause, fetch and offset parameters must not be used (with fetch precedence REJECT)
                5|20||40|REJECT||||Query parameter for offset provided, but no fetch parameter
                5|20||40|MIN_FETCH||||Query parameter for offset provided, but no fetch parameter
                5|||30|REJECT||||Query parameter for offset provided, but no fetch parameter
                |||42|REJECT||||Query parameter for offset provided, but no fetch parameter
                20||||REJECT||19||Query LIMIT 20 exceeds maximum limit 19
                20||||MIN_FETCH||19||Query LIMIT 20 exceeds maximum limit 19
                ||20||REJECT|||19|Fetch parameter 20 exceeds maximum fetch 19
                ||20||MIN_FETCH|||19|Fetch parameter 20 exceeds maximum fetch 19
                20|5|30||MIN_FETCH||||Query contains a OFFSET clause, fetch parameter must not be used (with fetch precedence MIN_FETCH)
            """,
            delimiterString = "|")
    void queryOffsetLimitRejected(
            String aqlLimit,
            String aqlOffset,
            String paramLimit,
            String paramOffset,
            AqlQueryServiceImp.FetchPrecedence fetchPrecedence,
            String defaultLimit,
            String maxLimit,
            String maxFetch,
            String message) {

        assertThatThrownBy(() -> runQueryTest(
                        aqlLimit,
                        aqlOffset,
                        paramLimit,
                        paramOffset,
                        fetchPrecedence,
                        defaultLimit,
                        maxLimit,
                        maxFetch))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage(message);
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
                ||||REJECT|||
                5||||REJECT|||
                5|15|||REJECT|||
                ||20||REJECT|||
                ||20|25|REJECT|||
                ||||REJECT|20|10|10
                20|30|||REJECT|20|20|20
                ||20|50|REJECT|20|20|20
                30||20|50|MIN_FETCH|30|30|20
                10||20|50|MIN_FETCH|30|30|20
            """,
            delimiterString = "|")
    void queryOffsetLimitAccepted(
            String aqlLimit,
            String aqlOffset,
            String paramLimit,
            String paramOffset,
            AqlQueryServiceImp.FetchPrecedence fetchPrecedence,
            String defaultLimit,
            String maxLimit,
            String maxFetch) {
        runQueryTest(aqlLimit, aqlOffset, paramLimit, paramOffset, fetchPrecedence, defaultLimit, maxLimit, maxFetch);
    }

    private void runQueryTest(
            String aqlLimit,
            String aqlOffset,
            String paramLimit,
            String paramOffset,
            AqlQueryServiceImp.FetchPrecedence fetchPrecedence,
            String defaultLimit,
            String maxLimit,
            String maxFetch) {
        // @format:off
        String query = "SELECT s FROM EHR_STATUS s %s %s".formatted(
                parseLong(aqlLimit).map(s -> "LIMIT " + s).orElse(""),
                parseLong(aqlOffset).map(s -> "OFFSET " + s).orElse("")
        );

        AqlQueryServiceImp.buildAqlQuery(
                new AqlQueryRequest(
                        query,
                        Map.of(),
                        parseLong(paramLimit).orElse(null),
                        Optional.ofNullable(paramOffset)
                                .filter(s -> !s.isEmpty())
                                .map(Long::parseLong)
                                .orElse(null)),
                fetchPrecedence,
                parseLong(defaultLimit).orElse(null),
                parseLong(maxLimit).orElse(null),
                parseLong(maxFetch).orElse(null));
        // @format:on
    }

    private static Optional<Long> parseLong(String longStr) {
        return Optional.ofNullable(longStr).filter(StringUtils::isNotEmpty).map(Long::parseLong);
    }
}
