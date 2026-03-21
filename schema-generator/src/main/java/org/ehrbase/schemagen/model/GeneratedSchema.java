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

/**
 * Output of {@link org.ehrbase.schemagen.SchemaGenerator}: DDL for tables, views, and search indexes.
 *
 * @param tableName  the generated table name (e.g., "obs_blood_pressure_v2")
 * @param ddl        CREATE TABLE + _history + RLS + indexes (executes against ehr_data schema)
 * @param viewDdl    CREATE VIEW + history view + as_of function (executes against ehr_views schema)
 * @param searchDdl  tsvector columns + GIN indexes for full-text search (executes against ehr_data schema)
 */
public record GeneratedSchema(String tableName, String ddl, String viewDdl, String searchDdl) {

    public GeneratedSchema(String tableName, String ddl) {
        this(tableName, ddl, "", "");
    }
}
