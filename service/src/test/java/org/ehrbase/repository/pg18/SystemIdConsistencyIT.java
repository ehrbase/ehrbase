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
 * Integration tests for ehr_system.system table and version UID format consistency.
 * Connects directly to PG18 testcontainer -- no Spring context needed.
 */
class SystemIdConsistencyIT {

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
    void systemTableHasEntry() throws Exception {
        try (Connection conn = connect()) {
            // Verify system table exists and can store a system_id
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.system (system_id) " + "VALUES (?) RETURNING id, system_id, created_at");
            String systemId = "ehrbase.test.local." + UUID.randomUUID();
            ps.setString(1, systemId);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id"))).isNotNull();
            assertThat(rs.getString("system_id")).isEqualTo(systemId);
            assertThat(rs.getTimestamp("created_at")).isNotNull();
        }
    }

    @Test
    void versionUidFormat() throws Exception {
        try (Connection conn = connect()) {
            // Insert a system record
            String systemId = "ehrbase.version-uid.test." + UUID.randomUUID();
            conn.createStatement().execute("INSERT INTO ehr_system.system (system_id) VALUES ('" + systemId + "')");

            // Create an EHR and template for composition
            ResultSet ehrRs = conn.createStatement()
                    .executeQuery("INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                            + "VALUES ('sysid-patient-" + UUID.randomUUID() + "', 'ehr.sysid.org', 1) RETURNING id");
            ehrRs.next();
            String ehrId = ehrRs.getString("id");

            String templateUnique = "test.sysid." + UUID.randomUUID();
            PreparedStatement tps =
                    conn.prepareStatement("INSERT INTO ehr_system.template (template_id, content, sys_tenant) "
                            + "VALUES (?, '<template/>', 1) RETURNING id");
            tps.setString(1, templateUnique);
            ResultSet tplRs = tps.executeQuery();
            tplRs.next();
            String templateId = tplRs.getString("id");

            // Create composition
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. Smith', 1, 'creation', 'committer', 1) RETURNING id, sys_version");
            ps.setString(1, ehrId);
            ps.setString(2, templateId);
            ResultSet compRs = ps.executeQuery();
            compRs.next();
            String compUuid = compRs.getString("id");
            int version = compRs.getInt("sys_version");

            // Construct version UID in openEHR format: {uuid}::{system_id}::{version}
            String versionUid = compUuid + "::" + systemId + "::" + version;

            // Verify format is valid: 3 parts separated by ::
            String[] parts = versionUid.split("::");
            assertThat(parts).hasSize(3);
            assertThat(UUID.fromString(parts[0])).isNotNull();
            assertThat(parts[1]).isEqualTo(systemId);
            assertThat(Integer.parseInt(parts[2])).isEqualTo(1);
        }
    }

    @Test
    void systemIdPersisted() throws Exception {
        try (Connection conn = connect()) {
            String systemId = "ehrbase.persisted.test." + UUID.randomUUID();

            // Insert system record
            PreparedStatement psInsert =
                    conn.prepareStatement("INSERT INTO ehr_system.system (system_id) VALUES (?) RETURNING id");
            psInsert.setString(1, systemId);
            ResultSet insertRs = psInsert.executeQuery();
            insertRs.next();
            String id = insertRs.getString("id");

            // Query back and verify
            PreparedStatement psQuery =
                    conn.prepareStatement("SELECT system_id, created_at FROM ehr_system.system WHERE id = ?::uuid");
            psQuery.setString(1, id);
            ResultSet queryRs = psQuery.executeQuery();

            assertThat(queryRs.next()).isTrue();
            assertThat(queryRs.getString("system_id")).isEqualTo(systemId);
            assertThat(queryRs.getTimestamp("created_at")).isNotNull();
        }
    }
}
