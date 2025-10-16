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

import java.util.Map;
import org.assertj.core.api.Assertions;
import org.ehrbase.api.exception.IllegalAqlException;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.junit.jupiter.api.Test;

class AqlQueryRequestTest {

    @Test
    void prepareAqlQueryRequest() {

        AqlQueryRequest request = AqlQueryRequest.prepare("SELECT e/ehr_id/value FROM EHR e", Map.of(), null, null);
        assertThat(request.aqlQuery().render()).isEqualTo("SELECT e/ehr_id/value FROM EHR e");
        assertThat(request.parameters()).isEmpty();
        assertThat(request.fetch()).isNull();
        assertThat(request.offset()).isNull();
    }

    @Test
    void prepareInvalidAql() {

        Map<String, Object> parameter = Map.of();
        Assertions.assertThatThrownBy(
                        () -> AqlQueryRequest.prepare("SELECT invalid FROM INVALID i", parameter, null, null))
                .isInstanceOf(IllegalAqlException.class);
    }

    @Test
    void withLimitAndFetch() {

        AqlQueryRequest request = new AqlQueryRequest(null, new AqlQuery(), Map.of(), 10L, 25L);
        assertThat(request.parameters()).isEmpty();
        assertThat(request.fetch()).isEqualTo(10L);
        assertThat(request.offset()).isEqualTo(25L);
    }
}
