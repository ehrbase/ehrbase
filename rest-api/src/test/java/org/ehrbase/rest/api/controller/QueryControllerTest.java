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
package org.ehrbase.rest.api.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.rest.api.dto.QueryRequestDto;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.ViewCatalogService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Migrated from OpenehrQueryControllerTest (partial — AQL-specific tests dropped)
 * + new SQL query controller tests.
 */
class QueryControllerTest {

    private final DSLContext mockDsl = mock();
    private final ViewCatalogService mockViewCatalog = mock();
    private final RequestContext mockRequestContext = mock();

    private final QueryController controller = new QueryController(mockDsl, mockViewCatalog, mockRequestContext);

    // Migrated: executeAddHocQueryUsingPOSTWithFetchInvalid → empty query
    @Test
    void executeSqlEmptyQuery() {
        assertThatThrownBy(() -> controller.executeSql(new QueryRequestDto("", null, null)))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void executeSqlNullQuery() {
        assertThatThrownBy(() -> controller.executeSql(new QueryRequestDto(null, null, null)))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessageContaining("must not be empty");
    }

    // NEW: Only SELECT allowed
    @ParameterizedTest
    @ValueSource(
            strings = {"INSERT INTO ehr_views.x VALUES (1)", "UPDATE ehr_views.x SET a=1", "DELETE FROM ehr_views.x"})
    void executeSqlNonSelectRejected(String sql) {
        assertThatThrownBy(() -> controller.executeSql(new QueryRequestDto(sql, null, null)))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessageContaining("Only SELECT");
    }

    // NEW: ehr_system and ehr_data schemas forbidden
    @ParameterizedTest
    @ValueSource(strings = {"SELECT * FROM ehr_system.ehr", "SELECT * FROM ehr_data.blood_pressure"})
    void executeSqlForbiddenSchemas(String sql) {
        assertThatThrownBy(() -> controller.executeSql(new QueryRequestDto(sql, null, null)))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessageContaining("ehr_views schema only");
    }

    // NEW: EXPLAIN with empty SQL
    @Test
    void explainEmptySql() {
        assertThatThrownBy(() -> controller.explainQuery(new QueryRequestDto("", null, null)))
                .isInstanceOf(InvalidApiParameterException.class);
    }
}
