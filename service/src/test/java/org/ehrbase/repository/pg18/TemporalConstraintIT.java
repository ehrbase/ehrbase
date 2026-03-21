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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.ehrbase.test.EhrbasePostgreSQLContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * PG18-specific integration test: Temporal constraints (WITHOUT OVERLAPS).
 * Connects directly to PG18 testcontainer — no Spring context needed.
 */
class TemporalConstraintIT {

    private static EhrbasePostgreSQLContainer pg;

    @BeforeAll
    static void startContainer() {
        pg = EhrbasePostgreSQLContainer.sharedInstance();
    }

    private Connection connect() throws Exception {
        return DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
    }

    @Test
    void pg18VersionConfirmed() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT version()");
            rs.next();
            assertThat(rs.getString(1)).contains("18");
        }
    }

    @Test
    void withoutOverlapsConstraintWorks() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TEMP TABLE test_temporal ("
                    + "id UUID DEFAULT uuidv7(), "
                    + "valid_period TSTZRANGE NOT NULL DEFAULT tstzrange(now(), NULL), "
                    + "PRIMARY KEY (id, valid_period WITHOUT OVERLAPS))");

            stmt.execute("INSERT INTO test_temporal DEFAULT VALUES");

            var rs = stmt.executeQuery("SELECT count(*) FROM test_temporal");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);

            stmt.execute("DROP TABLE test_temporal");
        }
    }

    @Test
    void overlappingPeriodsRejected() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TEMP TABLE test_overlap ("
                    + "id UUID, "
                    + "valid_period TSTZRANGE NOT NULL, "
                    + "PRIMARY KEY (id, valid_period WITHOUT OVERLAPS))");

            var id = java.util.UUID.randomUUID();
            stmt.execute("INSERT INTO test_overlap VALUES ('" + id + "', "
                    + "tstzrange('2025-01-01'::timestamptz, '2025-12-31'::timestamptz))");

            // Overlapping period for same ID should be rejected
            assertThatThrownBy(() -> stmt.execute("INSERT INTO test_overlap VALUES ('" + id + "', "
                            + "tstzrange('2025-06-01'::timestamptz, '2026-06-01'::timestamptz))"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("conflicting key value");

            stmt.execute("DROP TABLE test_overlap");
        }
    }

    @Test
    void btreeGistExtensionAvailable() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT 1 FROM pg_extension WHERE extname = 'btree_gist'");
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void ltreeExtensionAvailable() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT 1 FROM pg_extension WHERE extname = 'ltree'");
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void pgcryptoExtensionAvailable() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT 1 FROM pg_extension WHERE extname = 'pgcrypto'");
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void ehrSystemSchemaExists() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery(
                    "SELECT 1 FROM information_schema.schemata WHERE schema_name = 'ehr_system'");
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void ehrDataSchemaExists() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery(
                    "SELECT 1 FROM information_schema.schemata WHERE schema_name = 'ehr_data'");
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void ehrViewsSchemaExists() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery(
                    "SELECT 1 FROM information_schema.schemata WHERE schema_name = 'ehr_views'");
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void compositionTableUsesUuidv7() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery(
                    "SELECT column_default FROM information_schema.columns "
                            + "WHERE table_schema = 'ehr_system' AND table_name = 'composition' AND column_name = 'id'");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).contains("uuidv7");
        }
    }

    @Test
    void compositionTableHasTemporalConstraint() throws Exception {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery(
                    "SELECT column_name FROM information_schema.columns "
                            + "WHERE table_schema = 'ehr_system' AND table_name = 'composition' "
                            + "AND column_name = 'valid_period'");
            assertThat(rs.next()).isTrue();
        }
    }
}
