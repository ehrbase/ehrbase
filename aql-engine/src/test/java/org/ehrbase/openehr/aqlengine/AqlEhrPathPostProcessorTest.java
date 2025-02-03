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
package org.ehrbase.openehr.aqlengine;

import static org.assertj.core.api.Assertions.assertThat;

import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AqlEhrPathPostProcessorTest {
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
        AqlEhrPathPostProcessor.replaceEhrPaths(aqlQuery);
        assertThat(aqlQuery.render()).isEqualTo(expectedAql.replaceAll(" +", " "));
    }
}
