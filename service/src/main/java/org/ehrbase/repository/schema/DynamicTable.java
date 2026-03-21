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

import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

/**
 * Utility for creating JOOQ table and field references from {@link TemplateTableMetadata}
 * without codegen. Used for dynamic queries on auto-generated {@code ehr_data} tables.
 */
public final class DynamicTable {

    private DynamicTable() {}

    /**
     * Creates a JOOQ Table reference for the current (non-history) table.
     */
    public static Table<?> table(TemplateTableMetadata meta) {
        return DSL.table(DSL.name(meta.schemaName(), meta.tableName()));
    }

    /**
     * Creates a JOOQ Table reference for the history table.
     */
    public static Table<?> historyTable(TemplateTableMetadata meta) {
        return DSL.table(DSL.name(meta.schemaName(), meta.historyTableName()));
    }

    /**
     * Creates an untyped JOOQ Field reference for a column.
     */
    public static Field<Object> field(TemplateTableMetadata meta, String columnName) {
        return DSL.field(DSL.name(meta.schemaName(), meta.tableName(), columnName));
    }

    /**
     * Creates a typed JOOQ Field reference for a column.
     */
    public static <T> Field<T> field(TemplateTableMetadata meta, String columnName, Class<T> type) {
        return DSL.field(DSL.name(meta.schemaName(), meta.tableName(), columnName), type);
    }

    /**
     * Creates an untyped JOOQ Field reference using only the column name (no table qualification).
     * Useful inside INSERT/SELECT where the table context is already established.
     */
    public static Field<Object> col(String columnName) {
        return DSL.field(DSL.name(columnName));
    }

    /**
     * Creates a typed JOOQ Field reference using only the column name.
     */
    public static <T> Field<T> col(String columnName, Class<T> type) {
        return DSL.field(DSL.name(columnName), type);
    }
}
