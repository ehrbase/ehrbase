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
package org.ehrbase.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.ehrbase.api.tenant.TenantContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DataSourceConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JOOQ connection provider that executes SET LOCAL for PostgreSQL RLS session variables
 * on each connection acquisition. Reads tenant, user, and role from {@link TenantContext}.
 *
 * <p>Uses SET LOCAL (transaction-scoped) instead of SET (session-scoped) to ensure
 * HikariCP connection pool safety — variables auto-reset when the transaction ends.
 */
public class TenantAwareConnectionProvider extends DataSourceConnectionProvider {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareConnectionProvider.class);

    public TenantAwareConnectionProvider(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Connection acquire() {
        Connection connection = super.acquire();
        TenantContext.TenantInfo tenantInfo = TenantContext.get();

        if (tenantInfo != null) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET LOCAL ehrbase.current_tenant = " + tenantInfo.tenantId());
                stmt.execute("SET LOCAL ehrbase.current_user_id = '" + escapeSql(tenantInfo.userId()) + "'");
                stmt.execute("SET LOCAL ehrbase.user_role = '" + escapeSql(tenantInfo.userRole()) + "'");
                stmt.execute("SET LOCAL ehrbase.actor_id = '" + escapeSql(tenantInfo.userId()) + "'");
            } catch (SQLException e) {
                throw new DataAccessException("Failed to set tenant session variables", e);
            }
            log.trace(
                    "SET LOCAL tenant={} user={} role={}",
                    tenantInfo.tenantId(),
                    tenantInfo.userId(),
                    tenantInfo.userRole());
        }

        return connection;
    }

    private static String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }
}
