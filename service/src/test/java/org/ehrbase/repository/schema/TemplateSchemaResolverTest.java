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
package org.ehrbase.repository.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests for TemplateSchemaResolver — resolves table/column metadata from ehr_data schema.
 */
class TemplateSchemaResolverTest {

    @Test
    void constructorAcceptsDslContext() {
        DSLContext mockDsl = mock(DSLContext.class, Mockito.RETURNS_DEEP_STUBS);
        var resolver = new TemplateSchemaResolver(mockDsl);
        assertThat(resolver).isNotNull();
    }

    @Test
    void templateTableMetadataRecord() {
        var meta = new TemplateTableMetadata(
                "ehr_data",
                "blood_pressure",
                "blood_pressure_history",
                java.util.List.of(
                        new ColumnMetadata(
                                "systolic_magnitude", "numeric", false, false, false, "/data/systolic", "DV_QUANTITY"),
                        new ColumnMetadata(
                                "systolic_units", "text", false, false, false, "/data/systolic", "DV_QUANTITY"),
                        new ColumnMetadata(
                                "diastolic_magnitude", "numeric", true, false, false, "/data/diastolic", "DV_QUANTITY")),
                java.util.List.of(),
                null);

        assertThat(meta.tableName()).isEqualTo("blood_pressure");
        assertThat(meta.columns()).hasSize(3);
        assertThat(meta.columns().get(0).columnName()).isEqualTo("systolic_magnitude");
        assertThat(meta.columns().get(0).pgType()).isEqualTo("numeric");
        assertThat(meta.columns().get(0).nullable()).isFalse();
        assertThat(meta.columns().get(0).isSystemColumn()).isFalse();
        assertThat(meta.columns().get(0).generated()).isFalse();
        assertThat(meta.columns().get(2).nullable()).isTrue();
    }

    @Test
    void columnMetadataRecord() {
        var col =
                new ColumnMetadata("blood_pressure_systolic", "numeric", false, false, false, "/data/bp", "DV_QUANTITY");
        assertThat(col.columnName()).isEqualTo("blood_pressure_systolic");
        assertThat(col.pgType()).isEqualTo("numeric");
        assertThat(col.nullable()).isFalse();
        assertThat(col.isSystemColumn()).isFalse();
        assertThat(col.generated()).isFalse();
        assertThat(col.rmPath()).isEqualTo("/data/bp");
        assertThat(col.rmType()).isEqualTo("DV_QUANTITY");
    }

    @Test
    void columnMetadataFromInformationSchema() {
        var col = ColumnMetadata.fromInformationSchema("ehr_id", "uuid", false, false);
        assertThat(col.columnName()).isEqualTo("ehr_id");
        assertThat(col.isSystemColumn()).isTrue();
        assertThat(col.generated()).isFalse();
        assertThat(col.rmPath()).isNull();
    }

    @Test
    void generatedColumnDetection() {
        var generated = ColumnMetadata.fromInformationSchema("search_vector", "tsvector", true, true);
        assertThat(generated.generated()).isTrue();
        assertThat(generated.isSystemColumn()).isFalse();

        var stored = ColumnMetadata.fromInformationSchema("name_val", "text", true, false);
        assertThat(stored.generated()).isFalse();
    }

    @Test
    void storedColumnsExcludesGenerated() {
        var meta = new TemplateTableMetadata(
                "ehr_data",
                "test_table",
                "test_table_history",
                java.util.List.of(
                        new ColumnMetadata("id", "uuid", false, true, false, null, null),
                        new ColumnMetadata("name_val", "text", true, false, false, "/name", "DV_TEXT"),
                        new ColumnMetadata("name_val_search", "text", true, false, true, null, null),
                        new ColumnMetadata("search_vector", "tsvector", true, false, true, null, null)),
                java.util.List.of(),
                null);

        assertThat(meta.columns()).hasSize(4);
        assertThat(meta.storedColumns()).hasSize(2);
        assertThat(meta.storedColumns().stream().map(ColumnMetadata::columnName).toList())
                .containsExactly("id", "name_val");
    }

    @Test
    void systemColumnDetection() {
        assertThat(ColumnMetadata.fromInformationSchema("id", "uuid", false, false).isSystemColumn())
                .isTrue();
        assertThat(ColumnMetadata.fromInformationSchema("composition_id", "uuid", false, false)
                        .isSystemColumn())
                .isTrue();
        assertThat(ColumnMetadata.fromInformationSchema("sys_version", "int", false, false)
                        .isSystemColumn())
                .isTrue();
        assertThat(ColumnMetadata.fromInformationSchema("systolic_magnitude", "numeric", true, false)
                        .isSystemColumn())
                .isFalse();
    }
}
