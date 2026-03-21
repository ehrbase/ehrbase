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
package org.ehrbase.service;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.ehrbase.repository.schema.ColumnMetadata;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the {@code ehr_system.view_catalog} — tracks all views in {@code ehr_views} schema
 * with their types, column metadata, and refresh schedules.
 *
 * <p>Also responsible for generating cross-template patient summary views.
 */
@Service
public class ViewCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ViewCatalogService.class);

    private static final org.jooq.Table<?> VIEW_CATALOG = table(name("ehr_system", "view_catalog"));
    private static final org.jooq.Table<?> SCHEMA_REGISTRY = table(name("ehr_system", "schema_registry"));
    private static final org.jooq.Table<?> TEMPLATE = table(name("ehr_system", "template"));

    private final DSLContext dsl;

    public ViewCatalogService(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Registers template views (current + history + as_of function) in the catalog.
     * Called after SchemaGenerator DDL is executed.
     */
    @Transactional
    public void registerTemplateViews(
            String tableName, String templateId, List<ColumnMetadata> columns, short tenantId) {
        JSONB columnJson = buildColumnMetadataJson(columns);

        upsertCatalogEntry(
                "v_" + tableName,
                "template",
                templateId,
                "auto",
                "Current data view for template " + templateId,
                columnJson,
                false,
                null,
                tenantId);
        upsertCatalogEntry(
                "v_" + tableName + "_history",
                "template_history",
                templateId,
                "auto",
                "Version history view for template " + templateId,
                columnJson,
                false,
                null,
                tenantId);
        upsertCatalogEntry(
                tableName + "_as_of",
                "function",
                templateId,
                "auto",
                "Point-in-time function for template " + templateId,
                columnJson,
                false,
                null,
                tenantId);

        log.debug("Registered template views for '{}' in catalog (tenant={})", templateId, tenantId);
    }

    /**
     * Lists all views, optionally filtered by type.
     */
    public List<ViewCatalogEntry> listViews(@Nullable String viewType, short tenantId) {
        var query = dsl.select().from(VIEW_CATALOG);

        if (viewType != null) {
            return query.where(field(name("view_type"), String.class).eq(viewType)).fetch().stream()
                    .map(this::mapToEntry)
                    .toList();
        }

        return query.fetch().stream().map(this::mapToEntry).toList();
    }

    /**
     * Gets a single view's metadata.
     */
    public ViewCatalogEntry getView(String viewName, short tenantId) {
        Record row = dsl.select()
                .from(VIEW_CATALOG)
                .where(field(name("view_name"), String.class).eq(viewName))
                .fetchOne();
        return row != null ? mapToEntry(row) : null;
    }

    /**
     * Removes auto-generated views for a template (on template deletion).
     */
    @Transactional
    public void removeTemplateViews(String templateId, short tenantId) {
        int deleted = dsl.deleteFrom(VIEW_CATALOG)
                .where(field(name("template_id"), String.class).eq(templateId))
                .and(field(name("source"), String.class).eq("auto"))
                .execute();
        log.debug("Removed {} catalog entries for template '{}'", deleted, templateId);
    }

    /**
     * Generates a cross-template patient summary view using LATERAL JOINs.
     * Queries schema_registry for all active template tables, selects primary value columns,
     * and builds a single view with one row per patient.
     */
    @Transactional
    public void generatePatientSummaryView(short tenantId) {
        var templates = dsl.select(
                        field(name("sr", "table_name"), String.class), field(name("t", "template_id"), String.class))
                .from(SCHEMA_REGISTRY.as("sr"))
                .join(TEMPLATE.as("t"))
                .on(field(name("sr", "template_id"), UUID.class).eq(field(name("t", "id"), UUID.class)))
                .where(field(name("sr", "status"), String.class).eq("active"))
                .fetch();

        if (templates.isEmpty()) {
            log.debug("No templates registered — skipping patient summary view generation");
            return;
        }

        var sb = new StringBuilder();
        sb.append("CREATE OR REPLACE VIEW ehr_views.v_patient_summary AS\n");
        sb.append("SELECT\n");
        sb.append("    e.id AS ehr_id,\n");
        sb.append("    e.subject_id,\n");
        sb.append("    e.subject_namespace,\n");
        sb.append("    e.creation_date AS ehr_created");

        var lateralJoins = new StringBuilder();
        int aliasCounter = 0;

        for (Record template : templates) {
            String tableName = template.get(field(name("sr", "table_name"), String.class));
            String templateId = template.get(field(name("t", "template_id"), String.class));
            String alias = "t" + aliasCounter++;
            String viewName = "ehr_views.v_" + tableName;

            sb.append(",\n    ")
                    .append(alias)
                    .append(".committed_at AS ")
                    .append(alias)
                    .append("_last_time");

            lateralJoins.append("\nLEFT JOIN LATERAL (\n");
            lateralJoins.append("    SELECT v.committed_at\n");
            lateralJoins.append("    FROM ").append(viewName).append(" v\n");
            lateralJoins.append("    WHERE v.ehr_id = e.id\n");
            lateralJoins.append("    ORDER BY v.committed_at DESC\n");
            lateralJoins.append("    LIMIT 1\n");
            lateralJoins.append(") ").append(alias).append(" ON true");
        }

        sb.append("\nFROM ehr_system.ehr e");
        sb.append(lateralJoins);
        sb.append(";\n");

        dsl.execute(sb.toString());

        upsertCatalogEntry(
                "v_patient_summary",
                "cross_template",
                null,
                "auto",
                "Cross-template patient summary with latest data per template",
                null,
                false,
                null,
                tenantId);

        log.info("Generated patient summary view with {} template joins", templates.size());
    }

    private void upsertCatalogEntry(
            String viewName,
            String viewType,
            @Nullable String templateId,
            String source,
            String description,
            @Nullable JSONB columnMetadata,
            boolean isMaterialized,
            @Nullable String refreshSchedule,
            short tenantId) {
        dsl.insertInto(VIEW_CATALOG)
                .set(field(name("view_name"), String.class), viewName)
                .set(field(name("view_schema"), String.class), "ehr_views")
                .set(field(name("view_type"), String.class), viewType)
                .set(field(name("template_id"), String.class), templateId)
                .set(field(name("source"), String.class), source)
                .set(field(name("description"), String.class), description)
                .set(field(name("column_metadata"), JSONB.class), columnMetadata)
                .set(field(name("is_materialized"), Boolean.class), isMaterialized)
                .set(field(name("refresh_schedule"), String.class), refreshSchedule)
                .set(field(name("sys_tenant"), Short.class), tenantId)
                .onConflict(field(name("view_name")), field(name("view_schema")), field(name("sys_tenant")))
                .doUpdate()
                .set(field(name("description"), String.class), description)
                .set(field(name("column_metadata"), JSONB.class), columnMetadata)
                .set(field(name("updated_at")), org.jooq.impl.DSL.currentOffsetDateTime())
                .execute();
    }

    private JSONB buildColumnMetadataJson(List<ColumnMetadata> columns) {
        String json = columns.stream()
                .map(c -> "{\"name\":\"%s\",\"type\":\"%s\",\"nullable\":%s}"
                        .formatted(c.columnName(), c.pgType(), c.nullable()))
                .collect(Collectors.joining(",", "[", "]"));
        return JSONB.jsonb(json);
    }

    private ViewCatalogEntry mapToEntry(Record row) {
        return new ViewCatalogEntry(
                row.get(field(name("id"), UUID.class)),
                row.get(field(name("view_name"), String.class)),
                row.get(field(name("view_schema"), String.class)),
                row.get(field(name("view_type"), String.class)),
                row.get(field(name("template_id"), String.class)),
                row.get(field(name("source"), String.class)),
                row.get(field(name("description"), String.class)),
                row.get(field(name("is_materialized"), Boolean.class)));
    }

    public record ViewCatalogEntry(
            UUID id,
            String viewName,
            String viewSchema,
            String viewType,
            String templateId,
            String source,
            String description,
            boolean isMaterialized) {}
}
