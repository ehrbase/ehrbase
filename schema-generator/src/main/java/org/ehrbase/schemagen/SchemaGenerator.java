package org.ehrbase.schemagen;

import org.ehrbase.schemagen.model.ColumnDescriptor;
import org.ehrbase.schemagen.model.GeneratedSchema;
import org.ehrbase.schemagen.model.TableDescriptor;

/**
 * Generates PostgreSQL 18 DDL from a TableDescriptor.
 * Produces: CREATE TABLE, _history table, RLS policies, indexes, and views.
 */
public class SchemaGenerator {

    /**
     * Generates complete DDL for a template's tables, history, RLS, indexes, and views.
     */
    public GeneratedSchema generate(TableDescriptor table) {
        var ddl = new StringBuilder();

        ddl.append(generateCreateTable(table));
        ddl.append(generateHistoryTable(table));
        ddl.append(generateRlsPolicies(table));
        ddl.append(generateIndexes(table));
        ddl.append(generateView(table));

        for (TableDescriptor child : table.getChildTables()) {
            ddl.append(generate(child));
        }

        return new GeneratedSchema(table.getTableName(), ddl.toString());
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
                sb.append("    , ").append(quoteIdentifier(col.name() + "_search"))
                  .append(" TEXT GENERATED ALWAYS AS (casefold(").append(quoteIdentifier(col.name()))
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
            sb.append(table.getSchema()).append(".").append(table.getParentTableName()).append(" (id)\n");
        }

        sb.append(");\n\n");

        return sb.toString();
    }

    private String generateHistoryTable(TableDescriptor table) {
        String fqn = table.getFullyQualifiedName();
        return "-- History table: populated by application code (NOT triggers)\n"
                + "CREATE TABLE IF NOT EXISTS " + fqn + "_history (\n"
                + "    LIKE " + fqn + " INCLUDING ALL\n"
                + ");\n\n";
    }

    private String generateRlsPolicies(TableDescriptor table) {
        String fqn = table.getFullyQualifiedName();
        return "ALTER TABLE " + fqn + " ENABLE ROW LEVEL SECURITY;\n"
                + "ALTER TABLE " + fqn + " FORCE ROW LEVEL SECURITY;\n"
                + "CREATE POLICY tenant_policy ON " + fqn + "\n"
                + "    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);\n\n"
                + "ALTER TABLE " + fqn + "_history ENABLE ROW LEVEL SECURITY;\n"
                + "ALTER TABLE " + fqn + "_history FORCE ROW LEVEL SECURITY;\n"
                + "CREATE POLICY tenant_policy ON " + fqn + "_history\n"
                + "    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);\n\n";
    }

    private String generateIndexes(TableDescriptor table) {
        String fqn = table.getFullyQualifiedName();
        String shortName = table.getTableName();

        var sb = new StringBuilder();

        // Index on composition_id (most common query pattern)
        sb.append("CREATE INDEX IF NOT EXISTS idx_").append(shortName).append("_composition\n");
        sb.append("    ON ").append(fqn).append(" (composition_id);\n");

        // Index on ehr_id (patient queries)
        sb.append("CREATE INDEX IF NOT EXISTS idx_").append(shortName).append("_ehr\n");
        sb.append("    ON ").append(fqn).append(" (ehr_id);\n");

        sb.append("\n");
        return sb.toString();
    }

    private String generateView(TableDescriptor table) {
        String dataTable = table.getFullyQualifiedName();
        String viewName = "ehr_views.v_" + table.getTableName();

        return "CREATE OR REPLACE VIEW " + viewName + " AS\n"
                + "SELECT t.*,\n"
                + "    e.subject_id,\n"
                + "    c.committed_at,\n"
                + "    c.committer_name,\n"
                + "    c.sys_version AS composition_version,\n"
                + "    c.change_type\n"
                + "FROM " + dataTable + " t\n"
                + "JOIN ehr_system.composition c ON t.composition_id = c.id\n"
                + "JOIN ehr_system.ehr e ON t.ehr_id = e.id;\n\n";
    }

    /**
     * Determines if a TEXT column should get a virtual generated casefold() column for search.
     * Only for columns likely to be searched case-insensitively (value fields, names, comments).
     */
    private boolean isSearchableColumn(String name) {
        return name.endsWith("_value") || name.equals("comment_val")
                || name.endsWith("_name") || name.equals("name_val");
    }

    private String quoteIdentifier(String name) {
        // Only quote if it contains special chars or is a reserved word
        if (name.matches("[a-z_][a-z0-9_]*")) {
            return name;
        }
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }
}
