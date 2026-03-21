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
 * Integration tests for ehr_system.contribution table.
 * Connects directly to PG18 testcontainer — no Spring context needed.
 */
class ContributionRepositoryIT {

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
                .executeQuery(
                        "INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                                + "VALUES ('contrib-patient-" + UUID.randomUUID() + "', 'ehr.contrib.org', 1) "
                                + "RETURNING id");
        rs.next();
        return rs.getString("id");
    }

    @Test
    void createContribution() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.contribution (ehr_id, contribution_type, change_type, "
                            + "committer_name, committer_id, description, sys_tenant) "
                            + "VALUES (?::uuid, 'composition', 'creation', 'Dr. Smith', 'doc-1', "
                            + "'Initial composition', 1) "
                            + "RETURNING id, contribution_type, change_type, time_committed");
            ps.setString(1, ehrId);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id")).version()).isEqualTo(7);
            assertThat(rs.getString("contribution_type")).isEqualTo("composition");
            assertThat(rs.getString("change_type")).isEqualTo("creation");
            assertThat(rs.getTimestamp("time_committed")).isNotNull();
        }
    }

    @Test
    void listContributionsForEhr() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);

            // Insert 3 contributions
            for (int i = 1; i <= 3; i++) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ehr_system.contribution (ehr_id, contribution_type, change_type, "
                                + "committer_name, description, sys_tenant) "
                                + "VALUES (?::uuid, 'composition', 'creation', 'committer-" + i + "', "
                                + "'Contribution " + i + "', 1)");
                ps.setString(1, ehrId);
                ps.execute();
            }

            // Query by ehr_id
            PreparedStatement query =
                    conn.prepareStatement("SELECT count(*) FROM ehr_system.contribution WHERE ehr_id = ?::uuid");
            query.setString(1, ehrId);
            ResultSet rs = query.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(3);
        }
    }

    @Test
    void contributionLinkedToComposition() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);

            // Create template
            String templateUnique = "contrib.template." + UUID.randomUUID();
            PreparedStatement tps = conn.prepareStatement(
                    "INSERT INTO ehr_system.template (template_id, content, sys_tenant) "
                            + "VALUES (?, '<template/>', 1) RETURNING id");
            tps.setString(1, templateUnique);
            ResultSet tplRs = tps.executeQuery();
            tplRs.next();
            String templateId = tplRs.getString("id");

            // Create contribution
            PreparedStatement contribPs = conn.prepareStatement(
                    "INSERT INTO ehr_system.contribution (ehr_id, contribution_type, change_type, "
                            + "committer_name, sys_tenant) "
                            + "VALUES (?::uuid, 'composition', 'creation', 'Dr. Smith', 1) RETURNING id");
            contribPs.setString(1, ehrId);
            ResultSet contribRs = contribPs.executeQuery();
            contribRs.next();
            String contributionId = contribRs.getString("id");

            // Create composition referencing the contribution
            PreparedStatement compPs = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, contribution_id, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. Smith', 1, ?::uuid, 'creation', 'Dr. Smith', 1) RETURNING id");
            compPs.setString(1, ehrId);
            compPs.setString(2, templateId);
            compPs.setString(3, contributionId);
            ResultSet compRs = compPs.executeQuery();
            compRs.next();
            String compositionId = compRs.getString("id");

            // Verify the FK link: query composition and check contribution_id
            ResultSet verifyRs = conn.createStatement()
                    .executeQuery("SELECT contribution_id FROM ehr_system.composition WHERE id = '" + compositionId
                            + "'");
            assertThat(verifyRs.next()).isTrue();
            assertThat(verifyRs.getString("contribution_id")).isEqualTo(contributionId);
        }
    }
}
