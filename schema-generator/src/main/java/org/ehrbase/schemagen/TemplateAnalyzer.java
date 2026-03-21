package org.ehrbase.schemagen;

import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplateNode;
import org.ehrbase.schemagen.model.ColumnDescriptor;
import org.ehrbase.schemagen.model.TableDescriptor;

import java.util.List;
import java.util.Set;

/**
 * Analyzes a WebTemplate tree and produces TableDescriptor(s) for schema generation.
 * Walks the tree recursively, mapping structure nodes to table hierarchy and
 * data value nodes to PostgreSQL columns via TypeMapper.
 */
public class TemplateAnalyzer {

    private static final Set<String> STRUCTURE_NODES = Set.of(
            "COMPOSITION", "SECTION", "OBSERVATION", "EVALUATION",
            "INSTRUCTION", "ACTION", "ADMIN_ENTRY", "ACTIVITY",
            "HISTORY", "EVENT", "POINT_EVENT", "INTERVAL_EVENT",
            "ITEM_TREE", "ITEM_LIST", "ITEM_SINGLE", "ITEM_TABLE",
            "CLUSTER", "ELEMENT"
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

        return mainTable;
    }

    private void walkNode(WebTemplateNode node, TableDescriptor table, String pathPrefix) {
        for (WebTemplateNode child : node.getChildren()) {
            String rmType = child.getRmType();
            String childPath = buildPath(pathPrefix, child.getId());

            if (isRepeatingCluster(child)) {
                TableDescriptor childTable = createChildTable(child, table, childPath);
                table.addChildTable(childTable);
            } else if (isDataValueNode(rmType)) {
                String colBaseName = ColumnNamer.toColumnName(child.getId());
                String prefixedBaseName = pathPrefix.isEmpty()
                        ? colBaseName
                        : pathPrefix.replace("/", "_").replaceFirst("^_", "") + "_" + colBaseName;

                List<ColumnDescriptor> columns = TypeMapper.map(rmType, prefixedBaseName);
                for (ColumnDescriptor col : columns) {
                    table.addColumn(col);
                }

                // If element has children (e.g., ELEMENT wrapping a DV type), recurse
                if (!child.getChildren().isEmpty() && "ELEMENT".equals(rmType)) {
                    walkNode(child, table, childPath);
                }
            } else if (isStructureNode(rmType)) {
                walkNode(child, table, childPath);
            }
        }
    }

    private TableDescriptor createChildTable(WebTemplateNode node, TableDescriptor parent, String path) {
        String childTableName = parent.getTableName() + "_" + ColumnNamer.toColumnName(node.getId());
        TableDescriptor childTable = new TableDescriptor(childTableName, parent.getTemplateId());
        childTable.addSystemColumns();

        // FK to parent
        childTable.addColumn(new ColumnDescriptor("parent_id", "UUID NOT NULL", false));

        // Walk the cluster's children
        walkNode(node, childTable, "");

        return childTable;
    }

    private boolean isStructureNode(String rmType) {
        return STRUCTURE_NODES.contains(rmType);
    }

    private boolean isDataValueNode(String rmType) {
        return rmType.startsWith("DV_") || "ELEMENT".equals(rmType);
    }

    private boolean isRepeatingCluster(WebTemplateNode node) {
        return "CLUSTER".equals(node.getRmType())
                && node.getMax() != 1
                && node.getMax() == -1; // -1 means unbounded in WebTemplate
    }

    private String buildPath(String prefix, String id) {
        if (prefix.isEmpty()) {
            return id;
        }
        return prefix + "/" + id;
    }
}
