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
 * Integration tests for ehr_system.composition and ehr_system.composition_history tables.
 * Connects directly to PG18 testcontainer — no Spring context needed.
 */
class CompositionRepositoryIT {

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
     * Creates an EHR and template, returning their IDs as a two-element array [ehrId, templateId].
     */
    private String[] createEhrAndTemplate(Connection conn) throws Exception {
        ResultSet ehrRs = conn.createStatement()
                .executeQuery("INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                        + "VALUES ('comp-patient-" + UUID.randomUUID() + "', 'ehr.comp.org', 1) RETURNING id");
        ehrRs.next();
        String ehrId = ehrRs.getString("id");

        String templateUnique = "test.template." + UUID.randomUUID();
        PreparedStatement tps =
                conn.prepareStatement("INSERT INTO ehr_system.template (template_id, content, sys_tenant) "
                        + "VALUES (?, '<template/>', 1) RETURNING id");
        tps.setString(1, templateUnique);
        ResultSet tplRs = tps.executeQuery();
        tplRs.next();
        String templateId = tplRs.getString("id");

        return new String[] {ehrId, templateId};
    }

    @Test
    void createComposition() throws Exception {
        try (Connection conn = connect()) {
            String[] ids = createEhrAndTemplate(conn);
            String ehrId = ids[0];
            String templateId = ids[1];

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, composer_id, language, territory, category_code, "
                            + "sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. Smith', 'doc-1', 'en', 'US', '431', "
                            + "1, 'creation', 'test-committer', 1) "
                            + "RETURNING id, sys_version, archetype_id, template_name, composer_name");
            ps.setString(1, ehrId);
            ps.setString(2, templateId);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id")).version()).isEqualTo(7);
            assertThat(rs.getInt("sys_version")).isEqualTo(1);
            assertThat(rs.getString("archetype_id")).isEqualTo("openEHR-EHR-COMPOSITION.encounter.v1");
            assertThat(rs.getString("template_name")).isEqualTo("Encounter");
            assertThat(rs.getString("composer_name")).isEqualTo("Dr. Smith");
        }
    }

    @Test
    void updateComposition() throws Exception {
        try (Connection conn = connect()) {
            String[] ids = createEhrAndTemplate(conn);
            String ehrId = ids[0];
            String templateId = ids[1];

            // Create v1
            PreparedStatement psV1 = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. Smith', 1, 'creation', 'committer-a', 1) "
                            + "RETURNING id, valid_period");
            psV1.setString(1, ehrId);
            psV1.setString(2, templateId);
            ResultSet rsV1 = psV1.executeQuery();
            rsV1.next();
            String compId = rsV1.getString("id");

            // Archive v1 to history with closed valid_period
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.composition_history "
                            + "SELECT id, ehr_id, template_id, tstzrange(lower(valid_period), now()), "
                            + "archetype_id, template_name, composer_name, composer_id, language, "
                            + "territory, category_code, feeder_audit, participations, "
                            + "sys_version, contribution_id, change_type, committed_at, "
                            + "committer_name, committer_id, sys_tenant "
                            + "FROM ehr_system.composition WHERE id = '" + compId + "'");

            // Delete v1 from current
            conn.createStatement().execute("DELETE FROM ehr_system.composition WHERE id = '" + compId + "'");

            // Insert v2
            PreparedStatement psV2 = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (id, ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. Jones', 2, 'modification', 'committer-b', 1) "
                            + "RETURNING sys_version, composer_name");
            psV2.setString(1, compId);
            psV2.setString(2, ehrId);
            psV2.setString(3, templateId);
            ResultSet rsV2 = psV2.executeQuery();

            assertThat(rsV2.next()).isTrue();
            assertThat(rsV2.getInt("sys_version")).isEqualTo(2);
            assertThat(rsV2.getString("composer_name")).isEqualTo("Dr. Jones");

            // Verify v1 in history has closed valid_period
            ResultSet histRs = conn.createStatement()
                    .executeQuery("SELECT sys_version, upper_inf(valid_period) AS open "
                            + "FROM ehr_system.composition_history WHERE id = '" + compId + "'");
            assertThat(histRs.next()).isTrue();
            assertThat(histRs.getInt("sys_version")).isEqualTo(1);
            assertThat(histRs.getBoolean("open")).isFalse();
        }
    }

    @Test
    void deleteComposition() throws Exception {
        try (Connection conn = connect()) {
            String[] ids = createEhrAndTemplate(conn);
            String ehrId = ids[0];
            String templateId = ids[1];

            // Create composition
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. Smith', 1, 'creation', 'committer-a', 1) RETURNING id");
            ps.setString(1, ehrId);
            ps.setString(2, templateId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            String compId = rs.getString("id");

            // Archive to history with change_type='deleted' and closed valid_period
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.composition_history "
                            + "SELECT id, ehr_id, template_id, tstzrange(lower(valid_period), now()), "
                            + "archetype_id, template_name, composer_name, composer_id, language, "
                            + "territory, category_code, feeder_audit, participations, "
                            + "sys_version, contribution_id, 'deleted', committed_at, "
                            + "committer_name, committer_id, sys_tenant "
                            + "FROM ehr_system.composition WHERE id = '" + compId + "'");

            // Remove from current table
            conn.createStatement().execute("DELETE FROM ehr_system.composition WHERE id = '" + compId + "'");

            // Verify gone from current
            ResultSet currentRs = conn.createStatement()
                    .executeQuery("SELECT id FROM ehr_system.composition WHERE id = '" + compId + "'");
            assertThat(currentRs.next()).isFalse();

            // Verify in history with change_type='deleted'
            ResultSet histRs = conn.createStatement()
                    .executeQuery("SELECT change_type, upper_inf(valid_period) AS open "
                            + "FROM ehr_system.composition_history WHERE id = '" + compId + "'");
            assertThat(histRs.next()).isTrue();
            assertThat(histRs.getString("change_type")).isEqualTo("deleted");
            assertThat(histRs.getBoolean("open")).isFalse();
        }
    }

    @Test
    void getCompositionByVersion() throws Exception {
        try (Connection conn = connect()) {
            String[] ids = createEhrAndTemplate(conn);
            String ehrId = ids[0];
            String templateId = ids[1];

            // Create v1
            PreparedStatement psV1 = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. V1', 1, 'creation', 'committer-a', 1) RETURNING id");
            psV1.setString(1, ehrId);
            psV1.setString(2, templateId);
            ResultSet rsV1 = psV1.executeQuery();
            rsV1.next();
            String compId = rsV1.getString("id");

            // Archive v1 to history
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.composition_history "
                            + "SELECT id, ehr_id, template_id, tstzrange(lower(valid_period), now()), "
                            + "archetype_id, template_name, composer_name, composer_id, language, "
                            + "territory, category_code, feeder_audit, participations, "
                            + "sys_version, contribution_id, change_type, committed_at, "
                            + "committer_name, committer_id, sys_tenant "
                            + "FROM ehr_system.composition WHERE id = '" + compId + "'");

            // Delete v1 from current, insert v2
            conn.createStatement().execute("DELETE FROM ehr_system.composition WHERE id = '" + compId + "'");
            PreparedStatement psV2 = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (id, ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. V2', 2, 'modification', 'committer-b', 1)");
            psV2.setString(1, compId);
            psV2.setString(2, ehrId);
            psV2.setString(3, templateId);
            psV2.execute();

            // Query v1 from history
            ResultSet histRs = conn.createStatement()
                    .executeQuery("SELECT composer_name, sys_version FROM ehr_system.composition_history "
                            + "WHERE id = '" + compId + "' AND sys_version = 1");
            assertThat(histRs.next()).isTrue();
            assertThat(histRs.getString("composer_name")).isEqualTo("Dr. V1");
            assertThat(histRs.getInt("sys_version")).isEqualTo(1);

            // Query v2 from current
            ResultSet currRs = conn.createStatement()
                    .executeQuery("SELECT composer_name, sys_version FROM ehr_system.composition " + "WHERE id = '"
                            + compId + "'");
            assertThat(currRs.next()).isTrue();
            assertThat(currRs.getString("composer_name")).isEqualTo("Dr. V2");
            assertThat(currRs.getInt("sys_version")).isEqualTo(2);
        }
    }

    @Test
    void optimisticLockingViolation() throws Exception {
        try (Connection conn = connect()) {
            String[] ids = createEhrAndTemplate(conn);
            String ehrId = ids[0];
            String templateId = ids[1];

            // Create v1 with open-ended valid_period (default)
            PreparedStatement psV1 = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. Smith', 1, 'creation', 'committer-a', 1) RETURNING id");
            psV1.setString(1, ehrId);
            psV1.setString(2, templateId);
            ResultSet rsV1 = psV1.executeQuery();
            rsV1.next();
            String compId = rsV1.getString("id");

            // Try to insert v2 with overlapping open-ended valid_period (v1 not archived)
            // WITHOUT OVERLAPS constraint should reject this
            PreparedStatement psV2 = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (id, ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. Jones', 2, 'modification', 'committer-b', 1)");
            psV2.setString(1, compId);
            psV2.setString(2, ehrId);
            psV2.setString(3, templateId);

            assertThatThrownBy(psV2::execute)
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("conflicting key value");
        }
    }
}
