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
package org.ehrbase.service.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for PgToGraphQlTypeMapper — all PG type mappings, camelCase/PascalCase conversions.
 */
class PgToGraphQlTypeMapperTest {

    // ==================== Type Mapping ====================

    @ParameterizedTest
    @CsvSource({
        "UUID DEFAULT uuidv7(),false,ID!",
        "UUID DEFAULT uuidv7(),true,ID",
        "TEXT,false,String!",
        "TEXT,true,String",
        "DOUBLE PRECISION,false,Float!",
        "DOUBLE PRECISION,true,Float",
        "INTEGER,false,Int!",
        "BIGINT,false,Long!",
        "BIGINT,true,Long",
        "BOOLEAN,false,Boolean!",
        "BOOLEAN,true,Boolean",
        "SMALLINT,false,Int!",
        "TIMESTAMPTZ,false,DateTime!",
        "TIMESTAMP WITH TIME ZONE,true,DateTime",
        "TSTZRANGE,false,DateTimeRange!",
        "JSONB,false,JSON!",
        "JSONB,true,JSON",
    })
    void mapPgTypeToGraphQl(String pgType, boolean nullable, String expected) {
        assertThat(PgToGraphQlTypeMapper.map(pgType, nullable)).isEqualTo(expected);
    }

    @Test
    void tsvectorReturnsNull() {
        assertThat(PgToGraphQlTypeMapper.map("TSVECTOR", false)).isNull();
        assertThat(PgToGraphQlTypeMapper.map("TSVECTOR GENERATED ALWAYS AS", true))
                .isNull();
    }

    @Test
    void nullPgTypeDefaultsToString() {
        assertThat(PgToGraphQlTypeMapper.map(null, true)).isEqualTo("String");
        assertThat(PgToGraphQlTypeMapper.map(null, false)).isEqualTo("String!");
    }

    @Test
    void unknownPgTypeDefaultsToString() {
        assertThat(PgToGraphQlTypeMapper.map("CUSTOM_TYPE", false)).isEqualTo("String!");
    }

    // ==================== camelCase Conversion ====================

    @ParameterizedTest
    @CsvSource({
        "blood_pressure_systolic,bloodPressureSystolic",
        "ehr_id,ehrId",
        "sys_version,sysVersion",
        "single,single",
        "already_camel,alreadyCamel",
    })
    void toCamelCase(String input, String expected) {
        assertThat(PgToGraphQlTypeMapper.toCamelCase(input)).isEqualTo(expected);
    }

    @Test
    void toCamelCaseNullAndEmpty() {
        assertThat(PgToGraphQlTypeMapper.toCamelCase(null)).isNull();
        assertThat(PgToGraphQlTypeMapper.toCamelCase("")).isEmpty();
    }

    // ==================== PascalCase Type Name ====================

    @ParameterizedTest
    @CsvSource({
        "v_blood_pressure,BloodPressure",
        "v_lab_results_panel,LabResultsPanel",
        "some_table,SomeTable",
        "v_single,Single",
    })
    void toTypeName(String viewName, String expected) {
        assertThat(PgToGraphQlTypeMapper.toTypeName(viewName)).isEqualTo(expected);
    }

    // ==================== Query Field Name ====================

    @ParameterizedTest
    @CsvSource({
        "v_blood_pressure,bloodPressures",
        "v_lab_results,labResultss",
        "v_medication,medications",
    })
    void toQueryFieldName(String viewName, String expected) {
        assertThat(PgToGraphQlTypeMapper.toQueryFieldName(viewName)).isEqualTo(expected);
    }

    // ==================== Filter Input Type Name ====================

    @Test
    void filterInputTypeName() {
        assertThat(PgToGraphQlTypeMapper.filterInputTypeName("ID!")).isEqualTo("IDFilter");
        assertThat(PgToGraphQlTypeMapper.filterInputTypeName("String")).isEqualTo("StringFilter");
        assertThat(PgToGraphQlTypeMapper.filterInputTypeName("Int!")).isEqualTo("IntFilter");
        assertThat(PgToGraphQlTypeMapper.filterInputTypeName("Float")).isEqualTo("FloatFilter");
        assertThat(PgToGraphQlTypeMapper.filterInputTypeName("Boolean!")).isEqualTo("BooleanFilter");
        assertThat(PgToGraphQlTypeMapper.filterInputTypeName("DateTime")).isEqualTo("DateTimeFilter");
    }
}
