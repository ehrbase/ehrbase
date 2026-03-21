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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.ehrbase.test.ServiceIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * PG18-specific integration test: UUIDv7 features.
 * Verifies uuidv7() generates chronologically ordered UUIDs.
 */
@ServiceIntegrationTest
class UuidV7IT {

    @Autowired
    private DataSource dataSource;

    @Test
    void uuidv7GeneratesChronologicallyOrderedIds() throws Exception {
        List<UUID> uuids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            for (int i = 0; i < 10; i++) {
                ResultSet rs = stmt.executeQuery("SELECT uuidv7()");
                rs.next();
                uuids.add(UUID.fromString(rs.getString(1)));
            }
        }

        assertThat(uuids).hasSize(10);
        // UUIDv7 embeds timestamp in most significant bits — sequential generation
        // means string comparison should reflect chronological order
        for (int i = 1; i < uuids.size(); i++) {
            assertThat(uuids.get(i).toString())
                    .isGreaterThanOrEqualTo(uuids.get(i - 1).toString());
        }
    }

    @Test
    void uuidExtractTimestampReturnsTime() throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT uuid_extract_timestamp(uuidv7())");
            rs.next();
            var timestamp = rs.getTimestamp(1);
            assertThat(timestamp).isNotNull();
            // Should be within the last minute
            long diffMs = System.currentTimeMillis() - timestamp.getTime();
            assertThat(diffMs).isLessThan(60_000);
        }
    }

    @Test
    void uuidv7HasVersion7() throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT uuidv7()");
            rs.next();
            UUID uuid = UUID.fromString(rs.getString(1));
            assertThat(uuid.version()).isEqualTo(7);
        }
    }
}
