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
 * Integration tests for temporal queries on composition and composition_history tables.
 * Connects directly to PG18 testcontainer -- no Spring context needed.
 */
class TemporalQueryIT {

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

    private String[] createEhrAndTemplate(Connection conn) throws Exception {
        ResultSet ehrRs = conn.createStatement()
                .executeQuery("INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                        + "VALUES ('temporal-patient-" + UUID.randomUUID() + "', 'ehr.temporal.org', 1) RETURNING id");
        ehrRs.next();
        String ehrId = ehrRs.getString("id");

        String templateUnique = "test.temporal." + UUID.randomUUID();
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
    void pointInTimeQuery() throws Exception {
        try (Connection conn = connect()) {
            String[] ids = createEhrAndTemplate(conn);
            String ehrId = ids[0];
            String templateId = ids[1];

            // Create v1 at a known time T1
            PreparedStatement psV1 = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant, "
                            + "valid_period, committed_at) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. V1', 1, 'creation', 'committer-a', 1, "
                            + "tstzrange('2025-01-01 00:00:00+00'::timestamptz, NULL), "
                            + "'2025-01-01 00:00:00+00'::timestamptz) RETURNING id");
            psV1.setString(1, ehrId);
            psV1.setString(2, templateId);
            ResultSet rsV1 = psV1.executeQuery();
            rsV1.next();
            String compId = rsV1.getString("id");

            // Archive v1 to history with closed valid_period at T2
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.composition_history "
                            + "SELECT id, ehr_id, template_id, "
                            + "tstzrange('2025-01-01 00:00:00+00'::timestamptz, '2025-06-01 00:00:00+00'::timestamptz), "
                            + "archetype_id, template_name, composer_name, composer_id, language, "
                            + "territory, category_code, feeder_audit, participations, "
                            + "sys_version, contribution_id, change_type, committed_at, "
                            + "committer_name, committer_id, sys_tenant "
                            + "FROM ehr_system.composition WHERE id = '" + compId + "'");

            // Delete v1 from current
            conn.createStatement().execute("DELETE FROM ehr_system.composition WHERE id = '" + compId + "'");

            // Insert v2 starting at T2
            PreparedStatement psV2 = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (id, ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant, "
                            + "valid_period, committed_at) "
                            + "VALUES (?::uuid, ?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. V2', 2, 'modification', 'committer-b', 1, "
                            + "tstzrange('2025-06-01 00:00:00+00'::timestamptz, NULL), "
                            + "'2025-06-01 00:00:00+00'::timestamptz)");
            psV2.setString(1, compId);
            psV2.setString(2, ehrId);
            psV2.setString(3, templateId);
            psV2.execute();

            // Query point-in-time at T1 (March 2025) -- should return v1 from history
            PreparedStatement psQueryT1 =
                    conn.prepareStatement("SELECT composer_name, sys_version FROM ehr_system.composition_history "
                            + "WHERE id = ?::uuid AND valid_period @> '2025-03-15 00:00:00+00'::timestamptz");
            psQueryT1.setString(1, compId);
            ResultSet rsT1 = psQueryT1.executeQuery();
            assertThat(rsT1.next()).isTrue();
            assertThat(rsT1.getString("composer_name")).isEqualTo("Dr. V1");
            assertThat(rsT1.getInt("sys_version")).isEqualTo(1);

            // Query point-in-time at T3 (August 2025) -- should return v2 from current
            PreparedStatement psQueryT3 =
                    conn.prepareStatement("SELECT composer_name, sys_version FROM ehr_system.composition "
                            + "WHERE id = ?::uuid AND valid_period @> '2025-08-15 00:00:00+00'::timestamptz");
            psQueryT3.setString(1, compId);
            ResultSet rsT3 = psQueryT3.executeQuery();
            assertThat(rsT3.next()).isTrue();
            assertThat(rsT3.getString("composer_name")).isEqualTo("Dr. V2");
            assertThat(rsT3.getInt("sys_version")).isEqualTo(2);
        }
    }

    @Test
    void dateRangeQuery() throws Exception {
        try (Connection conn = connect()) {
            String[] ids = createEhrAndTemplate(conn);
            String ehrId = ids[0];
            String templateId = ids[1];

            // Create 3 compositions at different times with closed valid_periods in history
            // Comp A: Jan-Mar 2025
            PreparedStatement psA = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition_history (ehr_id, template_id, archetype_id, "
                            + "template_name, composer_name, sys_version, change_type, committer_name, "
                            + "sys_tenant, valid_period, committed_at) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. A', 1, 'creation', 'committer', 1, "
                            + "tstzrange('2025-01-01+00'::timestamptz, '2025-03-31+00'::timestamptz), "
                            + "'2025-01-01+00'::timestamptz) RETURNING id");
            psA.setString(1, ehrId);
            psA.setString(2, templateId);
            ResultSet rsA = psA.executeQuery();
            rsA.next();

            // Comp B: Apr-Jun 2025
            PreparedStatement psB = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition_history (ehr_id, template_id, archetype_id, "
                            + "template_name, composer_name, sys_version, change_type, committer_name, "
                            + "sys_tenant, valid_period, committed_at) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. B', 1, 'creation', 'committer', 1, "
                            + "tstzrange('2025-04-01+00'::timestamptz, '2025-06-30+00'::timestamptz), "
                            + "'2025-04-01+00'::timestamptz) RETURNING id");
            psB.setString(1, ehrId);
            psB.setString(2, templateId);
            ResultSet rsB = psB.executeQuery();
            rsB.next();

            // Comp C: Jul-Sep 2025
            PreparedStatement psC = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition_history (ehr_id, template_id, archetype_id, "
                            + "template_name, composer_name, sys_version, change_type, committer_name, "
                            + "sys_tenant, valid_period, committed_at) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. C', 1, 'creation', 'committer', 1, "
                            + "tstzrange('2025-07-01+00'::timestamptz, '2025-09-30+00'::timestamptz), "
                            + "'2025-07-01+00'::timestamptz) RETURNING id");
            psC.setString(1, ehrId);
            psC.setString(2, templateId);
            ResultSet rsC = psC.executeQuery();
            rsC.next();

            // Query with range Feb-May: should overlap with A and B
            PreparedStatement psQuery =
                    conn.prepareStatement("SELECT composer_name FROM ehr_system.composition_history "
                            + "WHERE ehr_id = ?::uuid "
                            + "AND valid_period && tstzrange('2025-02-01+00'::timestamptz, '2025-05-01+00'::timestamptz) "
                            + "ORDER BY composer_name");
            psQuery.setString(1, ehrId);
            ResultSet rs = psQuery.executeQuery();

            List<String> composers = new ArrayList<>();
            while (rs.next()) {
                composers.add(rs.getString("composer_name"));
            }
            assertThat(composers).containsExactly("Dr. A", "Dr. B");
        }
    }

    @Test
    void dstBoundaryComposition() throws Exception {
        try (Connection conn = connect()) {
            String[] ids = createEhrAndTemplate(conn);
            String ehrId = ids[0];
            String templateId = ids[1];

            // Insert composition with committed_at at DST boundary (EU spring forward)
            // 2025-03-30 02:00:00+01 is the CET -> CEST transition point
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant, "
                            + "committed_at) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. DST', 1, 'creation', 'committer-dst', 1, "
                            + "'2025-03-30 02:00:00+01'::timestamptz) RETURNING id, committed_at");
            ps.setString(1, ehrId);
            ps.setString(2, templateId);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            String compId = rs.getString("id");
            assertThat(rs.getTimestamp("committed_at")).isNotNull();

            // Retrieve it and verify the timestamp is stored correctly
            PreparedStatement psQuery = conn.prepareStatement("SELECT composer_name, committed_at, "
                    + "committed_at AT TIME ZONE 'UTC' AS committed_utc "
                    + "FROM ehr_system.composition WHERE id = ?::uuid");
            psQuery.setString(1, compId);
            ResultSet queryRs = psQuery.executeQuery();

            assertThat(queryRs.next()).isTrue();
            assertThat(queryRs.getString("composer_name")).isEqualTo("Dr. DST");
            // 2025-03-30 02:00:00+01 == 2025-03-30 01:00:00 UTC
            assertThat(queryRs.getTimestamp("committed_utc")).isNotNull();
            assertThat(queryRs.getTimestamp("committed_utc").toString()).startsWith("2025-03-30 01:00:00");
        }
    }
}
