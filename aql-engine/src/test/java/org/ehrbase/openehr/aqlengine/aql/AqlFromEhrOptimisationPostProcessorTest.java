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
package org.ehrbase.openehr.aqlengine.aql;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AqlFromEhrOptimisationPostProcessorTest {

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            SELECT e FROM EHR e |
            SELECT e/ehr_id/value FROM EHR e CONTAINS COMPOSITION c |
            SELECT c FROM EHR e[ehr_id/value = '5dd64358-76b4-4ffe-8d05-554406d9d023'] CONTAINS COMPOSITION c |
            SELECT s_el FROM EHR e CONTAINS (COMPOSITION c AND EHR_STATUS CONTAINS ELEMENT s_el) |
            SELECT c/uid/value FROM EHR e CONTAINS COMPOSITION c WHERE e/ehr_id/value = '5dd64358-76b4-4ffe-8d05-554406d9d023' |
            SELECT c FROM EHR e CONTAINS COMPOSITION c WHERE c/uid/value = '5dd64358-76b4-4ffe-8d05-554406d9d023' | SELECT c FROM COMPOSITION c WHERE c/uid/value = '5dd64358-76b4-4ffe-8d05-554406d9d023'
            """,
            delimiterString = "|")
    void removeRedundantFromEhr(String originalAql, String optimizedAql) {
        AqlQuery query = AqlQueryParser.parse(originalAql);
        AqlFromEhrOptimisationPostProcessor cut = new AqlFromEhrOptimisationPostProcessor();
        cut.afterParseAql(query, null, null);

        String expected = AqlQueryParser.parse(StringUtils.isBlank(optimizedAql) ? originalAql : optimizedAql)
                .render();
        assertThat(query.render()).isEqualTo(expected);
    }
}
