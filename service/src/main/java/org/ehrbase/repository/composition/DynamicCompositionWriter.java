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
package org.ehrbase.repository.composition;

import com.nedap.archie.rm.composition.Composition;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.repository.schema.ColumnMetadata;
import org.ehrbase.repository.schema.DynamicTable;
import org.ehrbase.repository.schema.TemplateTableMetadata;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.InsertSetStep;
import org.jooq.Record1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes RM Composition clinical data to auto-generated {@code ehr_data} template tables.
 * Uses JOOQ dynamic queries (no codegen for ehr_data schema).
 *
 * <p>All timestamp handling uses PostgreSQL's {@code now()} via column defaults,
 * which is transaction-scoped and consistent across all statements within a
 * {@code @Transactional} method. No Java-level timestamps are passed.
 */
@Component
public class DynamicCompositionWriter {

    private static final Logger log = LoggerFactory.getLogger(DynamicCompositionWriter.class);

    private final DSLContext dsl;

    public DynamicCompositionWriter(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Writes clinical data for a composition into the template table and child tables.
     *
     * @param compositionId  the composition UUID (from ehr_system.composition)
     * @param ehrId          the EHR UUID
     * @param tenantId       current tenant ID
     * @param composition    the RM Composition
     * @param webTemplate    the WebTemplate for column mapping
     * @param metadata       table metadata from TemplateSchemaResolver
     * @return the row ID of the main table entry
     */
    public UUID write(
            UUID compositionId,
            UUID ehrId,
            short tenantId,
            Composition composition,
            WebTemplate webTemplate,
            TemplateTableMetadata metadata) {

        CompositionTableData data = RmTreeWalker.extract(composition, webTemplate, metadata);

        UUID rowId = insertRow(compositionId, ehrId, tenantId, data.mainTableValues(), metadata);
        log.debug("Inserted main table row: {} -> id={}", metadata.fqn(), rowId);

        for (TemplateTableMetadata childMeta : metadata.childTables()) {
            List<Map<String, Object>> childRows =
                    data.childTableValues().getOrDefault(childMeta.tableName(), List.of());
            for (Map<String, Object> childRow : childRows) {
                insertChildRow(rowId, compositionId, ehrId, tenantId, childRow, childMeta);
            }
            if (!childRows.isEmpty()) {
                log.debug("Inserted {} child rows into {}", childRows.size(), childMeta.fqn());
            }
        }

        return rowId;
    }

    /**
     * Deletes clinical data for a composition from template tables.
     * Deletes child tables first (FK constraint order), then main table.
     */
    public void deleteByCompositionId(UUID compositionId, TemplateTableMetadata metadata) {
        for (TemplateTableMetadata child : metadata.childTables()) {
            int deleted = dsl.deleteFrom(DynamicTable.table(child))
                    .where(DynamicTable.field(child, "composition_id", UUID.class)
                            .eq(compositionId))
                    .execute();
            if (deleted > 0) {
                log.debug("Deleted {} rows from child table {}", deleted, child.fqn());
            }
        }

        int deleted = dsl.deleteFrom(DynamicTable.table(metadata))
                .where(DynamicTable.field(metadata, "composition_id", UUID.class)
                        .eq(compositionId))
                .execute();
        log.debug("Deleted {} rows from main table {}", deleted, metadata.fqn());
    }

    /**
     * Archives clinical data to history tables (for versioning).
     * Uses {@link TemplateTableMetadata#storedColumns()} to build an explicit column list,
     * excluding PostgreSQL GENERATED ALWAYS columns that cannot be inserted explicitly.
     * Deletes the archived rows from the current table after copying.
     */
    public void archiveByCompositionId(UUID compositionId, TemplateTableMetadata metadata) {
        for (TemplateTableMetadata child : metadata.childTables()) {
            archiveTable(compositionId, child);
        }
        archiveTable(compositionId, metadata);
    }

    private void archiveTable(UUID compositionId, TemplateTableMetadata meta) {
        String columnList = meta.storedColumns().stream()
                .map(ColumnMetadata::columnName)
                .collect(Collectors.joining(", "));

        dsl.execute(
                "INSERT INTO %s (%s) SELECT %s FROM %s WHERE composition_id = ?"
                        .formatted(meta.historyFqn(), columnList, columnList, meta.fqn()),
                compositionId);

        dsl.deleteFrom(DynamicTable.table(meta))
                .where(DynamicTable.field(meta, "composition_id", UUID.class).eq(compositionId))
                .execute();
    }

    private UUID insertRow(
            UUID compositionId,
            UUID ehrId,
            short tenantId,
            Map<String, Object> values,
            TemplateTableMetadata metadata) {

        InsertSetStep<?> insert = dsl.insertInto(DynamicTable.table(metadata));
        InsertSetMoreStep<?> step = insert.set(DynamicTable.col("composition_id"), (Object) compositionId)
                .set(DynamicTable.col("ehr_id"), (Object) ehrId)
                .set(DynamicTable.col("sys_tenant"), (Object) tenantId)
                .set(DynamicTable.col("sys_version"), (Object) 1);

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            step = step.set(DynamicTable.col(entry.getKey()), entry.getValue());
        }

        Record1<UUID> result =
                step.returningResult(DynamicTable.col("id", UUID.class)).fetchOne();

        return result != null ? result.value1() : null;
    }

    private void insertChildRow(
            UUID parentId,
            UUID compositionId,
            UUID ehrId,
            short tenantId,
            Map<String, Object> values,
            TemplateTableMetadata childMeta) {

        InsertSetStep<?> insert = dsl.insertInto(DynamicTable.table(childMeta));
        InsertSetMoreStep<?> step = insert.set(DynamicTable.col("parent_id"), (Object) parentId)
                .set(DynamicTable.col("composition_id"), (Object) compositionId)
                .set(DynamicTable.col("ehr_id"), (Object) ehrId)
                .set(DynamicTable.col("sys_tenant"), (Object) tenantId)
                .set(DynamicTable.col("sys_version"), (Object) 1);

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            step = step.set(DynamicTable.col(entry.getKey()), entry.getValue());
        }

        step.execute();
    }
}
