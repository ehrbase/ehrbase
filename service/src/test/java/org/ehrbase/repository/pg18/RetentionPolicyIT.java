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
 * Integration tests for ehr_system.retention_policy table.
 * Connects directly to PG18 testcontainer -- no Spring context needed.
 */
class RetentionPolicyIT {

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
    void createRetentionPolicy() throws Exception {
        try (Connection conn = connect()) {
            String policyName = "test-retention-" + UUID.randomUUID();

            PreparedStatement ps = conn.prepareStatement("INSERT INTO ehr_system.retention_policy "
                    + "(policy_name, retention_period, applies_to, action, requires_approval, sys_tenant) "
                    + "VALUES (?, '3 years'::interval, 'audit_event', 'archive', true, 1) "
                    + "RETURNING id, policy_name, retention_period, applies_to, action, "
                    + "requires_approval, created_at");
            ps.setString(1, policyName);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id")).version()).isEqualTo(7);
            assertThat(rs.getString("policy_name")).isEqualTo(policyName);
            assertThat(rs.getString("applies_to")).isEqualTo("audit_event");
            assertThat(rs.getString("action")).isEqualTo("archive");
            assertThat(rs.getBoolean("requires_approval")).isTrue();
            assertThat(rs.getTimestamp("created_at")).isNotNull();

            // Verify interval stored correctly by extracting epoch
            PreparedStatement psInterval = conn.prepareStatement("SELECT EXTRACT(year FROM retention_period) AS years "
                    + "FROM ehr_system.retention_policy WHERE policy_name = ?");
            psInterval.setString(1, policyName);
            ResultSet intervalRs = psInterval.executeQuery();
            assertThat(intervalRs.next()).isTrue();
            assertThat(intervalRs.getInt("years")).isEqualTo(3);
        }
    }

    @Test
    void sixYearRetention() throws Exception {
        try (Connection conn = connect()) {
            String policyName = "six-year-retention-" + UUID.randomUUID();

            // Create a 6-year retention policy (common for medical records)
            PreparedStatement psPolicy = conn.prepareStatement("INSERT INTO ehr_system.retention_policy "
                    + "(policy_name, retention_period, applies_to, action, requires_approval, "
                    + "approved_by, approved_at, sys_tenant) "
                    + "VALUES (?, '6 years'::interval, 'all', 'pseudonymize', true, "
                    + "'admin-approver', now(), 1) "
                    + "RETURNING id, retention_period, approved_by, approved_at");
            psPolicy.setString(1, policyName);
            ResultSet policyRs = psPolicy.executeQuery();
            assertThat(policyRs.next()).isTrue();
            assertThat(policyRs.getString("approved_by")).isEqualTo("admin-approver");
            assertThat(policyRs.getTimestamp("approved_at")).isNotNull();

            // Insert an audit event
            ResultSet auditInsertRs = conn.createStatement()
                    .executeQuery("INSERT INTO ehr_system.audit_event "
                            + "(event_type, target_type, action, actor_id, actor_role, tenant_id) "
                            + "VALUES ('data_access', 'ehr', 'read', 'retention-user', 'clinician', 1) "
                            + "RETURNING id");
            assertThat(auditInsertRs.next()).isTrue();
            String auditId = auditInsertRs.getString(1);

            // Verify audit event is queryable (retention is not enforced at DB level)
            ResultSet auditRs = conn.createStatement()
                    .executeQuery("SELECT id FROM ehr_system.audit_event " + "WHERE actor_id = 'retention-user'");

            // The audit_event query returns at least one row for 'retention-user'
            // (we just need to confirm it does not get deleted -- policy is metadata only)
            assertThat(auditRs.next()).isTrue();

            // Verify retention policy is stored and queryable
            PreparedStatement psQuery =
                    conn.prepareStatement("SELECT policy_name, EXTRACT(year FROM retention_period) AS years, "
                            + "applies_to, action, requires_approval "
                            + "FROM ehr_system.retention_policy WHERE policy_name = ?");
            psQuery.setString(1, policyName);
            ResultSet queryRs = psQuery.executeQuery();

            assertThat(queryRs.next()).isTrue();
            assertThat(queryRs.getInt("years")).isEqualTo(6);
            assertThat(queryRs.getString("applies_to")).isEqualTo("all");
            assertThat(queryRs.getString("action")).isEqualTo("pseudonymize");
            assertThat(queryRs.getBoolean("requires_approval")).isTrue();
        }
    }
}
