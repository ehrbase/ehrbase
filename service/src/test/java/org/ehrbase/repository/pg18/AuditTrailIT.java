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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.ehrbase.test.EhrbasePostgreSQLContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for ehr_system.audit_event table immutability and hash chain.
 * Connects directly to PG18 testcontainer — no Spring context needed.
 */
class AuditTrailIT {

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

    /**
     * Sets up permissions for ehrbase_restricted role and switches to it.
     * Requires a setup step as the owner before switching roles.
     */
    private Connection connectAsRestricted() throws Exception {
        // First: setup permissions as owner
        try (Connection setup = DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())) {
            setup.createStatement().execute("GRANT USAGE ON SCHEMA ehr_system TO ehrbase_restricted");
            setup.createStatement().execute("GRANT INSERT ON ehr_system.audit_event TO ehrbase_restricted");
        }
        // Now connect and switch to restricted role
        Connection conn = DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        conn.createStatement().execute("SET ROLE ehrbase_restricted");
        conn.createStatement().execute("SET ehrbase.current_tenant = '1'");
        return conn;
    }

    @Test
    void createAuditEvent() throws Exception {
        UUID targetId = UUID.randomUUID();
        try (Connection conn = connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.audit_event (event_type, target_type, target_id, action, "
                            + "actor_id, actor_role, tenant_id) "
                            + "VALUES ('data_access', 'composition', ?::uuid, 'read', "
                            + "'user-1', 'clinician', 1) "
                            + "RETURNING id, event_type, target_type, target_id, action, actor_id, actor_role, created_at");
            ps.setString(1, targetId.toString());
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id")).version()).isEqualTo(7);
            assertThat(rs.getString("event_type")).isEqualTo("data_access");
            assertThat(rs.getString("target_type")).isEqualTo("composition");
            assertThat(rs.getString("target_id")).isEqualTo(targetId.toString());
            assertThat(rs.getString("action")).isEqualTo("read");
            assertThat(rs.getString("actor_id")).isEqualTo("user-1");
            assertThat(rs.getString("actor_role")).isEqualTo("clinician");
            assertThat(rs.getTimestamp("created_at")).isNotNull();
        }
    }

    @org.junit.jupiter.api.Disabled(
            "Requires production multi-role setup (ehrbase owner + ehrbase_restricted app role)")
    @Test
    void auditEventImmutable() throws Exception {
        try (Connection conn = connectAsRestricted()) {
            // Insert an audit event first
            PreparedStatement ps =
                    conn.prepareStatement("INSERT INTO ehr_system.audit_event (event_type, target_type, action, "
                            + "actor_id, actor_role, tenant_id) "
                            + "VALUES ('data_access', 'ehr', 'read', 'user-imm', 'clinician', 1) RETURNING id");
            ResultSet rs = ps.executeQuery();
            rs.next();
            String auditId = rs.getString("id");

            // Attempt UPDATE — should be denied for ehrbase_restricted
            assertThatThrownBy(() -> conn.createStatement()
                            .execute(
                                    "UPDATE ehr_system.audit_event SET action = 'delete' WHERE id = '" + auditId + "'"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("permission denied");
        }
    }

    @org.junit.jupiter.api.Disabled(
            "Requires production multi-role setup (ehrbase owner + ehrbase_restricted app role)")
    @Test
    void auditEventNoDelete() throws Exception {
        try (Connection conn = connectAsRestricted()) {
            // Insert an audit event
            PreparedStatement ps =
                    conn.prepareStatement("INSERT INTO ehr_system.audit_event (event_type, target_type, action, "
                            + "actor_id, actor_role, tenant_id) "
                            + "VALUES ('data_modify', 'composition', 'create', 'user-nodel', 'clinician', 1) "
                            + "RETURNING id");
            ResultSet rs = ps.executeQuery();
            rs.next();
            String auditId = rs.getString("id");

            // Attempt DELETE — should be denied for ehrbase_restricted
            assertThatThrownBy(() -> conn.createStatement()
                            .execute("DELETE FROM ehr_system.audit_event WHERE id = '" + auditId + "'"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("permission denied");
        }
    }

    @Test
    void auditPrevHashChain() throws Exception {
        try (Connection conn = connect()) {
            String hash0 = "genesis";
            String hash1 = "sha256-aaa111";
            String hash2 = "sha256-bbb222";

            // Event 1: no prev_hash (genesis)
            PreparedStatement ps1 =
                    conn.prepareStatement("INSERT INTO ehr_system.audit_event (event_type, target_type, action, "
                            + "actor_id, actor_role, tenant_id, prev_hash) "
                            + "VALUES ('data_access', 'ehr', 'read', 'chain-user', 'clinician', 1, ?) "
                            + "RETURNING id");
            ps1.setString(1, hash0);
            ResultSet rs1 = ps1.executeQuery();
            rs1.next();
            String id1 = rs1.getString("id");

            // Event 2: prev_hash = hash1 (points to hash of event 1)
            PreparedStatement ps2 =
                    conn.prepareStatement("INSERT INTO ehr_system.audit_event (event_type, target_type, action, "
                            + "actor_id, actor_role, tenant_id, prev_hash) "
                            + "VALUES ('data_modify', 'composition', 'create', 'chain-user', 'clinician', 1, ?) "
                            + "RETURNING id");
            ps2.setString(1, hash1);
            ResultSet rs2 = ps2.executeQuery();
            rs2.next();
            String id2 = rs2.getString("id");

            // Event 3: prev_hash = hash2 (points to hash of event 2)
            PreparedStatement ps3 =
                    conn.prepareStatement("INSERT INTO ehr_system.audit_event (event_type, target_type, action, "
                            + "actor_id, actor_role, tenant_id, prev_hash) "
                            + "VALUES ('data_access', 'ehr', 'read', 'chain-user', 'admin', 1, ?) "
                            + "RETURNING id");
            ps3.setString(1, hash2);
            ResultSet rs3 = ps3.executeQuery();
            rs3.next();
            String id3 = rs3.getString("id");

            // Verify chain: read all 3 events and check prev_hash values
            ResultSet chainRs = conn.createStatement()
                    .executeQuery("SELECT id, prev_hash FROM ehr_system.audit_event "
                            + "WHERE id IN ('" + id1 + "', '" + id2 + "', '" + id3 + "') "
                            + "ORDER BY created_at");

            assertThat(chainRs.next()).isTrue();
            assertThat(chainRs.getString("prev_hash")).isEqualTo(hash0);

            assertThat(chainRs.next()).isTrue();
            assertThat(chainRs.getString("prev_hash")).isEqualTo(hash1);

            assertThat(chainRs.next()).isTrue();
            assertThat(chainRs.getString("prev_hash")).isEqualTo(hash2);
        }
    }
}
