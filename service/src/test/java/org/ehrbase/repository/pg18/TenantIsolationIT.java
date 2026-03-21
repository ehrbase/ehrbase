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
 * Integration tests for multi-tenant isolation via RLS policies.
 * Connects directly to PG18 testcontainer — no Spring context needed.
 */
class TenantIsolationIT {

    private static EhrbasePostgreSQLContainer pg;
    private static int secondTenantId;

    @BeforeAll
    static void startContainer() throws Exception {
        pg = EhrbasePostgreSQLContainer.sharedInstance();

        // Create a second tenant for isolation tests
        try (Connection conn =
                DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())) {
            // Use INSERT ... ON CONFLICT to be idempotent across test runs
            ResultSet rs = conn.createStatement()
                    .executeQuery(
                            "INSERT INTO ehr_system.tenant (name) VALUES ('test-tenant-2') "
                                    + "ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id");
            rs.next();
            secondTenantId = rs.getInt("id");
        }
    }

    private Connection connectWithTenant(int tenantId) throws Exception {
        Connection conn = DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        conn.createStatement().execute("SET ehrbase.current_tenant = '" + tenantId + "'");
        return conn;
    }

    @Test
    void createSecondTenant() throws Exception {
        assertThat(secondTenantId).isGreaterThan(1);

        try (Connection conn = connectWithTenant(1)) {
            // Verify both tenants exist (query without RLS since tenant table has no RLS)
            ResultSet rs = conn.createStatement()
                    .executeQuery(
                            "SELECT id, name FROM ehr_system.tenant WHERE name = 'test-tenant-2'");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("id")).isEqualTo(secondTenantId);
        }
    }

    @Test
    void ehrIsolationByTenant() throws Exception {
        String subjectTenant1 = "patient-t1-" + UUID.randomUUID();
        String subjectTenant2 = "patient-t2-" + UUID.randomUUID();

        // Create EHR in tenant 1
        try (Connection conn = connectWithTenant(1)) {
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                            + "VALUES ('" + subjectTenant1 + "', 'ehr.t1.org', 1)");
        }

        // Create EHR in tenant 2
        try (Connection conn = connectWithTenant(secondTenantId)) {
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                            + "VALUES ('" + subjectTenant2 + "', 'ehr.t2.org', " + secondTenantId + ")");
        }

        // Query from tenant 1 context: should see tenant 1 EHR, not tenant 2
        try (Connection conn = connectWithTenant(1)) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT subject_id FROM ehr_system.ehr WHERE subject_id IN (?, ?)");
            ps.setString(1, subjectTenant1);
            ps.setString(2, subjectTenant2);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("subject_id")).isEqualTo(subjectTenant1);
            assertThat(rs.next()).isFalse();
        }

        // Query from tenant 2 context: should see tenant 2 EHR, not tenant 1
        try (Connection conn = connectWithTenant(secondTenantId)) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT subject_id FROM ehr_system.ehr WHERE subject_id IN (?, ?)");
            ps.setString(1, subjectTenant1);
            ps.setString(2, subjectTenant2);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("subject_id")).isEqualTo(subjectTenant2);
            assertThat(rs.next()).isFalse();
        }
    }

    @Test
    void compositionIsolationByTenant() throws Exception {
        // Create EHR + template + composition in tenant 1
        String compArchetypeTenant1;
        try (Connection conn = connectWithTenant(1)) {
            ResultSet ehrRs = conn.createStatement()
                    .executeQuery(
                            "INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                                    + "VALUES ('comp-iso-t1-" + UUID.randomUUID() + "', 'ehr.iso.org', 1) "
                                    + "RETURNING id");
            ehrRs.next();
            String ehrId = ehrRs.getString("id");

            String tplUnique = "iso.template.t1." + UUID.randomUUID();
            PreparedStatement tps = conn.prepareStatement(
                    "INSERT INTO ehr_system.template (template_id, content, sys_tenant) "
                            + "VALUES (?, '<template/>', 1) RETURNING id");
            tps.setString(1, tplUnique);
            ResultSet tplRs = tps.executeQuery();
            tplRs.next();
            String templateId = tplRs.getString("id");

            compArchetypeTenant1 = "openEHR-COMPOSITION.iso-t1-" + UUID.randomUUID();
            PreparedStatement compPs = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, ?, 'IsoTemplate', 'Dr. T1', 1, 'creation', "
                            + "'committer-t1', 1) RETURNING id");
            compPs.setString(1, ehrId);
            compPs.setString(2, templateId);
            compPs.setString(3, compArchetypeTenant1);
            compPs.executeQuery();
        }

        // Create EHR + template + composition in tenant 2
        String compArchetypeTenant2;
        try (Connection conn = connectWithTenant(secondTenantId)) {
            ResultSet ehrRs = conn.createStatement()
                    .executeQuery(
                            "INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                                    + "VALUES ('comp-iso-t2-" + UUID.randomUUID() + "', 'ehr.iso.org', "
                                    + secondTenantId + ") RETURNING id");
            ehrRs.next();
            String ehrId = ehrRs.getString("id");

            String tplUnique = "iso.template.t2." + UUID.randomUUID();
            PreparedStatement tps = conn.prepareStatement(
                    "INSERT INTO ehr_system.template (template_id, content, sys_tenant) "
                            + "VALUES (?, '<template/>', " + secondTenantId + ") RETURNING id");
            tps.setString(1, tplUnique);
            ResultSet tplRs = tps.executeQuery();
            tplRs.next();
            String templateId = tplRs.getString("id");

            compArchetypeTenant2 = "openEHR-COMPOSITION.iso-t2-" + UUID.randomUUID();
            PreparedStatement compPs = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, ?, 'IsoTemplate', 'Dr. T2', 1, 'creation', "
                            + "'committer-t2', " + secondTenantId + ") RETURNING id");
            compPs.setString(1, ehrId);
            compPs.setString(2, templateId);
            compPs.setString(3, compArchetypeTenant2);
            compPs.executeQuery();
        }

        // Query from tenant 1: should only see tenant 1 composition
        try (Connection conn = connectWithTenant(1)) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT archetype_id FROM ehr_system.composition WHERE archetype_id IN (?, ?)");
            ps.setString(1, compArchetypeTenant1);
            ps.setString(2, compArchetypeTenant2);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("archetype_id")).isEqualTo(compArchetypeTenant1);
            assertThat(rs.next()).isFalse();
        }

        // Query from tenant 2: should only see tenant 2 composition
        try (Connection conn = connectWithTenant(secondTenantId)) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT archetype_id FROM ehr_system.composition WHERE archetype_id IN (?, ?)");
            ps.setString(1, compArchetypeTenant1);
            ps.setString(2, compArchetypeTenant2);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("archetype_id")).isEqualTo(compArchetypeTenant2);
            assertThat(rs.next()).isFalse();
        }
    }
}
