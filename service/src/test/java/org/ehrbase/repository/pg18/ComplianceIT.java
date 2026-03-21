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
 * Integration tests for ehr_system.consent and ehr_system.access_grants tables.
 * Connects directly to PG18 testcontainer — no Spring context needed.
 */
class ComplianceIT {

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
                        + "VALUES ('compliance-patient-" + UUID.randomUUID() + "', 'ehr.compliance.org', 1) "
                        + "RETURNING id");
        rs.next();
        return rs.getString("id");
    }

    @Test
    void createConsent() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.consent (ehr_id, consent_type, status, scope, granted_by, sys_tenant) "
                            + "VALUES (?::uuid, 'research', 'active', "
                            + "'{\"studies\": [\"cardio-2026\"]}'::jsonb, 'patient-self', 1) "
                            + "RETURNING id, consent_type, status, scope, granted_at");
            ps.setString(1, ehrId);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id")).version()).isEqualTo(7);
            assertThat(rs.getString("consent_type")).isEqualTo("research");
            assertThat(rs.getString("status")).isEqualTo("active");
            assertThat(rs.getString("scope")).contains("cardio-2026");
            assertThat(rs.getTimestamp("granted_at")).isNotNull();
        }
    }

    @Test
    void createAccessGrant() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);
            UUID userId = UUID.randomUUID();

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.access_grants (user_id, ehr_id, access_level, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 'read', 1) "
                            + "RETURNING id, user_id, ehr_id, access_level, granted_at");
            ps.setString(1, userId.toString());
            ps.setString(2, ehrId);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id")).version()).isEqualTo(7);
            assertThat(rs.getString("user_id")).isEqualTo(userId.toString());
            assertThat(rs.getString("ehr_id")).isEqualTo(ehrId);
            assertThat(rs.getString("access_level")).isEqualTo("read");
            assertThat(rs.getTimestamp("granted_at")).isNotNull();
        }
    }

    @Test
    void consentTypes() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);

            // Insert active consent
            PreparedStatement psActive = conn.prepareStatement(
                    "INSERT INTO ehr_system.consent (ehr_id, consent_type, status, granted_by, sys_tenant) "
                            + "VALUES (?::uuid, 'treatment', 'active', 'patient', 1) RETURNING id");
            psActive.setString(1, ehrId);
            ResultSet rsActive = psActive.executeQuery();
            rsActive.next();
            String activeId = rsActive.getString("id");

            // Insert withdrawn consent
            PreparedStatement psWithdrawn =
                    conn.prepareStatement("INSERT INTO ehr_system.consent (ehr_id, consent_type, status, granted_by, "
                            + "withdrawn_at, sys_tenant) "
                            + "VALUES (?::uuid, 'research', 'withdrawn', 'patient', now(), 1) RETURNING id");
            psWithdrawn.setString(1, ehrId);
            ResultSet rsWithdrawn = psWithdrawn.executeQuery();
            rsWithdrawn.next();
            String withdrawnId = rsWithdrawn.getString("id");

            // Insert expired consent (expires_at in the past)
            PreparedStatement psExpired =
                    conn.prepareStatement("INSERT INTO ehr_system.consent (ehr_id, consent_type, status, granted_by, "
                            + "expires_at, sys_tenant) "
                            + "VALUES (?::uuid, 'data_sharing', 'active', 'patient', "
                            + "now() - interval '1 day', 1) RETURNING id");
            psExpired.setString(1, ehrId);
            ResultSet rsExpired = psExpired.executeQuery();
            rsExpired.next();
            String expiredId = rsExpired.getString("id");

            // Query all consents for this EHR and verify
            PreparedStatement query = conn.prepareStatement("SELECT id, consent_type, status, withdrawn_at, expires_at "
                    + "FROM ehr_system.consent WHERE ehr_id = ?::uuid ORDER BY consent_type");
            query.setString(1, ehrId);
            ResultSet rs = query.executeQuery();

            List<String> types = new ArrayList<>();
            List<String> statuses = new ArrayList<>();
            while (rs.next()) {
                types.add(rs.getString("consent_type"));
                statuses.add(rs.getString("status"));
            }

            assertThat(types).containsExactly("data_sharing", "research", "treatment");
            assertThat(statuses).containsExactly("active", "withdrawn", "active");

            // Verify withdrawn consent has withdrawn_at set
            ResultSet wRs = conn.createStatement()
                    .executeQuery("SELECT withdrawn_at FROM ehr_system.consent WHERE id = '" + withdrawnId + "'");
            wRs.next();
            assertThat(wRs.getTimestamp("withdrawn_at")).isNotNull();

            // Verify expired consent has expires_at in the past
            ResultSet eRs = conn.createStatement()
                    .executeQuery("SELECT expires_at < now() AS is_expired FROM ehr_system.consent WHERE id = '"
                            + expiredId + "'");
            eRs.next();
            assertThat(eRs.getBoolean("is_expired")).isTrue();
        }
    }
}
