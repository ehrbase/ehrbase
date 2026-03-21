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
package org.ehrbase.plugin.extensionpoints;

import java.time.Duration;
import java.util.Map;
import org.pf4j.ExtensionPoint;

/**
 * PF4J extension point for intercepting GraphQL-generated SQL queries.
 * Replaces the AQL-based {@code QueryExtensionPoint}.
 *
 * <p>Plugins can implement this to inspect, modify, or reject SQL queries
 * before they are executed against {@code ehr_views}.
 */
public interface SqlQueryInterceptor extends ExtensionPoint {

    /**
     * Called before a GraphQL-generated SQL query is executed.
     * Implementations can modify the SQL or throw to reject the query.
     *
     * @param sql       the SQL query about to be executed
     * @param params    query parameters
     * @param userId    the authenticated user ID
     * @param tenantId  the current tenant ID
     * @return the (possibly modified) SQL to execute
     */
    default String beforeQuery(String sql, Map<String, Object> params, String userId, short tenantId) {
        return sql;
    }

    /**
     * Called after query execution with result metadata.
     *
     * @param sql       the SQL that was executed
     * @param rowCount  number of rows returned
     * @param elapsed   query execution duration
     * @param userId    the authenticated user ID
     * @param tenantId  the current tenant ID
     */
    default void afterQuery(String sql, int rowCount, Duration elapsed, String userId, short tenantId) {
        // no-op by default
    }
}
