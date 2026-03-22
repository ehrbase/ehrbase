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

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.schemagen.model.TableDescriptor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record3;
import org.jooq.Record4;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves a template_id to full table metadata needed for dynamic queries.
 *
 * <p>Two resolution paths:
 * <ul>
 *   <li><b>Fast path</b> (post-upload): Convert {@link TableDescriptor} directly via
 *       {@link #fromTableDescriptor(TableDescriptor)}</li>
 *   <li><b>Cold path</b> (app restart): Query {@code schema_registry} + {@code information_schema.columns}
 *       via {@link #resolve(String)}</li>
 * </ul>
 *
 * <p>Caching: in-memory ConcurrentHashMap, invalidated on template upload.
 */
@Component
public class TemplateSchemaResolver {

    private static final Logger log = LoggerFactory.getLogger(TemplateSchemaResolver.class);

    private static final org.jooq.Table<?> SCHEMA_REGISTRY = table(name("ehr_system", "schema_registry"));
    private static final org.jooq.Table<?> TEMPLATE = table(name("ehr_system", "template"));
    private static final org.jooq.Table<?> INFO_COLUMNS = table(name("information_schema", "columns"));

    private final DSLContext dsl;
    private final Map<String, TemplateTableMetadata> cache = new ConcurrentHashMap<>();

    public TemplateSchemaResolver(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Fast path: convert a TableDescriptor directly to TemplateTableMetadata.
     * Called after template upload when the TableDescriptor is still in memory.
     * Also populates the cache.
     */
    public TemplateTableMetadata fromTableDescriptor(TableDescriptor descriptor) {
        TemplateTableMetadata meta = convertDescriptor(descriptor);
        cache.put(descriptor.getTemplateId(), meta);
        log.debug("Cached table metadata for template '{}' -> {}", descriptor.getTemplateId(), meta.fqn());
        return meta;
    }

    /**
     * Cold path: resolve a template_id string to table metadata by querying the database.
     * Used on app restart when TableDescriptor is not in memory.
     */
    public TemplateTableMetadata resolve(String templateId) {
        TemplateTableMetadata cached = cache.get(templateId);
        if (cached != null) {
            return cached;
        }

        // Look up schema_registry via template table
        Record registryRecord = dsl.select(
                        field(name("sr", "table_name"), String.class), field(name("sr", "schema_name"), String.class))
                .from(SCHEMA_REGISTRY.as("sr"))
                .join(TEMPLATE.as("t"))
                .on(field(name("sr", "template_id"), UUID.class).eq(field(name("t", "id"), UUID.class)))
                .where(field(name("t", "template_id"), String.class).eq(templateId))
                .and(field(name("sr", "status"), String.class).eq("active"))
                .fetchOne();

        if (registryRecord == null) {
            throw new ObjectNotFoundException(
                    "schema_registry", "No active schema registry entry for template '%s'".formatted(templateId));
        }

        String tableName = registryRecord.get(field(name("sr", "table_name"), String.class));
        String schemaName = registryRecord.get(field(name("sr", "schema_name"), String.class));

        TemplateTableMetadata meta = buildFromInformationSchema(schemaName, tableName);
        cache.put(templateId, meta);
        log.debug("Resolved and cached table metadata for template '{}' -> {}", templateId, meta.fqn());
        return meta;
    }

    /**
     * Resolve by template UUID (internal ID from ehr_system.template).
     */
    public TemplateTableMetadata resolveByUuid(UUID templateUuid) {
        Record registryRecord = dsl.select(
                        field(name("sr", "table_name"), String.class),
                        field(name("sr", "schema_name"), String.class),
                        field(name("t", "template_id"), String.class))
                .from(SCHEMA_REGISTRY.as("sr"))
                .join(TEMPLATE.as("t"))
                .on(field(name("sr", "template_id"), UUID.class).eq(field(name("t", "id"), UUID.class)))
                .where(field(name("t", "id"), UUID.class).eq(templateUuid))
                .and(field(name("sr", "status"), String.class).eq("active"))
                .fetchOne();

        if (registryRecord == null) {
            throw new ObjectNotFoundException(
                    "schema_registry",
                    "No active schema registry entry for template UUID '%s'".formatted(templateUuid));
        }

        String templateId = registryRecord.get(field(name("t", "template_id"), String.class));
        String tableName = registryRecord.get(field(name("sr", "table_name"), String.class));
        String schemaName = registryRecord.get(field(name("sr", "schema_name"), String.class));

        TemplateTableMetadata cached = cache.get(templateId);
        if (cached != null) {
            return cached;
        }

        TemplateTableMetadata meta = buildFromInformationSchema(schemaName, tableName);
        cache.put(templateId, meta);
        return meta;
    }

    /**
     * Invalidate cached metadata for a template. Called on template re-upload.
     */
    public void invalidate(String templateId) {
        cache.remove(templateId);
        log.debug("Invalidated cache for template '{}'", templateId);
    }

    /**
     * Clear all cached metadata.
     */
    public void invalidateAll() {
        cache.clear();
    }

    private TemplateTableMetadata buildFromInformationSchema(String schemaName, String tableName) {
        List<ColumnMetadata> columns = queryColumns(schemaName, tableName);

        // Find child tables: tables in same schema with name starting with "{tableName}_"
        List<TemplateTableMetadata> childTables = findChildTables(schemaName, tableName);

        return new TemplateTableMetadata(schemaName, tableName, tableName + "_history", columns, childTables, null);
    }

    private List<ColumnMetadata> queryColumns(String schemaName, String tableName) {
        Result<Record4<String, String, String, String>> rows = dsl.select(
                        field(name("column_name"), String.class),
                        field(name("data_type"), String.class),
                        field(name("is_nullable"), String.class),
                        field(name("is_generated"), String.class))
                .from(INFO_COLUMNS)
                .where(field(name("table_schema"), String.class).eq(schemaName))
                .and(field(name("table_name"), String.class).eq(tableName))
                .orderBy(field(name("ordinal_position")))
                .fetch();

        return rows.stream()
                .map(r -> ColumnMetadata.fromInformationSchema(
                        r.value1(), r.value2(), "YES".equals(r.value3()), !"NEVER".equals(r.value4())))
                .toList();
    }

    private List<TemplateTableMetadata> findChildTables(String schemaName, String parentTableName) {
        // Child tables have names like: {parentTableName}_{cluster_name}
        // They exist in schema_registry or can be detected by naming convention
        String childPrefix = parentTableName + "_";

        Result<Record3<String, String, String>> childTableRows = dsl.select(
                        field(name("table_name"), String.class),
                        field(name("table_schema"), String.class),
                        field(name("table_name"), String.class))
                .from(table(name("information_schema", "tables")))
                .where(field(name("table_schema"), String.class).eq(schemaName))
                .and(field(name("table_name"), String.class).like(childPrefix + "%"))
                .and(field(name("table_name"), String.class).notLike("%_history"))
                .fetch();

        List<TemplateTableMetadata> children = new ArrayList<>();
        for (Record3<String, String, String> row : childTableRows) {
            String childTableName = row.value1();
            List<ColumnMetadata> childColumns = queryColumns(schemaName, childTableName);
            children.add(new TemplateTableMetadata(
                    schemaName, childTableName, childTableName + "_history", childColumns, List.of(), parentTableName));
        }
        return children;
    }

    private TemplateTableMetadata convertDescriptor(TableDescriptor descriptor) {
        List<ColumnMetadata> columns = descriptor.getColumns().stream()
                .map(ColumnMetadata::fromColumnDescriptor)
                .toList();

        List<TemplateTableMetadata> childTables = descriptor.getChildTables().stream()
                .map(this::convertDescriptor)
                .toList();

        return new TemplateTableMetadata(
                descriptor.getSchema(),
                descriptor.getTableName(),
                descriptor.getTableName() + "_history",
                columns,
                childTables,
                descriptor.getParentTableName());
    }
}
