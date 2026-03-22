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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.ehrbase.test.EhrbasePostgreSQLContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for emergency/break-glass access audit events.
 * Connects directly to PG18 testcontainer -- no Spring context needed.
 */
class BreakGlassIT {

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
    void emergencyAccessCreatesAuditEvent() throws Exception {
        try (Connection conn = connect()) {
            UUID targetId = UUID.randomUUID();
            String justification = "Patient in cardiac arrest, immediate access required";

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.audit_event (event_type, target_type, target_id, action, "
                            + "actor_id, actor_role, justification, details, tenant_id) "
                            + "VALUES ('emergency_access', 'ehr', ?::uuid, 'emergency_override', "
                            + "'emergency-doc-1', 'emergency_physician', ?, "
                            + "'{\"reason_code\": \"CARDIAC_ARREST\", \"department\": \"ER\"}'::jsonb, 1) "
                            + "RETURNING id, event_type, action, justification, details");
            ps.setString(1, targetId.toString());
            ps.setString(2, justification);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id")).version()).isEqualTo(7);
            assertThat(rs.getString("event_type")).isEqualTo("emergency_access");
            assertThat(rs.getString("action")).isEqualTo("emergency_override");
            assertThat(rs.getString("justification")).isEqualTo(justification);

            String details = rs.getString("details");
            assertThat(details).contains("CARDIAC_ARREST");
            assertThat(details).contains("ER");
        }
    }

    @Test
    void emergencyAccessWithoutJustification() throws Exception {
        try (Connection conn = connect()) {
            UUID targetId = UUID.randomUUID();

            // Insert emergency access event with null justification and null details
            // Enforcement of mandatory justification is in the application layer
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.audit_event (event_type, target_type, target_id, action, "
                            + "actor_id, actor_role, justification, details, tenant_id) "
                            + "VALUES ('emergency_access', 'ehr', ?::uuid, 'emergency_override', "
                            + "'no-justification-doc', 'physician', NULL, NULL, 1) "
                            + "RETURNING id, event_type, justification, details");
            ps.setString(1, targetId.toString());
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("event_type")).isEqualTo("emergency_access");
            assertThat(rs.getString("justification")).isNull();
            assertThat(rs.getString("details")).isNull();
        }
    }

    @Test
    void emergencyAccessQueryable() throws Exception {
        try (Connection conn = connect()) {
            String uniqueActor = "break-glass-actor-" + UUID.randomUUID();

            // Insert multiple emergency access events
            for (int i = 0; i < 3; i++) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ehr_system.audit_event (event_type, target_type, target_id, action, "
                                + "actor_id, actor_role, justification, tenant_id) "
                                + "VALUES ('emergency_access', 'ehr', ?::uuid, 'emergency_override', "
                                + "?, 'emergency_physician', 'Emergency case #" + i + "', 1) "
                                + "RETURNING id");
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, uniqueActor);
                ps.executeQuery();
            }

            // Insert a non-emergency event by the same actor
            PreparedStatement psNormal =
                    conn.prepareStatement("INSERT INTO ehr_system.audit_event (event_type, target_type, action, "
                            + "actor_id, actor_role, tenant_id) "
                            + "VALUES ('data_access', 'ehr', 'read', ?, 'emergency_physician', 1) "
                            + "RETURNING id");
            psNormal.setString(1, uniqueActor);
            psNormal.executeQuery();

            // Query only emergency_access events for this actor
            PreparedStatement psQuery =
                    conn.prepareStatement("SELECT id, event_type, justification FROM ehr_system.audit_event "
                            + "WHERE event_type = 'emergency_access' AND actor_id = ? "
                            + "ORDER BY created_at");
            psQuery.setString(1, uniqueActor);
            ResultSet rs = psQuery.executeQuery();

            List<String> justifications = new ArrayList<>();
            while (rs.next()) {
                assertThat(rs.getString("event_type")).isEqualTo("emergency_access");
                justifications.add(rs.getString("justification"));
            }

            assertThat(justifications).hasSize(3);
            assertThat(justifications).containsExactly("Emergency case #0", "Emergency case #1", "Emergency case #2");

            // Verify through the v_audit_trail view as well
            PreparedStatement psView = conn.prepareStatement("SELECT count(*) AS cnt FROM ehr_views.v_audit_trail "
                    + "WHERE event_type = 'emergency_access' AND actor_id = ?");
            psView.setString(1, uniqueActor);
            ResultSet viewRs = psView.executeQuery();
            assertThat(viewRs.next()).isTrue();
            assertThat(viewRs.getInt("cnt")).isEqualTo(3);

            // Verify the label is correct
            PreparedStatement psLabel =
                    conn.prepareStatement("SELECT DISTINCT event_type_label FROM ehr_views.v_audit_trail "
                            + "WHERE event_type = 'emergency_access' AND actor_id = ?");
            psLabel.setString(1, uniqueActor);
            ResultSet labelRs = psLabel.executeQuery();
            assertThat(labelRs.next()).isTrue();
            assertThat(labelRs.getString("event_type_label")).isEqualTo("Emergency/Break-Glass Access");
        }
    }
}
