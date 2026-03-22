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
 * Integration tests verifying SQL injection prevention via PreparedStatements.
 * Connects directly to PG18 testcontainer -- no Spring context needed.
 */
class SqlInjectionPreventionIT {

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
    void preparedStatementPreventsInjection() throws Exception {
        try (Connection conn = connect()) {
            String maliciousInput = "'; DROP TABLE ehr_system.ehr; --";

            // Use PreparedStatement with malicious input as subject_id
            PreparedStatement ps =
                    conn.prepareStatement("INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                            + "VALUES (?, ?, 1) RETURNING id");
            ps.setString(1, maliciousInput);
            ps.setString(2, "ehr.injection.org");
            ResultSet rs = ps.executeQuery();
            assertThat(rs.next()).isTrue();

            // Verify the ehr_system.ehr table still exists and is queryable
            ResultSet tableRs = conn.createStatement()
                    .executeQuery("SELECT 1 FROM information_schema.tables "
                            + "WHERE table_schema = 'ehr_system' AND table_name = 'ehr'");
            assertThat(tableRs.next()).isTrue();

            // Verify the malicious string was stored literally as data
            PreparedStatement queryPs =
                    conn.prepareStatement("SELECT subject_id FROM ehr_system.ehr WHERE subject_id = ?");
            queryPs.setString(1, maliciousInput);
            ResultSet queryRs = queryPs.executeQuery();
            assertThat(queryRs.next()).isTrue();
            assertThat(queryRs.getString("subject_id")).isEqualTo(maliciousInput);
        }
    }

    @Test
    void parameterizedQuerySafe() throws Exception {
        try (Connection conn = connect()) {
            // Subject IDs with special SQL characters
            String subjectWithQuotes = "patient-o'malley; SELECT 1;";
            String subjectWithSemicolon = "patient;DROP TABLE users;--";
            String subjectWithBackslash = "patient\\admin";

            // Insert all three
            PreparedStatement ps =
                    conn.prepareStatement("INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                            + "VALUES (?, 'ehr.special.org', 1) RETURNING id, subject_id");

            ps.setString(1, subjectWithQuotes);
            ResultSet rs1 = ps.executeQuery();
            assertThat(rs1.next()).isTrue();
            assertThat(rs1.getString("subject_id")).isEqualTo(subjectWithQuotes);

            ps.setString(1, subjectWithSemicolon);
            ResultSet rs2 = ps.executeQuery();
            assertThat(rs2.next()).isTrue();
            assertThat(rs2.getString("subject_id")).isEqualTo(subjectWithSemicolon);

            ps.setString(1, subjectWithBackslash);
            ResultSet rs3 = ps.executeQuery();
            assertThat(rs3.next()).isTrue();
            assertThat(rs3.getString("subject_id")).isEqualTo(subjectWithBackslash);

            // Query them back and verify they are stored correctly
            PreparedStatement queryPs =
                    conn.prepareStatement("SELECT subject_id FROM ehr_system.ehr WHERE subject_id = ?");

            queryPs.setString(1, subjectWithQuotes);
            ResultSet qr1 = queryPs.executeQuery();
            assertThat(qr1.next()).isTrue();
            assertThat(qr1.getString("subject_id")).isEqualTo(subjectWithQuotes);

            queryPs.setString(1, subjectWithSemicolon);
            ResultSet qr2 = queryPs.executeQuery();
            assertThat(qr2.next()).isTrue();
            assertThat(qr2.getString("subject_id")).isEqualTo(subjectWithSemicolon);

            queryPs.setString(1, subjectWithBackslash);
            ResultSet qr3 = queryPs.executeQuery();
            assertThat(qr3.next()).isTrue();
            assertThat(qr3.getString("subject_id")).isEqualTo(subjectWithBackslash);
        }
    }

    @Test
    void sqlCommentInjectionPrevented() throws Exception {
        try (Connection conn = connect()) {
            // Insert a legitimate record
            String legitimateSubject = "patient-legit-" + UUID.randomUUID();
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) " + "VALUES ('"
                            + legitimateSubject + "', 'ehr.legit.org', 1)");

            // Insert another record
            String otherSubject = "patient-other-" + UUID.randomUUID();
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) " + "VALUES ('"
                            + otherSubject + "', 'ehr.legit.org', 1)");

            // Attempt SQL injection via OR '1'='1' in a PreparedStatement
            String injectionAttempt = legitimateSubject + "' OR '1'='1";

            PreparedStatement ps = conn.prepareStatement("SELECT subject_id FROM ehr_system.ehr WHERE subject_id = ?");
            ps.setString(1, injectionAttempt);
            ResultSet rs = ps.executeQuery();

            // The injection attempt should NOT return any rows because the literal
            // string "patient-legit-xxx' OR '1'='1" does not exist as a subject_id
            assertThat(rs.next()).isFalse();
        }
    }
}
