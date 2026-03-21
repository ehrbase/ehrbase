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

import java.util.List;
import org.ehrbase.schemagen.model.ColumnDescriptor;

/**
 * Maps openEHR RM 1.1.0 data types to PostgreSQL column types.
 * Covers all 22 concrete DV types from the openEHR data_types package.
 */
public final class TypeMapper {

    private TypeMapper() {}

    /**
     * Maps an RM type name to one or more PostgreSQL column descriptors.
     *
     * @param rmType   the RM type name (e.g., "DV_QUANTITY", "DV_TEXT")
     * @param baseName the base column name (from ColumnNamer)
     * @return list of columns to generate for this data point
     */
    public static List<ColumnDescriptor> map(String rmType, String baseName) {
        return switch (rmType) {
            // === Basic types ===
            case "DV_BOOLEAN" -> List.of(col(baseName, "BOOLEAN"));

            case "DV_STATE" -> List.of(col(baseName, "JSONB"));

            case "DV_IDENTIFIER" ->
                List.of(
                        col(baseName + "_id", "TEXT"),
                        col(baseName + "_issuer", "TEXT"),
                        col(baseName + "_assigner", "TEXT"),
                        col(baseName + "_type", "TEXT"));

            // === Text types ===
            case "DV_TEXT" -> List.of(col(baseName, "TEXT"));

            case "DV_CODED_TEXT" ->
                List.of(
                        col(baseName + "_value", "TEXT"),
                        col(baseName + "_code", "TEXT"),
                        col(baseName + "_terminology", "TEXT"));

            // === Quantity types ===
            case "DV_QUANTITY" ->
                List.of(
                        col(baseName + "_magnitude", "DOUBLE PRECISION"),
                        col(baseName + "_units", "TEXT"),
                        col(baseName + "_precision", "INTEGER"));

            case "DV_COUNT" -> List.of(col(baseName, "BIGINT"));

            case "DV_PROPORTION" ->
                List.of(
                        col(baseName + "_numerator", "DOUBLE PRECISION"),
                        col(baseName + "_denominator", "DOUBLE PRECISION"),
                        col(baseName + "_type", "INTEGER"));

            case "DV_ORDINAL" ->
                List.of(
                        col(baseName + "_value", "INTEGER"),
                        col(baseName + "_symbol_value", "TEXT"),
                        col(baseName + "_symbol_code", "TEXT"));

            case "DV_SCALE" ->
                List.of(
                        col(baseName + "_value", "DOUBLE PRECISION"),
                        col(baseName + "_symbol_value", "TEXT"),
                        col(baseName + "_symbol_code", "TEXT"));

            // === Date/Time types ===
            // CRITICAL: Use TEXT for value (supports partial dates like "2024" or "2024-03").
            // Pre-compute magnitude (DOUBLE PRECISION) at write time for ORDER BY / comparison.
            case "DV_DATE" ->
                List.of(
                        col(baseName, "TEXT"), // ISO 8601 value
                        col(baseName + "_magnitude", "DOUBLE PRECISION")); // days since 0001-01-01

            case "DV_TIME" ->
                List.of(
                        col(baseName, "TEXT"), // ISO 8601 value
                        col(baseName + "_magnitude", "DOUBLE PRECISION")); // seconds since 00:00:00

            case "DV_DATE_TIME" ->
                List.of(
                        col(baseName, "TEXT"), // ISO 8601 value
                        col(baseName + "_magnitude", "DOUBLE PRECISION")); // seconds since 0001-01-01T00:00:00Z

            case "DV_DURATION" ->
                List.of(
                        col(baseName, "TEXT"), // ISO 8601 duration
                        col(baseName + "_magnitude", "DOUBLE PRECISION")); // total seconds

            // === Encapsulated types ===
            case "DV_MULTIMEDIA" -> List.of(col(baseName + "_uri", "TEXT"), col(baseName + "_media_type", "TEXT"));
            // Large binary data stored externally (S3/MinIO), NOT as BYTEA

            case "DV_PARSABLE" -> List.of(col(baseName + "_value", "TEXT"), col(baseName + "_formalism", "TEXT"));

            // === URI types ===
            case "DV_URI", "DV_EHR_URI" -> List.of(col(baseName, "TEXT"));

            // === Time specification types (very rare) ===
            case "DV_PERIODIC_TIME_SPECIFICATION", "DV_GENERAL_TIME_SPECIFICATION" -> List.of(col(baseName, "JSONB"));

            // === JSONB fallback for any unmapped type ===
            default -> List.of(col(baseName, "JSONB"));
        };
    }

    private static ColumnDescriptor col(String name, String pgType) {
        return new ColumnDescriptor(name, pgType);
    }
}
