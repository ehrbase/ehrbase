package org.ehrbase.schemagen;

import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplateInput;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplateNode;
import org.ehrbase.schemagen.model.ColumnDescriptor;
import org.ehrbase.schemagen.model.TableDescriptor;

import java.util.List;
import java.util.Set;

/**
 * Analyzes a WebTemplate tree and produces TableDescriptor(s) for schema generation.
 * <p>
 * Walks the tree recursively:
 * - Structure nodes (SECTION, OBSERVATION, etc.) → recurse into children
 * - ELEMENT nodes → extract DV type from children, map to columns via TypeMapper
 * - Repeating CLUSTERs (max == -1) → separate child table
 */
public class TemplateAnalyzer {

    private static final Set<String> STRUCTURE_NODES = Set.of(
            "COMPOSITION", "SECTION", "OBSERVATION", "EVALUATION",
            "INSTRUCTION", "ACTION", "ADMIN_ENTRY", "ACTIVITY",
            "HISTORY", "EVENT", "POINT_EVENT", "INTERVAL_EVENT",
            "ITEM_TREE", "ITEM_LIST", "ITEM_SINGLE", "ITEM_TABLE"
    );

    /**
     * Analyzes a WebTemplate and produces a TableDescriptor with columns and child tables.
     */
    public TableDescriptor analyze(WebTemplate webTemplate) {
        String templateId = webTemplate.getTemplateId();
        WebTemplateNode root = webTemplate.getTree();

        String tableName = ColumnNamer.toTableName(root.getRmType(), templateId);
        TableDescriptor mainTable = new TableDescriptor(tableName, templateId);
        mainTable.addSystemColumns();

        walkNode(root, mainTable, "");

        // Add participations JSONB column (clinicians involved in composition)
        mainTable.addColumn(new ColumnDescriptor("participations", "JSONB DEFAULT '[]'"));

        return mainTable;
    }

    private void walkNode(WebTemplateNode node, TableDescriptor table, String pathPrefix) {
        for (WebTemplateNode child : node.getChildren()) {
            String rmType = child.getRmType();
            String childId = child.getId();

            if (isRepeatingCluster(child)) {
                // Repeating CLUSTER (max == -1 unbounded) → separate child table
                TableDescriptor childTable = createChildTable(child, table);
                table.addChildTable(childTable);

            } else if ("ELEMENT".equals(rmType)) {
                // ELEMENT wraps a DV type — look at children for the actual DV node
                handleElement(child, table, pathPrefix);

            } else if ("CLUSTER".equals(rmType)) {
                // Non-repeating CLUSTER — flatten into parent table with prefixed columns
                String newPrefix = buildPrefix(pathPrefix, childId);
                walkNode(child, table, newPrefix);

            } else if (isDvType(rmType)) {
                // Direct DV type (rare — usually wrapped in ELEMENT)
                String colBase = buildColumnName(pathPrefix, childId);
                List<ColumnDescriptor> cols = TypeMapper.map(rmType, colBase);
                cols.forEach(table::addColumn);

            } else if (STRUCTURE_NODES.contains(rmType)) {
                // Structure node — recurse without adding prefix (structures don't generate columns)
                walkNode(child, table, pathPrefix);
            }
        }
    }

    /**
     * Handles an ELEMENT node. ELEMENTs contain the actual DV type as a child
     * (e.g., ELEMENT with child DV_QUANTITY for a blood pressure reading).
     * The inputs list on the element also describes the expected data shape.
     */
    private void handleElement(WebTemplateNode element, TableDescriptor table, String pathPrefix) {
        String elementId = element.getId();
        String colBase = buildColumnName(pathPrefix, elementId);

        // Check if element has DV children (the actual data type)
        for (WebTemplateNode dvChild : element.getChildren()) {
            String dvType = dvChild.getRmType();
            if (isDvType(dvType)) {
                List<ColumnDescriptor> cols = TypeMapper.map(dvType, colBase);
                cols.forEach(table::addColumn);
                addNullFlavourIfOptional(element, table, colBase);
                return;
            }
        }

        // Fallback: if no DV child, use inputs to determine type
        if (!element.getInputs().isEmpty()) {
            String rmType = inferRmTypeFromInputs(element);
            List<ColumnDescriptor> cols = TypeMapper.map(rmType, colBase);
            cols.forEach(table::addColumn);
            addNullFlavourIfOptional(element, table, colBase);
            return;
        }

        // Last resort: JSONB fallback
        table.addColumn(new ColumnDescriptor(colBase, "JSONB"));
    }

    /**
     * Adds a _null_flavour TEXT column for optional ELEMENT nodes.
     * openEHR allows null_flavour on any data value (e.g., "not available", "unknown").
     * Only generated for optional elements (min == 0) since mandatory elements can't be null.
     */
    private void addNullFlavourIfOptional(WebTemplateNode element, TableDescriptor table, String colBase) {
        if (element.getMin() == 0) {
            table.addColumn(new ColumnDescriptor(colBase + "_null_flavour", "TEXT"));
        }
    }

    /**
     * Infers the RM type from the ELEMENT's inputs list.
     * WebTemplateInput has 'type' field: CODED_TEXT, TEXT, DECIMAL, INTEGER, DATE, TIME, DATETIME, etc.
     */
    private String inferRmTypeFromInputs(WebTemplateNode element) {
        List<WebTemplateInput> inputs = element.getInputs();
        if (inputs.isEmpty()) return "DV_TEXT";

        // If there's a 'coded_text' input, it's DV_CODED_TEXT
        for (WebTemplateInput input : inputs) {
            if ("CODED_TEXT".equals(input.getType())) return "DV_CODED_TEXT";
        }

        // Check first input type
        String firstType = inputs.getFirst().getType();
        return switch (firstType) {
            case "TEXT" -> "DV_TEXT";
            case "DECIMAL" -> "DV_QUANTITY";
            case "INTEGER" -> "DV_COUNT";
            case "BOOLEAN" -> "DV_BOOLEAN";
            case "DATE" -> "DV_DATE";
            case "TIME" -> "DV_TIME";
            case "DATETIME" -> "DV_DATE_TIME";
            case "DURATION" -> "DV_DURATION";
            default -> "DV_TEXT";
        };
    }

    private TableDescriptor createChildTable(WebTemplateNode node, TableDescriptor parent) {
        String childTableName = parent.getTableName() + "_" + ColumnNamer.toColumnName(node.getId());
        TableDescriptor childTable = new TableDescriptor(childTableName, parent.getTemplateId());
        childTable.addSystemColumns();
        childTable.addColumn(new ColumnDescriptor("parent_id", "UUID NOT NULL", false));

        walkNode(node, childTable, "");

        return childTable;
    }

    private boolean isDvType(String rmType) {
        return rmType != null && rmType.startsWith("DV_");
    }

    private boolean isRepeatingCluster(WebTemplateNode node) {
        // max == -1 means unbounded in WebTemplate
        return "CLUSTER".equals(node.getRmType()) && node.getMax() == -1;
    }

    private String buildPrefix(String existing, String id) {
        if (existing.isEmpty()) return id;
        return existing + "_" + id;
    }

    private String buildColumnName(String prefix, String id) {
        String colName = ColumnNamer.toColumnName(id);
        if (prefix.isEmpty()) return colName;
        String prefixClean = ColumnNamer.toColumnName(prefix);
        return prefixClean + "_" + colName;
    }
}
