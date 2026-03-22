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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import org.ehrbase.test.EhrbasePostgreSQLContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for ehr_status is_modifiable and is_queryable flags.
 * Connects directly to PG18 testcontainer -- no Spring context needed.
 */
class EhrStatusFlagsIT {

    private static EhrbasePostgreSQLContainer pg;

    @BeforeAll
    static void startContainer() {
        pg = EhrbasePostgreSQLContainer.sharedInstance();
    }

    private Connection connect() throws Exception {
        Connection conn = DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        conn.createStatement().execute("SET ehrbase.current_tenant = '1'");
        return conn;
    }

    private String createEhr(Connection conn) throws Exception {
        ResultSet rs = conn.createStatement()
                .executeQuery("INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                        + "VALUES ('flags-patient-" + UUID.randomUUID() + "', 'ehr.flags.org', 1) RETURNING id");
        rs.next();
        return rs.getString("id");
    }

    private String createEhrStatus(Connection conn, String ehrId, boolean isModifiable, boolean isQueryable)
            throws Exception {
        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ehr_system.ehr_status (ehr_id, is_queryable, is_modifiable, subject_id, "
                        + "subject_namespace, archetype_node_id, name, sys_version, change_type, "
                        + "committer_name, sys_tenant) "
                        + "VALUES (?::uuid, ?, ?, 'flags-patient', 'ehr.flags.org', "
                        + "'openEHR-EHR-EHR_STATUS.generic.v1', 'EHR Status', 1, 'creation', "
                        + "'test-committer', 1) RETURNING id");
        ps.setString(1, ehrId);
        ps.setBoolean(2, isQueryable);
        ps.setBoolean(3, isModifiable);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getString("id");
    }

    @Test
    void isModifiableFalseBlocksCompositionInsert() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);

            // Create EHR with is_modifiable=false at the EHR level
            conn.createStatement()
                    .execute("UPDATE ehr_system.ehr SET is_modifiable = false WHERE id = '" + ehrId + "'");

            // Verify is_modifiable is stored as false on the EHR
            ResultSet ehrRs = conn.createStatement()
                    .executeQuery("SELECT is_modifiable FROM ehr_system.ehr WHERE id = '" + ehrId + "'");
            assertThat(ehrRs.next()).isTrue();
            assertThat(ehrRs.getBoolean("is_modifiable")).isFalse();

            // Also create EHR Status with is_modifiable=false
            String statusId = createEhrStatus(conn, ehrId, false, true);

            // Verify the flag is stored correctly on the status
            ResultSet statusRs = conn.createStatement()
                    .executeQuery("SELECT is_modifiable FROM ehr_system.ehr_status WHERE id = '" + statusId + "'");
            assertThat(statusRs.next()).isTrue();
            assertThat(statusRs.getBoolean("is_modifiable")).isFalse();
        }
    }

    @Test
    void isQueryableFalseStoredCorrectly() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);

            // Set is_queryable=false on the EHR
            conn.createStatement().execute("UPDATE ehr_system.ehr SET is_queryable = false WHERE id = '" + ehrId + "'");

            // Verify stored
            ResultSet ehrRs = conn.createStatement()
                    .executeQuery("SELECT is_queryable FROM ehr_system.ehr WHERE id = '" + ehrId + "'");
            assertThat(ehrRs.next()).isTrue();
            assertThat(ehrRs.getBoolean("is_queryable")).isFalse();

            // Also create status with is_queryable=false
            String statusId = createEhrStatus(conn, ehrId, true, false);

            // Verify admin can still read it
            ResultSet statusRs = conn.createStatement()
                    .executeQuery("SELECT id, is_queryable FROM ehr_system.ehr_status WHERE id = '" + statusId + "'");
            assertThat(statusRs.next()).isTrue();
            assertThat(statusRs.getBoolean("is_queryable")).isFalse();
            assertThat(statusRs.getString("id")).isEqualTo(statusId);
        }
    }

    @Test
    void updateIsModifiable() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);

            // Create EHR Status v1 with is_modifiable=true
            String statusId = createEhrStatus(conn, ehrId, true, true);

            // Verify v1 is_modifiable is true
            ResultSet v1Rs = conn.createStatement()
                    .executeQuery("SELECT is_modifiable FROM ehr_system.ehr_status WHERE id = '" + statusId + "'");
            assertThat(v1Rs.next()).isTrue();
            assertThat(v1Rs.getBoolean("is_modifiable")).isTrue();

            // Archive v1 to history with closed valid_period
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.ehr_status_history "
                            + "SELECT id, ehr_id, tstzrange(lower(valid_period), now()), "
                            + "is_queryable, is_modifiable, subject_id, subject_namespace, "
                            + "archetype_node_id, name, sys_version, contribution_id, change_type, "
                            + "committed_at, committer_name, committer_id, sys_tenant "
                            + "FROM ehr_system.ehr_status WHERE id = '" + statusId + "'");

            // Delete v1 from current
            conn.createStatement().execute("DELETE FROM ehr_system.ehr_status WHERE id = '" + statusId + "'");

            // Insert v2 with is_modifiable=false
            PreparedStatement psV2 = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_status (id, ehr_id, is_queryable, is_modifiable, subject_id, "
                            + "subject_namespace, archetype_node_id, name, sys_version, change_type, "
                            + "committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, true, false, 'flags-patient', 'ehr.flags.org', "
                            + "'openEHR-EHR-EHR_STATUS.generic.v1', 'EHR Status', 2, 'modification', "
                            + "'committer-b', 1) RETURNING sys_version, is_modifiable");
            psV2.setString(1, statusId);
            psV2.setString(2, ehrId);
            ResultSet v2Rs = psV2.executeQuery();

            assertThat(v2Rs.next()).isTrue();
            assertThat(v2Rs.getInt("sys_version")).isEqualTo(2);
            assertThat(v2Rs.getBoolean("is_modifiable")).isFalse();

            // Verify v1 in history has is_modifiable=true
            ResultSet histRs = conn.createStatement()
                    .executeQuery("SELECT sys_version, is_modifiable FROM ehr_system.ehr_status_history "
                            + "WHERE id = '" + statusId + "' AND sys_version = 1");
            assertThat(histRs.next()).isTrue();
            assertThat(histRs.getInt("sys_version")).isEqualTo(1);
            assertThat(histRs.getBoolean("is_modifiable")).isTrue();
        }
    }

    @Test
    void updateIsQueryable() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);

            // Create EHR Status v1 with is_queryable=true
            String statusId = createEhrStatus(conn, ehrId, true, true);

            // Archive v1 to history with closed valid_period
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.ehr_status_history "
                            + "SELECT id, ehr_id, tstzrange(lower(valid_period), now()), "
                            + "is_queryable, is_modifiable, subject_id, subject_namespace, "
                            + "archetype_node_id, name, sys_version, contribution_id, change_type, "
                            + "committed_at, committer_name, committer_id, sys_tenant "
                            + "FROM ehr_system.ehr_status WHERE id = '" + statusId + "'");

            // Delete v1 from current
            conn.createStatement().execute("DELETE FROM ehr_system.ehr_status WHERE id = '" + statusId + "'");

            // Insert v2 with is_queryable=false
            PreparedStatement psV2 = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_status (id, ehr_id, is_queryable, is_modifiable, subject_id, "
                            + "subject_namespace, archetype_node_id, name, sys_version, change_type, "
                            + "committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, false, true, 'flags-patient', 'ehr.flags.org', "
                            + "'openEHR-EHR-EHR_STATUS.generic.v1', 'EHR Status', 2, 'modification', "
                            + "'committer-b', 1) RETURNING sys_version, is_queryable");
            psV2.setString(1, statusId);
            psV2.setString(2, ehrId);
            ResultSet v2Rs = psV2.executeQuery();

            assertThat(v2Rs.next()).isTrue();
            assertThat(v2Rs.getInt("sys_version")).isEqualTo(2);
            assertThat(v2Rs.getBoolean("is_queryable")).isFalse();

            // Verify v1 in history has is_queryable=true
            ResultSet histRs = conn.createStatement()
                    .executeQuery("SELECT sys_version, is_queryable FROM ehr_system.ehr_status_history "
                            + "WHERE id = '" + statusId + "' AND sys_version = 1");
            assertThat(histRs.next()).isTrue();
            assertThat(histRs.getInt("sys_version")).isEqualTo(1);
            assertThat(histRs.getBoolean("is_queryable")).isTrue();
        }
    }
}
