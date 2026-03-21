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

import java.util.List;
import org.springframework.lang.Nullable;

/**
 * Metadata for a template-driven table in the {@code ehr_data} schema.
 * Used by the dynamic writer/reader to construct JOOQ queries without codegen.
 *
 * @param schemaName        schema name, typically "ehr_data"
 * @param tableName         table name (e.g., "comp_blood_pressure_v2")
 * @param historyTableName  history table name (e.g., "comp_blood_pressure_v2_history")
 * @param columns           column metadata for this table
 * @param childTables       child tables for repeating structures
 * @param parentTableName   parent table name if this is a child table (null for root)
 */
public record TemplateTableMetadata(
        String schemaName,
        String tableName,
        String historyTableName,
        List<ColumnMetadata> columns,
        List<TemplateTableMetadata> childTables,
        @Nullable String parentTableName) {

    /**
     * Fully qualified table name: "ehr_data.comp_blood_pressure_v2".
     */
    public String fqn() {
        return schemaName + "." + tableName;
    }

    /**
     * Fully qualified history table name: "ehr_data.comp_blood_pressure_v2_history".
     */
    public String historyFqn() {
        return schemaName + "." + historyTableName;
    }

    /**
     * Returns only the clinical (non-system) columns.
     */
    public List<ColumnMetadata> clinicalColumns() {
        return columns.stream().filter(c -> !c.isSystemColumn()).toList();
    }

    /**
     * Returns system columns (id, composition_id, ehr_id, valid_period, sys_version, sys_tenant).
     */
    public List<ColumnMetadata> systemColumns() {
        return columns.stream().filter(ColumnMetadata::isSystemColumn).toList();
    }

    /**
     * Whether this is a child table (has parent).
     */
    public boolean isChildTable() {
        return parentTableName != null;
    }
}
