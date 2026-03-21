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

import org.springframework.lang.Nullable;

/**
 * Metadata for a single column in a template-driven table.
 *
 * @param columnName     PostgreSQL column name (e.g., "systolic_magnitude")
 * @param pgType         PostgreSQL type (e.g., "DOUBLE PRECISION", "TEXT")
 * @param nullable        whether the column allows NULL values
 * @param isSystemColumn  true for id, composition_id, ehr_id, valid_period, sys_version, sys_tenant
 * @param rmPath          openEHR RM path for this column (used by writer/reader mapping)
 * @param rmType          openEHR RM type name (e.g., "DV_QUANTITY", "DV_CODED_TEXT")
 */
public record ColumnMetadata(
        String columnName,
        String pgType,
        boolean nullable,
        boolean isSystemColumn,
        @Nullable String rmPath,
        @Nullable String rmType) {

    private static final java.util.Set<String> SYSTEM_COLUMNS = java.util.Set.of(
            "id", "composition_id", "ehr_id", "valid_period", "sys_version", "sys_tenant", "parent_id");

    /**
     * Creates a ColumnMetadata from a schema-generator {@link org.ehrbase.schemagen.model.ColumnDescriptor}.
     */
    public static ColumnMetadata fromColumnDescriptor(org.ehrbase.schemagen.model.ColumnDescriptor cd) {
        return new ColumnMetadata(
                cd.name(), cd.pgType(), cd.nullable(), SYSTEM_COLUMNS.contains(cd.name()), null, null);
    }

    /**
     * Creates a ColumnMetadata from information_schema query results.
     */
    public static ColumnMetadata fromInformationSchema(String columnName, String dataType, boolean isNullable) {
        return new ColumnMetadata(columnName, dataType, isNullable, SYSTEM_COLUMNS.contains(columnName), null, null);
    }
}
