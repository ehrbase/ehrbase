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
package org.ehrbase.service.graphql.fetcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

/**
 * Tests for FilterTranslator — each operator: eq, in, gt, gte, lt, lte, contains, textSearch.
 */
class FilterTranslatorTest {

    private static final String SCHEMA = "ehr_views";
    private static final String VIEW = "v_blood_pressure";

    @Test
    void nullFilterReturnsTrueCondition() {
        Condition condition = FilterTranslator.translate(null, SCHEMA, VIEW);
        assertThat(condition).isEqualTo(DSL.trueCondition());
    }

    @Test
    void emptyFilterReturnsTrueCondition() {
        Condition condition = FilterTranslator.translate(Map.of(), SCHEMA, VIEW);
        assertThat(condition).isEqualTo(DSL.trueCondition());
    }

    @Test
    void eqOperator() {
        var filter = Map.<String, Object>of("ehrId", Map.of("eq", "abc-123"));
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        assertThat(condition.toString()).contains("ehr_id");
    }

    @Test
    void gtOperator() {
        var filter = Map.<String, Object>of("systolicMagnitude", Map.of("gt", 120));
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        assertThat(condition.toString()).contains("systolic_magnitude");
        assertThat(condition.toString()).contains(">");
    }

    @Test
    void gteOperator() {
        var filter = Map.<String, Object>of("systolicMagnitude", Map.of("gte", 120));
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        assertThat(condition.toString()).contains(">=");
    }

    @Test
    void ltOperator() {
        var filter = Map.<String, Object>of("systolicMagnitude", Map.of("lt", 80));
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        assertThat(condition.toString()).contains("<");
    }

    @Test
    void lteOperator() {
        var filter = Map.<String, Object>of("systolicMagnitude", Map.of("lte", 80));
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        assertThat(condition.toString()).contains("<=");
    }

    @Test
    void inOperator() {
        var filter = Map.<String, Object>of("category", Map.of("in", List.of("event", "persistent")));
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        assertThat(condition.toString()).contains("in");
    }

    @Test
    void containsOperator() {
        var filter = Map.<String, Object>of("composerName", Map.of("contains", "Smith"));
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        String sql = condition.toString().toLowerCase();
        assertThat(sql).contains("composer_name");
    }

    @Test
    void textSearchOperator() {
        var filter = Map.<String, Object>of("textSearch", Map.of("query", "blood pressure"));
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        assertThat(condition.toString()).contains("search_vector");
        assertThat(condition.toString()).contains("plainto_tsquery");
    }

    @Test
    void multipleFiltersAndCombined() {
        var filter = Map.<String, Object>of(
                "systolicMagnitude", Map.of("gt", 120),
                "diastolicMagnitude", Map.of("lt", 80));
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        String sql = condition.toString();
        assertThat(sql).contains("systolic_magnitude");
        assertThat(sql).contains("diastolic_magnitude");
    }

    @Test
    void unknownOperatorIgnored() {
        var filter = Map.<String, Object>of("field", Map.of("unknownOp", "value"));
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        // Unknown operator maps to trueCondition, which doesn't add meaningful filter
        assertThat(condition).isNotNull();
    }

    @Test
    void nonMapFilterValueIgnored() {
        var filter = Map.<String, Object>of("simpleField", "just-a-string");
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        assertThat(condition).isEqualTo(DSL.trueCondition());
    }

    @Test
    void camelToSnakeConversion() {
        // ehrId → ehr_id, systolicMagnitude → systolic_magnitude
        var filter = Map.<String, Object>of("ehrId", Map.of("eq", "test"));
        Condition condition = FilterTranslator.translate(filter, SCHEMA, VIEW);
        assertThat(condition.toString()).contains("ehr_id");
    }
}
