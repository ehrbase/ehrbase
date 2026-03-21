package org.ehrbase.schemagen;

import org.ehrbase.schemagen.model.ColumnDescriptor;
import org.ehrbase.schemagen.model.GeneratedSchema;
import org.ehrbase.schemagen.model.TableDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(schema.ddl()).contains("CREATE OR REPLACE VIEW ehr_views.v_obs_blood_pressure_v2");
        assertThat(schema.ddl()).contains("JOIN ehr_system.composition");
        assertThat(schema.ddl()).contains("JOIN ehr_system.ehr");
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
