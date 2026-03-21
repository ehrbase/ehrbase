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
package org.ehrbase.schemagen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TableDescriptor {

    private final String tableName;
    private final String templateId;
    private final String schema;
    private final List<ColumnDescriptor> columns = new ArrayList<>();
    private final List<TableDescriptor> childTables = new ArrayList<>();
    private String parentTableName;

    public TableDescriptor(String tableName, String templateId) {
        this(tableName, templateId, "ehr_data");
    }

    public TableDescriptor(String tableName, String templateId, String schema) {
        this.tableName = tableName;
        this.templateId = templateId;
        this.schema = schema;
    }

    public void addSystemColumns() {
        columns.add(new ColumnDescriptor("id", "UUID DEFAULT uuidv7()", false));
        columns.add(new ColumnDescriptor("composition_id", "UUID", false));
        columns.add(new ColumnDescriptor("ehr_id", "UUID", false));
        columns.add(new ColumnDescriptor("valid_period", "TSTZRANGE NOT NULL DEFAULT tstzrange(now(), NULL)", false));
        columns.add(new ColumnDescriptor("sys_version", "INT NOT NULL DEFAULT 1", false));
        columns.add(new ColumnDescriptor("sys_tenant", "SMALLINT", false));
    }

    public void addColumn(ColumnDescriptor column) {
        // Deduplicate: if column name already exists, append a numeric suffix
        String name = column.name();
        Set<String> existingNames = columns.stream().map(ColumnDescriptor::name).collect(Collectors.toSet());
        String uniqueName = name;
        int suffix = 2;
        while (existingNames.contains(uniqueName)) {
            uniqueName = name + "_" + suffix++;
        }
        if (!uniqueName.equals(name)) {
            column = new ColumnDescriptor(
                    uniqueName, column.pgType(), column.nullable(), column.defaultValue(), column.comment());
        }
        columns.add(column);
    }

    public void addColumns(List<ColumnDescriptor> cols, String pathPrefix) {
        for (ColumnDescriptor col : cols) {
            String prefixedName = pathPrefix.isEmpty()
                    ? col.name()
                    : pathPrefix.replace("/", "_").replaceFirst("^_", "") + "_" + col.name();
            columns.add(new ColumnDescriptor(
                    prefixedName, col.pgType(), col.nullable(), col.defaultValue(), col.comment()));
        }
    }

    public void addChildTable(TableDescriptor child) {
        child.parentTableName = this.tableName;
        childTables.add(child);
    }

    public String getTableName() {
        return tableName;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getSchema() {
        return schema;
    }

    public List<ColumnDescriptor> getColumns() {
        return columns;
    }

    public List<TableDescriptor> getChildTables() {
        return childTables;
    }

    public String getParentTableName() {
        return parentTableName;
    }

    public String getFullyQualifiedName() {
        return schema + "." + tableName;
    }
}
