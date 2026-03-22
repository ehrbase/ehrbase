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
 * Integration tests for compliance views: v_audit_trail, v_access_log, v_folder_tree.
 * Connects directly to PG18 testcontainer -- no Spring context needed.
 */
class ComplianceViewsIT {

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
                        + "VALUES ('views-patient-" + UUID.randomUUID() + "', 'ehr.views.org', 1) RETURNING id");
        rs.next();
        return rs.getString("id");
    }

    private String insertAuditEvent(
            Connection conn, String eventType, String targetType, String action, String actorId, String actorRole)
            throws Exception {
        PreparedStatement ps =
                conn.prepareStatement("INSERT INTO ehr_system.audit_event (event_type, target_type, action, "
                        + "actor_id, actor_role, tenant_id) "
                        + "VALUES (?, ?, ?, ?, ?, 1) RETURNING id");
        ps.setString(1, eventType);
        ps.setString(2, targetType);
        ps.setString(3, action);
        ps.setString(4, actorId);
        ps.setString(5, actorRole);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getString("id");
    }

    @Test
    void auditTrailViewQueryable() throws Exception {
        try (Connection conn = connect()) {
            // Insert audit events
            String id1 = insertAuditEvent(conn, "data_access", "ehr", "read", "view-user-1", "clinician");
            String id2 = insertAuditEvent(conn, "data_modify", "composition", "create", "view-user-2", "admin");

            // Query through the view
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT audit_id, event_type, event_type_label, action, action_label, "
                            + "actor_id, actor_role "
                            + "FROM ehr_views.v_audit_trail "
                            + "WHERE audit_id IN ('" + id1 + "', '" + id2 + "') "
                            + "ORDER BY created_at");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("audit_id")).isEqualTo(id1);
            assertThat(rs.getString("event_type")).isEqualTo("data_access");
            assertThat(rs.getString("event_type_label")).isEqualTo("Data Access");
            assertThat(rs.getString("action")).isEqualTo("read");
            assertThat(rs.getString("action_label")).isEqualTo("Read");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("audit_id")).isEqualTo(id2);
            assertThat(rs.getString("event_type")).isEqualTo("data_modify");
            assertThat(rs.getString("event_type_label")).isEqualTo("Data Modification");
            assertThat(rs.getString("action")).isEqualTo("create");
            assertThat(rs.getString("action_label")).isEqualTo("Created");
        }
    }

    @Test
    void accessLogViewQueryable() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);
            UUID targetId = UUID.fromString(ehrId);

            // Insert a data_access event targeting the EHR
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.audit_event (event_type, target_type, target_id, action, "
                            + "actor_id, actor_role, tenant_id) "
                            + "VALUES ('data_access', 'ehr', ?::uuid, 'read', 'access-log-user', 'clinician', 1) "
                            + "RETURNING id");
            ps.setString(1, ehrId);
            ResultSet insertRs = ps.executeQuery();
            insertRs.next();
            String auditId = insertRs.getString("id");

            // Query through the access log view
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT access_event_id, accessor_id, accessor_role, resource_type, "
                            + "resource_id, action, ehr_id "
                            + "FROM ehr_views.v_access_log "
                            + "WHERE access_event_id = '" + auditId + "'");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("accessor_id")).isEqualTo("access-log-user");
            assertThat(rs.getString("accessor_role")).isEqualTo("clinician");
            assertThat(rs.getString("resource_type")).isEqualTo("ehr");
            assertThat(rs.getString("action")).isEqualTo("read");
            assertThat(rs.getString("ehr_id")).isEqualTo(ehrId);
        }
    }

    @Test
    void folderTreeViewQueryable() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);

            // Insert folders with ltree paths
            PreparedStatement psRoot =
                    conn.prepareStatement("INSERT INTO ehr_system.ehr_folder (ehr_id, path, name, archetype_node_id, "
                            + "sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, 'root'::ext.ltree, 'Root', 'at0001', 1, 'creation', "
                            + "'test-committer', 1) RETURNING id");
            psRoot.setString(1, ehrId);
            ResultSet rootRs = psRoot.executeQuery();
            rootRs.next();
            String rootId = rootRs.getString("id");

            PreparedStatement psChild = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_folder (ehr_id, parent_id, path, name, archetype_node_id, "
                            + "sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 'root.labs'::ext.ltree, 'Lab Results', 'at0002', "
                            + "1, 'creation', 'test-committer', 1) RETURNING id");
            psChild.setString(1, ehrId);
            psChild.setString(2, rootId);
            ResultSet childRs = psChild.executeQuery();
            childRs.next();
            String childId = childRs.getString("id");

            PreparedStatement psGrandchild = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_folder (ehr_id, parent_id, path, name, archetype_node_id, "
                            + "sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 'root.labs.blood'::ext.ltree, 'Blood Tests', 'at0003', "
                            + "1, 'creation', 'test-committer', 1) RETURNING id");
            psGrandchild.setString(1, ehrId);
            psGrandchild.setString(2, childId);
            psGrandchild.executeQuery();

            // Query through the folder tree view
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT folder_id, folder_name, depth, path_text "
                            + "FROM ehr_views.v_folder_tree "
                            + "WHERE ehr_id = '" + ehrId + "' "
                            + "ORDER BY depth");

            List<String> names = new ArrayList<>();
            List<Integer> depths = new ArrayList<>();
            while (rs.next()) {
                names.add(rs.getString("folder_name"));
                depths.add(rs.getInt("depth"));
            }

            assertThat(names).containsExactly("Root", "Lab Results", "Blood Tests");
            assertThat(depths).containsExactly(1, 2, 3);
        }
    }

    @Test
    void auditTrailFilterByDateRange() throws Exception {
        try (Connection conn = connect()) {
            // Insert events at different times using explicit timestamps
            String uniqueActor = "date-range-actor-" + UUID.randomUUID();

            conn.createStatement()
                    .executeQuery("INSERT INTO ehr_system.audit_event "
                            + "(event_type, target_type, action, actor_id, actor_role, tenant_id, created_at) "
                            + "VALUES ('data_access', 'ehr', 'read', '" + uniqueActor + "', 'clinician', 1, "
                            + "'2025-06-01 10:00:00+00') RETURNING id")
                    .next();

            conn.createStatement()
                    .executeQuery("INSERT INTO ehr_system.audit_event "
                            + "(event_type, target_type, action, actor_id, actor_role, tenant_id, created_at) "
                            + "VALUES ('data_modify', 'composition', 'create', '" + uniqueActor + "', 'clinician', 1, "
                            + "'2025-07-15 10:00:00+00') RETURNING id")
                    .next();

            conn.createStatement()
                    .executeQuery("INSERT INTO ehr_system.audit_event "
                            + "(event_type, target_type, action, actor_id, actor_role, tenant_id, created_at) "
                            + "VALUES ('data_access', 'ehr', 'read', '" + uniqueActor + "', 'admin', 1, "
                            + "'2025-09-01 10:00:00+00') RETURNING id")
                    .next();

            // Query the view with date range filter: July only
            PreparedStatement ps =
                    conn.prepareStatement("SELECT audit_id, event_type, action FROM ehr_views.v_audit_trail "
                            + "WHERE actor_id = ? "
                            + "AND created_at >= '2025-07-01'::timestamptz "
                            + "AND created_at < '2025-08-01'::timestamptz");
            ps.setString(1, uniqueActor);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("event_type")).isEqualTo("data_modify");
            assertThat(rs.getString("action")).isEqualTo("create");
            assertThat(rs.next()).isFalse();
        }
    }

    @Test
    void auditTrailFilterByActor() throws Exception {
        try (Connection conn = connect()) {
            String actor1 = "actor-filter-1-" + UUID.randomUUID();
            String actor2 = "actor-filter-2-" + UUID.randomUUID();

            // Insert events by different actors
            insertAuditEvent(conn, "data_access", "ehr", "read", actor1, "clinician");
            insertAuditEvent(conn, "data_access", "ehr", "read", actor1, "clinician");
            insertAuditEvent(conn, "data_modify", "composition", "create", actor2, "admin");

            // Filter by actor1
            PreparedStatement ps1 =
                    conn.prepareStatement("SELECT count(*) AS cnt FROM ehr_views.v_audit_trail WHERE actor_id = ?");
            ps1.setString(1, actor1);
            ResultSet rs1 = ps1.executeQuery();
            assertThat(rs1.next()).isTrue();
            assertThat(rs1.getInt("cnt")).isEqualTo(2);

            // Filter by actor2
            PreparedStatement ps2 =
                    conn.prepareStatement("SELECT count(*) AS cnt FROM ehr_views.v_audit_trail WHERE actor_id = ?");
            ps2.setString(1, actor2);
            ResultSet rs2 = ps2.executeQuery();
            assertThat(rs2.next()).isTrue();
            assertThat(rs2.getInt("cnt")).isEqualTo(1);
        }
    }
}
