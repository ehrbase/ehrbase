package org.ehrbase.openehr.aqlengine.sql;

import org.ehrbase.api.dto.AqlQueryRequest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ParsedQueryTest {

    @Test
    void getSqlNoPagination() {
        String sql = "SELECT rm_entity, entity_concept, entity_name, data FROM ehr.comp_data";
        AqlQueryRequest queryRequest = new AqlQueryRequest(sql, null, null, null);

        ParsedQuery parsedQuery = new ParsedQuery(queryRequest);

        assertSoftly(softly -> {
            softly.assertThat(parsedQuery.getSql()).isEqualTo(sql);
            softly.assertThat(parsedQuery.getLimit()).isNull();
            softly.assertThat(parsedQuery.getOffset()).isNull();
        });
    }

    @Test
    void getSqlWithLimit() {
        String sql = "SELECT rm_entity, entity_concept, entity_name, data FROM ehr.comp_data";
        AqlQueryRequest queryRequest = new AqlQueryRequest(sql, null, 10L, null);

        ParsedQuery parsedQuery = new ParsedQuery(queryRequest);

        assertSoftly(softly -> {
            softly.assertThat(parsedQuery.getSql())
                    .isEqualTo("SELECT rm_entity, entity_concept, entity_name, data FROM ehr.comp_data LIMIT 10");
            softly.assertThat(parsedQuery.getLimit()).isEqualTo(10);
            softly.assertThat(parsedQuery.getOffset()).isNull();
        });
    }

    @Test
    void getSqlExistingLimitDoesNotAddAnother() {
        String sql = "SELECT rm_entity, entity_concept, entity_name, data FROM ehr.comp_data LIMIT 10";
        AqlQueryRequest queryRequest = new AqlQueryRequest(sql, null, 20L, null);

        ParsedQuery parsedQuery = new ParsedQuery(queryRequest);

        assertSoftly(softly -> {
            softly.assertThat(parsedQuery.getSql()).isEqualTo(sql);
            softly.assertThat(parsedQuery.getLimit()).isNull();
            softly.assertThat(parsedQuery.getOffset()).isNull();
        });
    }

    @Test
    void getSqlWithOffset() {
        String sql = "SELECT rm_entity, entity_concept, entity_name, data FROM ehr.comp_data";
        AqlQueryRequest queryRequest = new AqlQueryRequest(sql, null, null, 5L);

        ParsedQuery parsedQuery = new ParsedQuery(queryRequest);

        assertSoftly(softly -> {
            softly.assertThat(parsedQuery.getSql())
                    .isEqualTo("SELECT rm_entity, entity_concept, entity_name, data FROM ehr.comp_data OFFSET 5");
            softly.assertThat(parsedQuery.getLimit()).isNull();
            softly.assertThat(parsedQuery.getOffset()).isEqualTo(5);
        });
    }

    @Test
    void getSqlExistingOffsetDoesNotAddAnother() {
        String sql = "SELECT rm_entity, entity_concept, entity_name, data FROM ehr.comp_data OFFSET 5";
        AqlQueryRequest queryRequest = new AqlQueryRequest(sql, null, null, 10L);

        ParsedQuery parsedQuery = new ParsedQuery(queryRequest);

        assertSoftly(softly -> {
            softly.assertThat(parsedQuery.getSql()).isEqualTo(sql);
            softly.assertThat(parsedQuery.getLimit()).isNull();
            softly.assertThat(parsedQuery.getOffset()).isNull();
        });
    }

    @Test
    void getSqlWithLimitAndOffset() {
        String queryString = "SELECT rm_entity, entity_concept, entity_name, data FROM ehr.comp_data";
        AqlQueryRequest queryRequest = new AqlQueryRequest(queryString, null, 10L, 5L);

        ParsedQuery parsedQuery = new ParsedQuery(queryRequest);

        assertSoftly(softly -> {
            softly.assertThat(parsedQuery.getSql())
                    .isEqualTo("SELECT rm_entity, entity_concept, entity_name, data FROM ehr.comp_data LIMIT 10 OFFSET 5");
            softly.assertThat(parsedQuery.getLimit()).isEqualTo(10);
            softly.assertThat(parsedQuery.getOffset()).isEqualTo(5);
        });
    }

    @Test
    void getSqlWithParameters() {
        // Arrange
        String queryString = """
                SELECT rm_entity, entity_concept, entity_name, data
                FROM ehr.comp_data
                WHERE cd.data ->'V'->'df'->>'cd' = :cd";
                """;
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("cd", "XVDVFE45767");
        AqlQueryRequest queryRequest = new AqlQueryRequest(queryString, parameters, null, null);

        ParsedQuery parsedQuery = new ParsedQuery(queryRequest);

        assertSoftly(softly -> {
            softly.assertThat(parsedQuery.getSql()).isEqualTo(queryString);
            softly.assertThat(parsedQuery.getParameters()).containsKey("cd");
            softly.assertThat(parsedQuery.getParameters().get("cd")).isEqualTo("XVDVFE45767");
        });
    }

    @Test
    void getSqlMissingParameterThrowsException() {
        String queryString = """
                SELECT rm_entity, entity_concept, entity_name, data
                FROM ehr.comp_data
                WHERE cd.data ->'V'->'df'->>'cd' = :cd
                AND cd.entity_idx = :entity_idx";
                """;
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("cd", "XVDVFE45767");
        AqlQueryRequest queryRequest = new AqlQueryRequest(queryString, parameters, null, null);

        assertThatThrownBy(() -> new ParsedQuery(queryRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing value for query parameter: entity_idx");
    }
}