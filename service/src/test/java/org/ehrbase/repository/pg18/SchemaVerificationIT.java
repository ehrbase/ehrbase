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
package org.ehrbase.repository.pg18;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.ehrbase.test.EhrbasePostgreSQLContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the PG18 schema is fully set up with all expected tables.
 */
class SchemaVerificationIT {

    private static EhrbasePostgreSQLContainer pg;

    @BeforeAll
    static void startContainer() {
        pg = EhrbasePostgreSQLContainer.sharedInstance();
    }

    private Connection connect() throws Exception {
        return DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
    }

    @Test
    void listAllTables() throws Exception {
        List<String> tables = new ArrayList<>();
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT table_schema || '.' || table_name FROM information_schema.tables "
                            + "WHERE table_schema IN ('ehr_system','ehr_data','ehr_views','ehr_staging') "
                            + "ORDER BY 1");
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        System.out.println("=== Tables in PG18 ===");
        tables.forEach(System.out::println);
        System.out.println("=== Total: " + tables.size() + " ===");

        // Core tables from V1__core_tables.sql
        assertThat(tables).contains("ehr_system.tenant");
        assertThat(tables).contains("ehr_system.ehr");
        assertThat(tables).contains("ehr_system.users");
        assertThat(tables).contains("ehr_system.template");

        // Versioned tables from V2__versioned_tables.sql
        assertThat(tables).contains("ehr_system.contribution");
        assertThat(tables).contains("ehr_system.composition");
        assertThat(tables).contains("ehr_system.ehr_status");

        // Compliance from V3
        assertThat(tables).contains("ehr_system.audit_event");

        // View catalog from V6
        assertThat(tables).contains("ehr_system.view_catalog");
    }

    @Test
    void defaultTenantExists() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT name FROM ehr_system.tenant WHERE id = 1");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("default");
        }
    }
}
