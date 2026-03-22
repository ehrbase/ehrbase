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
import java.sql.ResultSet;
import org.ehrbase.test.EhrbasePostgreSQLContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for pgcrypto extension availability and encrypt/decrypt operations.
 * Connects directly to PG18 testcontainer -- no Spring context needed.
 */
class EncryptionIT {

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
    void pgcryptoAvailable() throws Exception {
        try (Connection conn = connect()) {
            // Verify pgcrypto extension is installed
            ResultSet extRs =
                    conn.createStatement().executeQuery("SELECT 1 FROM pg_extension WHERE extname = 'pgcrypto'");
            assertThat(extRs.next()).isTrue();

            // Verify pgp_sym_encrypt function exists
            ResultSet encFnRs =
                    conn.createStatement().executeQuery("SELECT 1 FROM pg_proc WHERE proname = 'pgp_sym_encrypt'");
            assertThat(encFnRs.next()).isTrue();

            // Verify pgp_sym_decrypt function exists
            ResultSet decFnRs =
                    conn.createStatement().executeQuery("SELECT 1 FROM pg_proc WHERE proname = 'pgp_sym_decrypt'");
            assertThat(decFnRs.next()).isTrue();
        }
    }

    @Test
    void pgcryptoEncryptDecrypt() throws Exception {
        try (Connection conn = connect()) {
            String plaintext = "sensitive-data";
            String key = "my-secret-key-2026";

            // Encrypt then decrypt and verify round-trip
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT ext.pgp_sym_decrypt("
                            + "ext.pgp_sym_encrypt('" + plaintext + "', '" + key + "'), "
                            + "'" + key + "') AS decrypted");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("decrypted")).isEqualTo(plaintext);
        }
    }

    @Test
    void pgcryptoWrongKeyFails() throws Exception {
        try (Connection conn = connect()) {
            String plaintext = "sensitive-data";
            String key1 = "correct-key-2026";
            String key2 = "wrong-key-2026";

            // Encrypt with key1
            ResultSet encRs = conn.createStatement()
                    .executeQuery("SELECT ext.pgp_sym_encrypt('" + plaintext + "', '" + key1 + "') AS encrypted");
            assertThat(encRs.next()).isTrue();
            byte[] encrypted = encRs.getBytes("encrypted");
            assertThat(encrypted).isNotNull();

            // Decrypt with key2 should fail (pgcrypto throws an error on wrong key)
            boolean decryptFailed = false;
            try {
                // Use a PreparedStatement to pass the encrypted bytes
                var ps = conn.prepareStatement("SELECT ext.pgp_sym_decrypt(?, ?) AS decrypted");
                ps.setBytes(1, encrypted);
                ps.setString(2, key2);
                ResultSet decRs = ps.executeQuery();
                decRs.next();
                // If we get here, the decrypt returned something different from plaintext
                String result = decRs.getString("decrypted");
                assertThat(result).isNotEqualTo(plaintext);
            } catch (Exception e) {
                // pgcrypto raises an error when the wrong key is used
                decryptFailed = true;
                assertThat(e.getMessage()).containsIgnoringCase("wrong key");
            }

            // Either it threw an exception or returned wrong data -- both are acceptable
            // as long as the original plaintext is not recoverable with the wrong key
            assertThat(decryptFailed)
                    .as("Decrypting with the wrong key should raise an error")
                    .isTrue();
        }
    }
}
