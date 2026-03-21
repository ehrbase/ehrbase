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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.ehrbase.schemagen.model.ColumnDescriptor;
import org.ehrbase.schemagen.model.GeneratedSchema;
import org.ehrbase.schemagen.model.TableDescriptor;

/**
 * Generates PostgreSQL 18 DDL from a TableDescriptor.
 * Produces: CREATE TABLE, _history table, RLS policies, indexes, views (current + history),
 * point-in-time functions, and full-text search infrastructure.
 */
public class SchemaGenerator {

    /**
     * Generates complete DDL for a template's tables, history, RLS, indexes, views, and search.
     */
    public GeneratedSchema generate(TableDescriptor table) {
        var ddl = new StringBuilder();
        var viewDdl = new StringBuilder();
        var searchDdl = new StringBuilder();

        ddl.append(generateCreateTable(table));
        ddl.append(generateHistoryTable(table));
        ddl.append(generateRlsPolicies(table));
        ddl.append(generateIndexes(table));

        viewDdl.append(generateView(table));
        viewDdl.append(generateHistoryView(table));
        viewDdl.append(generateAsOfFunction(table));

        searchDdl.append(generateFullTextSearch(table));

        for (TableDescriptor child : table.getChildTables()) {
            GeneratedSchema childSchema = generate(child);
            ddl.append(childSchema.ddl());
            viewDdl.append(childSchema.viewDdl());
            searchDdl.append(childSchema.searchDdl());
        }

        return new GeneratedSchema(table.getTableName(), ddl.toString(), viewDdl.toString(), searchDdl.toString());
    }

    private String generateCreateTable(TableDescriptor table) {
        var sb = new StringBuilder();
        String fqn = table.getFullyQualifiedName();

        sb.append("CREATE TABLE IF NOT EXISTS ").append(fqn).append(" (\n");

        for (int i = 0; i < table.getColumns().size(); i++) {
            ColumnDescriptor col = table.getColumns().get(i);
            sb.append("    ").append(quoteIdentifier(col.name())).append(" ").append(col.pgType());
            if (i < table.getColumns().size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        // PG18 virtual generated columns for case-insensitive search on key TEXT fields
        for (ColumnDescriptor col : table.getColumns()) {
            if ("TEXT".equals(col.pgType()) && isSearchableColumn(col.name())) {
                sb.append("    , ")
                        .append(quoteIdentifier(col.name() + "_search"))
                        .append(" TEXT GENERATED ALWAYS AS (casefold(")
                        .append(quoteIdentifier(col.name()))
                        .append(")) VIRTUAL\n");
            }
        }

        // PG18 temporal primary key
        sb.append("    , PRIMARY KEY (id, valid_period WITHOUT OVERLAPS)\n");

        // PG18 temporal foreign key to composition (skip for child tables with parent_id)
        if (table.getParentTableName() == null) {
            sb.append("    , FOREIGN KEY (composition_id, PERIOD valid_period)\n");
            sb.append("        REFERENCES ehr_system.composition (id, PERIOD valid_period)\n");
        } else {
            sb.append("    , FOREIGN KEY (parent_id) REFERENCES ");
            sb.append(table.getSchema())
                    .append(".")
                    .append(table.getParentTableName())
                    .append(" (id)\n");
        }

        sb.append(");\n\n");

        return sb.toString();
    }

    private String generateHistoryTable(TableDescriptor table) {
        String fqn = table.getFullyQualifiedName();
        return "-- History table: populated by application code (NOT triggers)\n" + "CREATE TABLE IF NOT EXISTS " + fqn
                + "_history (\n" + "    LIKE " + fqn + " INCLUDING ALL\n" + ");\n\n";
    }

    private String generateRlsPolicies(TableDescriptor table) {
        String fqn = table.getFullyQualifiedName();
        return "ALTER TABLE " + fqn + " ENABLE ROW LEVEL SECURITY;\n" + "ALTER TABLE " + fqn
                + " FORCE ROW LEVEL SECURITY;\n" + "CREATE POLICY tenant_policy ON " + fqn + "\n"
                + "    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);\n\n"
                + "ALTER TABLE " + fqn + "_history ENABLE ROW LEVEL SECURITY;\n" + "ALTER TABLE " + fqn
                + "_history FORCE ROW LEVEL SECURITY;\n" + "CREATE POLICY tenant_policy ON " + fqn + "_history\n"
                + "    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);\n\n";
    }

    private String generateIndexes(TableDescriptor table) {
        String fqn = table.getFullyQualifiedName();
        String shortName = table.getTableName();

        var sb = new StringBuilder();

        sb.append("CREATE INDEX IF NOT EXISTS idx_").append(shortName).append("_composition\n");
        sb.append("    ON ").append(fqn).append(" (composition_id);\n");

        sb.append("CREATE INDEX IF NOT EXISTS idx_").append(shortName).append("_ehr\n");
        sb.append("    ON ").append(fqn).append(" (ehr_id);\n");

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Generates the current-data view joining template data with composition metadata and EHR subject info.
     * For child tables, joins via parent view instead of directly to composition.
     */
    private String generateView(TableDescriptor table) {
        String dataTable = table.getFullyQualifiedName();
        String viewName = "ehr_views.v_" + table.getTableName();

        if (table.getParentTableName() != null) {
            String parentViewName = "ehr_views.v_" + table.getParentTableName();
            return "CREATE OR REPLACE VIEW " + viewName + " AS\n"
                    + "SELECT ct.*,\n"
                    + "    pv.composition_id AS parent_composition_id,\n"
                    + "    pv.subject_id,\n"
                    + "    pv.subject_namespace,\n"
                    + "    pv.committed_at,\n"
                    + "    pv.committer_name,\n"
                    + "    pv.composition_version,\n"
                    + "    pv.change_type,\n"
                    + "    uuid_extract_timestamp(ct.id) AS created_at\n"
                    + "FROM " + dataTable + " ct\n"
                    + "JOIN " + parentViewName + " pv ON ct.parent_id = pv.id;\n\n";
        }

        return "CREATE OR REPLACE VIEW " + viewName + " AS\n"
                + "SELECT t.*,\n"
                + "    e.subject_id,\n"
                + "    e.subject_namespace,\n"
                + "    c.committed_at,\n"
                + "    c.committer_name,\n"
                + "    c.sys_version AS composition_version,\n"
                + "    c.change_type,\n"
                + "    uuid_extract_timestamp(t.id) AS created_at\n"
                + "FROM " + dataTable + " t\n"
                + "JOIN ehr_system.composition c ON t.composition_id = c.id\n"
                + "JOIN ehr_system.ehr e ON t.ehr_id = e.id;\n\n";
    }

    /**
     * Generates a history view that UNIONs current + _history tables with an is_historical flag.
     * History rows join to composition_history (matching by sys_version) for correct metadata.
     */
    private String generateHistoryView(TableDescriptor table) {
        if (table.getParentTableName() != null) {
            return "";
        }

        String dataTable = table.getFullyQualifiedName();
        String historyTable = dataTable + "_history";
        String viewName = "ehr_views.v_" + table.getTableName() + "_history";

        return "CREATE OR REPLACE VIEW " + viewName + " AS\n"
                + "SELECT t.*, false AS is_historical,\n"
                + "    e.subject_id, e.subject_namespace,\n"
                + "    c.committed_at, c.committer_name,\n"
                + "    c.sys_version AS composition_version, c.change_type,\n"
                + "    uuid_extract_timestamp(t.id) AS created_at\n"
                + "FROM " + dataTable + " t\n"
                + "JOIN ehr_system.composition c ON t.composition_id = c.id\n"
                + "JOIN ehr_system.ehr e ON t.ehr_id = e.id\n"
                + "UNION ALL\n"
                + "SELECT h.*, true AS is_historical,\n"
                + "    e.subject_id, e.subject_namespace,\n"
                + "    ch.committed_at, ch.committer_name,\n"
                + "    ch.sys_version AS composition_version, ch.change_type,\n"
                + "    uuid_extract_timestamp(h.id) AS created_at\n"
                + "FROM " + historyTable + " h\n"
                + "JOIN ehr_system.composition_history ch ON h.composition_id = ch.id"
                + " AND h.sys_version = ch.sys_version\n"
                + "JOIN ehr_system.ehr e ON h.ehr_id = e.id;\n\n";
    }

    /**
     * Generates a point-in-time SQL function that returns rows valid at a given timestamp.
     * Uses the temporal range containment operator {@code @>} on valid_period.
     */
    private String generateAsOfFunction(TableDescriptor table) {
        if (table.getParentTableName() != null) {
            return "";
        }

        String tableName = table.getTableName();
        String historyViewName = "ehr_views.v_" + tableName + "_history";
        String functionName = "ehr_views." + tableName + "_as_of";

        return "CREATE OR REPLACE FUNCTION " + functionName + "(ts TIMESTAMPTZ)\n"
                + "RETURNS SETOF " + historyViewName + "\n"
                + "LANGUAGE sql STABLE AS $$\n"
                + "    SELECT * FROM " + historyViewName + "\n"
                + "    WHERE valid_period @> ts;\n"
                + "$$;\n\n";
    }

    /**
     * Generates full-text search infrastructure: tsvector STORED column + GIN index + pg_trgm index.
     * Uses STORED (not VIRTUAL) because GIN indexes require physical storage.
     */
    private String generateFullTextSearch(TableDescriptor table) {
        List<String> textColumns = new ArrayList<>();
        for (ColumnDescriptor col : table.getColumns()) {
            if ("TEXT".equals(col.pgType()) && isSearchableColumn(col.name())) {
                textColumns.add(col.name());
            }
        }

        if (textColumns.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        String fqn = table.getFullyQualifiedName();
        String shortName = table.getTableName();

        String tsvectorExpr = textColumns.stream()
                .map(c -> "coalesce(" + quoteIdentifier(c) + ", '')")
                .collect(Collectors.joining(" || ' ' || "));

        sb.append("ALTER TABLE ")
                .append(fqn)
                .append(" ADD COLUMN IF NOT EXISTS search_vector TSVECTOR\n")
                .append("    GENERATED ALWAYS AS (to_tsvector('simple', ")
                .append(tsvectorExpr)
                .append(")) STORED;\n\n");

        sb.append("CREATE INDEX IF NOT EXISTS idx_")
                .append(shortName)
                .append("_search\n")
                .append("    ON ")
                .append(fqn)
                .append(" USING GIN (search_vector);\n\n");

        String primaryTextCol = textColumns.getFirst();
        sb.append("CREATE INDEX IF NOT EXISTS idx_")
                .append(shortName)
                .append("_trgm\n")
                .append("    ON ")
                .append(fqn)
                .append(" USING GIN (")
                .append(quoteIdentifier(primaryTextCol))
                .append(" ext.gin_trgm_ops);\n\n");

        return sb.toString();
    }

    /**
     * Determines if a TEXT column should get a virtual generated casefold() column for search
     * and be included in the tsvector for full-text search.
     */
    private boolean isSearchableColumn(String name) {
        return name.endsWith("_value")
                || name.equals("comment_val")
                || name.endsWith("_name")
                || name.equals("name_val");
    }

    private String quoteIdentifier(String name) {
        if (name.matches("[a-z_][a-z0-9_]*")) {
            return name;
        }
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }
}
