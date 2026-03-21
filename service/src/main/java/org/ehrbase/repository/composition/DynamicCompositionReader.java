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
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.repository.schema.DynamicTable;
import org.ehrbase.repository.schema.TemplateTableMetadata;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads RM Composition clinical data from auto-generated {@code ehr_data} template tables
 * and reconstructs Archie RM objects via {@link RmReconstructor}.
 */
@Component
public class DynamicCompositionReader {

    private static final Logger log = LoggerFactory.getLogger(DynamicCompositionReader.class);

    private final DSLContext dsl;

    public DynamicCompositionReader(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Reads the current (latest) version of clinical data for a composition.
     */
    public Optional<Composition> readCurrent(
            UUID compositionId,
            TemplateTableMetadata metadata,
            WebTemplate webTemplate,
            CompositionMetadata compositionMeta) {

        Map<String, Object> mainRow = dsl.select()
                .from(DynamicTable.table(metadata))
                .where(DynamicTable.field(metadata, "composition_id", UUID.class)
                        .eq(compositionId))
                .fetchOneMap();

        if (mainRow == null) {
            return Optional.empty();
        }

        Map<String, List<Map<String, Object>>> childRows = readChildTables(compositionId, metadata);

        return Optional.of(RmReconstructor.reconstruct(mainRow, childRows, webTemplate, compositionMeta));
    }

    /**
     * Reads a specific version of clinical data.
     * Searches both current and _history tables using UNION.
     */
    public Optional<Composition> readVersion(
            UUID compositionId,
            int version,
            TemplateTableMetadata metadata,
            WebTemplate webTemplate,
            CompositionMetadata compositionMeta) {

        // UNION current + history, filter by sys_version
        Map<String, Object> row = dsl.resultQuery(
                        "SELECT * FROM " + metadata.fqn()
                                + " WHERE composition_id = ? AND sys_version = ?"
                                + " UNION ALL"
                                + " SELECT * FROM " + metadata.historyFqn()
                                + " WHERE composition_id = ? AND sys_version = ?"
                                + " LIMIT 1",
                        compositionId,
                        version,
                        compositionId,
                        version)
                .fetchOneMap();

        if (row == null) {
            return Optional.empty();
        }

        Map<String, List<Map<String, Object>>> childRows = readChildTablesForVersion(compositionId, version, metadata);

        return Optional.of(RmReconstructor.reconstruct(row, childRows, webTemplate, compositionMeta));
    }

    /**
     * Reads clinical data at a specific point in time using temporal query.
     * Uses {@code valid_period @> timestamp} on UNION of current + history.
     */
    public Optional<Composition> readAtTime(
            UUID compositionId,
            OffsetDateTime timestamp,
            TemplateTableMetadata metadata,
            WebTemplate webTemplate,
            CompositionMetadata compositionMeta) {

        Map<String, Object> row = dsl.resultQuery(
                        "SELECT * FROM " + metadata.fqn()
                                + " WHERE composition_id = ? AND valid_period @> ?::timestamptz"
                                + " UNION ALL"
                                + " SELECT * FROM " + metadata.historyFqn()
                                + " WHERE composition_id = ? AND valid_period @> ?::timestamptz"
                                + " LIMIT 1",
                        compositionId,
                        timestamp,
                        compositionId,
                        timestamp)
                .fetchOneMap();

        if (row == null) {
            return Optional.empty();
        }

        // For point-in-time, we need child rows that match the same temporal range
        Map<String, List<Map<String, Object>>> childRows = readChildTablesAtTime(compositionId, timestamp, metadata);

        return Optional.of(RmReconstructor.reconstruct(row, childRows, webTemplate, compositionMeta));
    }

    private Map<String, List<Map<String, Object>>> readChildTables(UUID compositionId, TemplateTableMetadata metadata) {

        Map<String, List<Map<String, Object>>> childRows = new HashMap<>();
        for (TemplateTableMetadata child : metadata.childTables()) {
            Result<Record> rows = dsl.select()
                    .from(DynamicTable.table(child))
                    .where(DynamicTable.field(child, "composition_id", UUID.class)
                            .eq(compositionId))
                    .fetch();

            if (!rows.isEmpty()) {
                childRows.put(
                        child.tableName(), rows.stream().map(Record::intoMap).toList());
            }
        }
        return childRows;
    }

    private Map<String, List<Map<String, Object>>> readChildTablesForVersion(
            UUID compositionId, int version, TemplateTableMetadata metadata) {

        Map<String, List<Map<String, Object>>> childRows = new HashMap<>();
        for (TemplateTableMetadata child : metadata.childTables()) {
            Result<Record> rows = dsl.resultQuery(
                            "SELECT * FROM " + child.fqn()
                                    + " WHERE composition_id = ? AND sys_version = ?"
                                    + " UNION ALL"
                                    + " SELECT * FROM " + child.historyFqn()
                                    + " WHERE composition_id = ? AND sys_version = ?",
                            compositionId,
                            version,
                            compositionId,
                            version)
                    .fetch();

            if (!rows.isEmpty()) {
                childRows.put(
                        child.tableName(), rows.stream().map(Record::intoMap).toList());
            }
        }
        return childRows;
    }

    private Map<String, List<Map<String, Object>>> readChildTablesAtTime(
            UUID compositionId, OffsetDateTime timestamp, TemplateTableMetadata metadata) {

        Map<String, List<Map<String, Object>>> childRows = new HashMap<>();
        for (TemplateTableMetadata child : metadata.childTables()) {
            Result<Record> rows = dsl.resultQuery(
                            "SELECT * FROM " + child.fqn()
                                    + " WHERE composition_id = ? AND valid_period @> ?::timestamptz"
                                    + " UNION ALL"
                                    + " SELECT * FROM " + child.historyFqn()
                                    + " WHERE composition_id = ? AND valid_period @> ?::timestamptz",
                            compositionId,
                            timestamp,
                            compositionId,
                            timestamp)
                    .fetch();

            if (!rows.isEmpty()) {
                childRows.put(
                        child.tableName(), rows.stream().map(Record::intoMap).toList());
            }
        }
        return childRows;
    }
}
