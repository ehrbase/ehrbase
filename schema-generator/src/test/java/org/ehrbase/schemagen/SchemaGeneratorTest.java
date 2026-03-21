/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.schemagen;

import static org.assertj.core.api.Assertions.assertThat;

import org.ehrbase.schemagen.model.ColumnDescriptor;
import org.ehrbase.schemagen.model.GeneratedSchema;
import org.ehrbase.schemagen.model.TableDescriptor;
import org.junit.jupiter.api.Test;

class SchemaGeneratorTest {

    @Test
    void generateDdl_containsCreateTable() {
        TableDescriptor table = createBloodPressureTable();
        SchemaGenerator generator = new SchemaGenerator();

        GeneratedSchema schema = generator.generate(table);

        assertThat(schema.ddl()).contains("CREATE TABLE IF NOT EXISTS ehr_data.obs_blood_pressure_v2");
    }

    @Test
    void generateDdl_containsUuidV7Default() {
        TableDescriptor table = createBloodPressureTable();
        SchemaGenerator generator = new SchemaGenerator();

        GeneratedSchema schema = generator.generate(table);

        assertThat(schema.ddl()).contains("uuidv7()");
    }

    @Test
    void generateDdl_containsTemporalPrimaryKey() {
        TableDescriptor table = createBloodPressureTable();
        SchemaGenerator generator = new SchemaGenerator();

        GeneratedSchema schema = generator.generate(table);

        assertThat(schema.ddl()).contains("PRIMARY KEY (id, valid_period WITHOUT OVERLAPS)");
    }

    @Test
    void generateDdl_containsTemporalForeignKey() {
        TableDescriptor table = createBloodPressureTable();
        SchemaGenerator generator = new SchemaGenerator();

        GeneratedSchema schema = generator.generate(table);

        assertThat(schema.ddl()).contains("FOREIGN KEY (composition_id, PERIOD valid_period)");
        assertThat(schema.ddl()).contains("REFERENCES ehr_system.composition");
    }

    @Test
    void generateDdl_containsHistoryTable() {
        TableDescriptor table = createBloodPressureTable();
        SchemaGenerator generator = new SchemaGenerator();

        GeneratedSchema schema = generator.generate(table);

        assertThat(schema.ddl()).contains("ehr_data.obs_blood_pressure_v2_history");
        assertThat(schema.ddl()).contains("LIKE ehr_data.obs_blood_pressure_v2 INCLUDING ALL");
        assertThat(schema.ddl()).contains("populated by application code (NOT triggers)");
    }

    @Test
    void generateDdl_containsRlsPolicies() {
        TableDescriptor table = createBloodPressureTable();
        SchemaGenerator generator = new SchemaGenerator();

        GeneratedSchema schema = generator.generate(table);

        assertThat(schema.ddl()).contains("ENABLE ROW LEVEL SECURITY");
        assertThat(schema.ddl()).contains("FORCE ROW LEVEL SECURITY");
        assertThat(schema.ddl()).contains("tenant_policy");
        assertThat(schema.ddl()).contains("current_setting('ehrbase.current_tenant')");
    }

    @Test
    void generateDdl_containsIndexes() {
        TableDescriptor table = createBloodPressureTable();
        SchemaGenerator generator = new SchemaGenerator();

        GeneratedSchema schema = generator.generate(table);

        assertThat(schema.ddl()).contains("CREATE INDEX IF NOT EXISTS");
        assertThat(schema.ddl()).contains("(composition_id)");
        assertThat(schema.ddl()).contains("(ehr_id)");
    }

    @Test
    void generateDdl_containsView() {
        TableDescriptor table = createBloodPressureTable();
        SchemaGenerator generator = new SchemaGenerator();

        GeneratedSchema schema = generator.generate(table);

        assertThat(schema.viewDdl()).contains("CREATE OR REPLACE VIEW ehr_views.v_obs_blood_pressure_v2");
        assertThat(schema.viewDdl()).contains("JOIN ehr_system.composition");
        assertThat(schema.viewDdl()).contains("JOIN ehr_system.ehr");
        assertThat(schema.viewDdl()).contains("uuid_extract_timestamp");
        assertThat(schema.viewDdl()).contains("subject_namespace");
        assertThat(schema.viewDdl()).contains("v_obs_blood_pressure_v2_history");
        assertThat(schema.viewDdl()).contains("is_historical");
        assertThat(schema.viewDdl()).contains("_as_of");
    }

    @Test
    void generateDdl_containsClinicalColumns() {
        TableDescriptor table = createBloodPressureTable();
        SchemaGenerator generator = new SchemaGenerator();

        GeneratedSchema schema = generator.generate(table);

        assertThat(schema.ddl()).contains("systolic_magnitude");
        assertThat(schema.ddl()).contains("DOUBLE PRECISION");
        assertThat(schema.ddl()).contains("systolic_units");
    }

    private TableDescriptor createBloodPressureTable() {
        TableDescriptor table = new TableDescriptor("obs_blood_pressure_v2", "blood_pressure.v2");
        table.addSystemColumns();
        table.addColumn(new ColumnDescriptor("systolic_magnitude", "DOUBLE PRECISION"));
        table.addColumn(new ColumnDescriptor("systolic_units", "TEXT"));
        table.addColumn(new ColumnDescriptor("systolic_precision", "INTEGER"));
        table.addColumn(new ColumnDescriptor("diastolic_magnitude", "DOUBLE PRECISION"));
        table.addColumn(new ColumnDescriptor("diastolic_units", "TEXT"));
        table.addColumn(new ColumnDescriptor("diastolic_precision", "INTEGER"));
        return table;
    }
}
