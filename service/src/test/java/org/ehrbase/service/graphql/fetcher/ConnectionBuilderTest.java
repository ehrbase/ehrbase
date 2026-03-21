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
package org.ehrbase.service.graphql.fetcher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for ConnectionBuilder — cursor encoding/decoding, pageInfo computation.
 */
class ConnectionBuilderTest {

    @Test
    void encodeCursorProducesBase64() {
        String cursor = ConnectionBuilder.encodeCursor(0);
        assertThat(cursor).isNotNull().isNotEmpty();
        // Should be base64 decodable
        assertThat(java.util.Base64.getDecoder().decode(cursor)).isNotEmpty();
    }

    @Test
    void decodeCursorReversesEncode() {
        String encoded = ConnectionBuilder.encodeCursor(5);
        int decoded = ConnectionBuilder.decodeCursor(encoded);
        // decodeCursor returns offset+1 (to position after the cursor)
        assertThat(decoded).isEqualTo(6);
    }

    @Test
    void decodeCursorNullReturnsZero() {
        assertThat(ConnectionBuilder.decodeCursor(null)).isZero();
        assertThat(ConnectionBuilder.decodeCursor("")).isZero();
    }

    @Test
    void cursorRoundTrip() {
        for (int i = 0; i < 100; i++) {
            String cursor = ConnectionBuilder.encodeCursor(i);
            int decoded = ConnectionBuilder.decodeCursor(cursor);
            assertThat(decoded).isEqualTo(i + 1);
        }
    }

    @Test
    void encodeCursorDifferentForDifferentOffsets() {
        String cursor0 = ConnectionBuilder.encodeCursor(0);
        String cursor10 = ConnectionBuilder.encodeCursor(10);
        String cursor100 = ConnectionBuilder.encodeCursor(100);
        assertThat(cursor0).isNotEqualTo(cursor10);
        assertThat(cursor10).isNotEqualTo(cursor100);
    }

    @Test
    void cursorContainsOffset() {
        String cursor = ConnectionBuilder.encodeCursor(42);
        String decoded = new String(java.util.Base64.getDecoder().decode(cursor));
        assertThat(decoded).contains("42");
        assertThat(decoded).startsWith("cursor:");
    }
}
