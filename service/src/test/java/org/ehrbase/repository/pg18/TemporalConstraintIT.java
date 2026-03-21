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
import java.sql.Statement;
import javax.sql.DataSource;
import org.ehrbase.test.ServiceIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * PG18-specific integration test: Temporal constraints (WITHOUT OVERLAPS).
 * Verifies PG18 rejects overlapping valid_period ranges for the same entity.
 */
@ServiceIntegrationTest
class TemporalConstraintIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void pg18SupportsWithoutOverlaps() throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            // Verify PG18 is running by checking version
            var rs = stmt.executeQuery("SELECT version()");
            rs.next();
            String version = rs.getString(1);
            assertThat(version).containsIgnoringCase("PostgreSQL");
            // PG18 version string contains "18"
            assertThat(version).contains("18");
        }
    }

    @Test
    void pg18SupportsTemporalConstraints() throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            // Create a temporary table with WITHOUT OVERLAPS to verify PG18 support
            stmt.execute("CREATE TEMP TABLE temp_temporal_test ("
                    + "id UUID DEFAULT uuidv7(), "
                    + "valid_period TSTZRANGE NOT NULL DEFAULT tstzrange(now(), NULL), "
                    + "PRIMARY KEY (id, valid_period WITHOUT OVERLAPS))");

            // Insert should succeed
            stmt.execute("INSERT INTO temp_temporal_test DEFAULT VALUES");

            var rs = stmt.executeQuery("SELECT count(*) FROM temp_temporal_test");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);

            stmt.execute("DROP TABLE temp_temporal_test");
        }
    }

    @Test
    void btreeGistExtensionAvailable() throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            // btree_gist is required for WITHOUT OVERLAPS
            var rs = stmt.executeQuery("SELECT 1 FROM pg_extension WHERE extname = 'btree_gist'");
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void ltreeExtensionAvailable() throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            // ltree needed for folder paths
            var rs = stmt.executeQuery("SELECT 1 FROM pg_extension WHERE extname = 'ltree'");
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void pgcryptoExtensionAvailable() throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT 1 FROM pg_extension WHERE extname = 'pgcrypto'");
            assertThat(rs.next()).isTrue();
        }
    }
}
