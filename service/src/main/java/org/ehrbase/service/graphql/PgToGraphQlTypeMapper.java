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
package org.ehrbase.service.graphql;

/**
 * Maps PostgreSQL column types to GraphQL scalar type names.
 */
public final class PgToGraphQlTypeMapper {

    private PgToGraphQlTypeMapper() {}

    /**
     * Maps a PostgreSQL type string to a GraphQL type name.
     *
     * @param pgType PostgreSQL type (e.g., "UUID DEFAULT uuidv7()", "TEXT", "DOUBLE PRECISION")
     * @param nullable whether the column allows null
     * @return GraphQL type string (e.g., "ID!", "String", "Float")
     */
    public static String map(String pgType, boolean nullable) {
        String baseType = mapBase(pgType);
        if (baseType == null) {
            return null;
        }
        return nullable ? baseType : baseType + "!";
    }

    private static String mapBase(String pgType) {
        if (pgType == null) {
            return "String";
        }
        String normalized = pgType.toUpperCase().trim();

        if (normalized.startsWith("UUID")) return "ID";
        if (normalized.equals("TEXT")) return "String";
        if (normalized.equals("DOUBLE PRECISION")) return "Float";
        if (normalized.equals("INTEGER") || normalized.equals("INT") || normalized.startsWith("INT ")) return "Int";
        if (normalized.equals("SMALLINT")) return "Int";
        if (normalized.equals("BIGINT")) return "Long";
        if (normalized.equals("BOOLEAN") || normalized.startsWith("BOOLEAN ")) return "Boolean";
        if (normalized.startsWith("TIMESTAMPTZ") || normalized.equals("TIMESTAMP WITH TIME ZONE")) return "DateTime";
        if (normalized.startsWith("TSTZRANGE")) return "DateTimeRange";
        if (normalized.equals("JSONB") || normalized.startsWith("JSONB ")) return "JSON";
        if (normalized.equals("TSVECTOR") || normalized.startsWith("TSVECTOR ")) return null;

        if (normalized.startsWith("TEXT ")) return "String";
        if (normalized.startsWith("INT ")) return "Int";

        return "String";
    }

    /**
     * Converts a snake_case column name to camelCase for GraphQL fields.
     */
    public static String toCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }
        String[] parts = snakeCase.split("_");
        var sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    sb.append(parts[i].substring(1));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Converts a view name like "v_blood_pressure" to PascalCase GraphQL type "BloodPressure".
     */
    public static String toTypeName(String viewName) {
        String name = viewName.startsWith("v_") ? viewName.substring(2) : viewName;
        String[] parts = name.split("_");
        var sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Converts a view name to a GraphQL query field name (camelCase, plural).
     * "v_blood_pressure" -> "bloodPressures"
     */
    public static String toQueryFieldName(String viewName) {
        String name = viewName.startsWith("v_") ? viewName.substring(2) : viewName;
        return toCamelCase(name) + "s";
    }

    /**
     * Determines the filter input type name for a GraphQL type.
     */
    public static String filterInputTypeName(String graphQlType) {
        return switch (graphQlType.replace("!", "")) {
            case "ID" -> "IDFilter";
            case "String" -> "StringFilter";
            case "Float" -> "FloatFilter";
            case "Int", "Long" -> "IntFilter";
            case "Boolean" -> "BooleanFilter";
            case "DateTime" -> "DateTimeFilter";
            default -> null;
        };
    }
}
