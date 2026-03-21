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
 * Integration tests for ehr_system.ehr and ehr_system.ehr_status tables.
 * Connects directly to PG18 testcontainer — no Spring context needed.
 */
class EhrRepositoryIT {

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

    @Test
    void createEhr() throws Exception {
        try (Connection conn = connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                            + "VALUES (?, ?, 1) RETURNING id, subject_id, subject_namespace, creation_date");
            ps.setString(1, "patient-001");
            ps.setString(2, "ehr.test.org");
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            UUID id = UUID.fromString(rs.getString("id"));
            assertThat(id).isNotNull();
            assertThat(id.version()).isEqualTo(7);
            assertThat(rs.getString("subject_id")).isEqualTo("patient-001");
            assertThat(rs.getString("subject_namespace")).isEqualTo("ehr.test.org");
            assertThat(rs.getTimestamp("creation_date")).isNotNull();
        }
    }

    @Test
    void createEhrWithId() throws Exception {
        UUID explicitId = UUID.randomUUID();
        try (Connection conn = connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr (id, subject_id, subject_namespace, sys_tenant) "
                            + "VALUES (?::uuid, ?, ?, 1) RETURNING id");
            ps.setString(1, explicitId.toString());
            ps.setString(2, "patient-explicit");
            ps.setString(3, "ehr.test.org");
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id"))).isEqualTo(explicitId);
        }
    }

    @Test
    void createEhrStatus() throws Exception {
        try (Connection conn = connect()) {
            // Create EHR first
            ResultSet ehrRs = conn.createStatement()
                    .executeQuery(
                            "INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                                    + "VALUES ('patient-status', 'ehr.test.org', 1) RETURNING id");
            ehrRs.next();
            String ehrId = ehrRs.getString("id");

            // Create EHR Status
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_status (ehr_id, is_queryable, is_modifiable, subject_id, "
                            + "subject_namespace, archetype_node_id, name, sys_version, change_type, "
                            + "committer_name, sys_tenant) "
                            + "VALUES (?::uuid, true, true, 'patient-status', 'ehr.test.org', "
                            + "'openEHR-EHR-EHR_STATUS.generic.v1', 'EHR Status', 1, 'creation', "
                            + "'test-committer', 1) RETURNING id, sys_version, change_type");
            ps.setString(1, ehrId);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id"))).isNotNull();
            assertThat(rs.getInt("sys_version")).isEqualTo(1);
            assertThat(rs.getString("change_type")).isEqualTo("creation");
        }
    }

    @Test
    void updateEhrStatus() throws Exception {
        try (Connection conn = connect()) {
            // Create EHR
            ResultSet ehrRs = conn.createStatement()
                    .executeQuery(
                            "INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                                    + "VALUES ('patient-update', 'ehr.test.org', 1) RETURNING id");
            ehrRs.next();
            String ehrId = ehrRs.getString("id");

            // Create EHR Status v1
            PreparedStatement psV1 = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_status (ehr_id, is_queryable, is_modifiable, subject_id, "
                            + "subject_namespace, archetype_node_id, name, sys_version, change_type, "
                            + "committer_name, sys_tenant) "
                            + "VALUES (?::uuid, true, true, 'patient-update', 'ehr.test.org', "
                            + "'openEHR-EHR-EHR_STATUS.generic.v1', 'EHR Status', 1, 'creation', "
                            + "'committer-a', 1) RETURNING id, valid_period");
            psV1.setString(1, ehrId);
            ResultSet rsV1 = psV1.executeQuery();
            rsV1.next();
            String statusId = rsV1.getString("id");

            // Archive v1 to history with closed valid_period
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.ehr_status_history "
                            + "SELECT id, ehr_id, tstzrange(lower(valid_period), now()), "
                            + "is_queryable, is_modifiable, subject_id, subject_namespace, "
                            + "archetype_node_id, name, sys_version, contribution_id, change_type, "
                            + "committed_at, committer_name, committer_id, sys_tenant "
                            + "FROM ehr_system.ehr_status WHERE id = '" + statusId + "'");

            // Delete v1 from current
            conn.createStatement()
                    .execute("DELETE FROM ehr_system.ehr_status WHERE id = '" + statusId + "'");

            // Insert v2
            PreparedStatement psV2 = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_status (id, ehr_id, is_queryable, is_modifiable, subject_id, "
                            + "subject_namespace, archetype_node_id, name, sys_version, change_type, "
                            + "committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, false, true, 'patient-update', 'ehr.test.org', "
                            + "'openEHR-EHR-EHR_STATUS.generic.v1', 'EHR Status', 2, 'modification', "
                            + "'committer-b', 1) RETURNING sys_version, is_queryable");
            psV2.setString(1, statusId);
            psV2.setString(2, ehrId);
            ResultSet rsV2 = psV2.executeQuery();

            assertThat(rsV2.next()).isTrue();
            assertThat(rsV2.getInt("sys_version")).isEqualTo(2);
            assertThat(rsV2.getBoolean("is_queryable")).isFalse();

            // Verify v1 in history
            ResultSet histRs = conn.createStatement()
                    .executeQuery("SELECT sys_version, is_queryable, upper_inf(valid_period) AS open "
                            + "FROM ehr_system.ehr_status_history WHERE id = '" + statusId + "'");
            assertThat(histRs.next()).isTrue();
            assertThat(histRs.getInt("sys_version")).isEqualTo(1);
            assertThat(histRs.getBoolean("is_queryable")).isTrue();
            assertThat(histRs.getBoolean("open")).isFalse();
        }
    }

    @Test
    void findEhrBySubject() throws Exception {
        String uniqueSubject = "patient-find-" + UUID.randomUUID();
        try (Connection conn = connect()) {
            // Create EHR with unique subject
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                            + "VALUES ('" + uniqueSubject + "', 'ehr.find.org', 1)");

            // Query by subject
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, subject_id, subject_namespace FROM ehr_system.ehr "
                            + "WHERE subject_id = ? AND subject_namespace = ?");
            ps.setString(1, uniqueSubject);
            ps.setString(2, "ehr.find.org");
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("subject_id")).isEqualTo(uniqueSubject);
            assertThat(rs.getString("subject_namespace")).isEqualTo("ehr.find.org");
        }
    }

    @Test
    void ehrNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        try (Connection conn = connect()) {
            PreparedStatement ps =
                    conn.prepareStatement("SELECT id FROM ehr_system.ehr WHERE id = ?::uuid");
            ps.setString(1, nonExistentId.toString());
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isFalse();
        }
    }
}
