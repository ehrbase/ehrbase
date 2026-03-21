package org.ehrbase.schemagen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ColumnNamerTest {

    @Test
    void simpleId_returnsLowerCase() {
        assertThat(ColumnNamer.toColumnName("Systolic")).isEqualTo("systolic");
    }

    @Test
    void idWithSpaces_replacedWithUnderscore() {
        assertThat(ColumnNamer.toColumnName("Any Event")).isEqualTo("any_event");
    }

    @Test
    void idWithSpecialChars_sanitized() {
        assertThat(ColumnNamer.toColumnName("blood-pressure/reading")).isEqualTo("blood_pressure_reading");
    }

    @Test
    void idWithMultipleUnderscores_collapsed() {
        assertThat(ColumnNamer.toColumnName("some___weird___id")).isEqualTo("some_weird_id");
    }

    @Test
    void leadingTrailingUnderscores_stripped() {
        assertThat(ColumnNamer.toColumnName("_myfield_")).isEqualTo("myfield");
    }

    @Test
    void emptyId_returnsUnnamed() {
        assertThat(ColumnNamer.toColumnName("")).isEqualTo("unnamed");
        assertThat(ColumnNamer.toColumnName(null)).isEqualTo("unnamed");
    }

    @Test
    void sqlReservedWord_getsSuffix() {
        assertThat(ColumnNamer.toColumnName("select")).isEqualTo("select_val");
        assertThat(ColumnNamer.toColumnName("table")).isEqualTo("table_val");
        assertThat(ColumnNamer.toColumnName("order")).isEqualTo("order_val");
        assertThat(ColumnNamer.toColumnName("user")).isEqualTo("user_val");
        assertThat(ColumnNamer.toColumnName("value")).isEqualTo("value_val");
    }

    @Test
    void longId_truncatedWithHash() {
        String longId = "a".repeat(100);
        String result = ColumnNamer.toColumnName(longId);
        assertThat(result.length()).isLessThanOrEqualTo(63);
    }

    @ParameterizedTest
    @CsvSource({
            "OBSERVATION, blood_pressure.v2, obs_blood_pressure_v2",
            "EVALUATION, problem_diagnosis.v1, eval_problem_diagnosis_v1",
            "INSTRUCTION, medication_order.v3, instr_medication_order_v3",
            "ACTION, procedure.v1, act_procedure_v1",
            "ADMIN_ENTRY, discharge.v1, admin_discharge_v1",
            "COMPOSITION, encounter.v1, comp_encounter_v1"
    })
    void toTableName_producesExpectedFormat(String rmType, String templateId, String expected) {
        assertThat(ColumnNamer.toTableName(rmType, templateId)).isEqualTo(expected);
    }

    @Test
    void toTableName_longTemplateId_truncated() {
        String longId = "very_long_template_id_that_exceeds_postgresql_identifier_limit_of_63_chars_easily";
        String result = ColumnNamer.toTableName("OBSERVATION", longId);
        assertThat(result.length()).isLessThanOrEqualTo(63);
    }
}
