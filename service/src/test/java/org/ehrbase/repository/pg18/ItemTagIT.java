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
 * Integration tests for ehr_system.item_tag table.
 * Connects directly to PG18 testcontainer — no Spring context needed.
 */
class ItemTagIT {

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
    void createTag() throws Exception {
        UUID targetId = UUID.randomUUID();
        try (Connection conn = connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.item_tag (target_id, target_type, key, value, owner_id, sys_tenant) "
                            + "VALUES (?::uuid, 'composition', 'priority', 'high', 'user-1', 1) "
                            + "RETURNING id, target_id, target_type, key, value, owner_id, created_at");
            ps.setString(1, targetId.toString());
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id")).version()).isEqualTo(7);
            assertThat(rs.getString("target_id")).isEqualTo(targetId.toString());
            assertThat(rs.getString("target_type")).isEqualTo("composition");
            assertThat(rs.getString("key")).isEqualTo("priority");
            assertThat(rs.getString("value")).isEqualTo("high");
            assertThat(rs.getString("owner_id")).isEqualTo("user-1");
            assertThat(rs.getTimestamp("created_at")).isNotNull();
        }
    }

    @Test
    void queryTagsByTarget() throws Exception {
        UUID targetId = UUID.randomUUID();
        try (Connection conn = connect()) {
            // Insert 3 tags for the same target
            String[] keys = {"priority", "department", "reviewed"};
            String[] values = {"high", "cardiology", "true"};
            for (int i = 0; i < 3; i++) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ehr_system.item_tag (target_id, target_type, key, value, owner_id, sys_tenant) "
                                + "VALUES (?::uuid, 'composition', ?, ?, 'user-tags', 1)");
                ps.setString(1, targetId.toString());
                ps.setString(2, keys[i]);
                ps.setString(3, values[i]);
                ps.execute();
            }

            // Query by target
            PreparedStatement query = conn.prepareStatement(
                    "SELECT key, value FROM ehr_system.item_tag "
                            + "WHERE target_id = ?::uuid AND target_type = 'composition' ORDER BY key");
            query.setString(1, targetId.toString());
            ResultSet rs = query.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("key")).isEqualTo("department");
            assertThat(rs.getString("value")).isEqualTo("cardiology");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("key")).isEqualTo("priority");
            assertThat(rs.getString("value")).isEqualTo("high");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("key")).isEqualTo("reviewed");
            assertThat(rs.getString("value")).isEqualTo("true");

            assertThat(rs.next()).isFalse();
        }
    }

    @Test
    void deleteTag() throws Exception {
        UUID targetId = UUID.randomUUID();
        try (Connection conn = connect()) {
            // Insert a tag
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.item_tag (target_id, target_type, key, value, owner_id, sys_tenant) "
                            + "VALUES (?::uuid, 'ehr', 'temporary', 'yes', 'user-del', 1) RETURNING id");
            ps.setString(1, targetId.toString());
            ResultSet rs = ps.executeQuery();
            rs.next();
            String tagId = rs.getString("id");

            // Delete the tag
            int deleted = conn.createStatement()
                    .executeUpdate("DELETE FROM ehr_system.item_tag WHERE id = '" + tagId + "'");
            assertThat(deleted).isEqualTo(1);

            // Verify gone
            ResultSet verifyRs = conn.createStatement()
                    .executeQuery("SELECT id FROM ehr_system.item_tag WHERE id = '" + tagId + "'");
            assertThat(verifyRs.next()).isFalse();
        }
    }
}
