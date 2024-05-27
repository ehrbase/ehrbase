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
package org.ehrbase.api.rest;

/**
 * EHRbase specific HTTP headers that are not part of the openEHR standard.
 */
public final class EHRbaseHeader {

    private EHRbaseHeader() {}

    public static final String TEMPLATE_ID = "EHRBase-Template-ID";

    /**
     * Used by the /query endpoint to perform only a dry run query.
     */
    public static final String AQL_DRY_RUN = "EHRbase-AQL-Dry-Run";

    /**
     * Used by the /query endpoint to provide the executed SQL statement in the return metadata.
     */
    public static final String AQL_EXECUTED_SQL = "EHRbase-AQL-Executed-SQL";

    /**
     * Used by the /query endpoint to provide the database query plan in the return metadata.
     */
    public static final String AQL_QUERY_PLAN = "EHRbase-AQL-Query-Plan";
}
