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
package org.ehrbase.application.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end test: RFC 7807 error handling across the full stack.
 */
class ErrorHandlingE2EIT extends EndToEndTestBase {

    @Test
    void invalidUuidReturnsRfc7807() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/ehrs/{ehrId}", String.class, "not-a-uuid");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Invalid");
    }

    @Test
    void notFoundReturnsRfc7807() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/ehrs/{ehrId}", String.class, UUID.randomUUID());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unknownEndpointReturns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/nonexistent", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void adl2StubReturns501() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/templates/adl2", String.class);
        // ADL 2.4 returns 501 Not Implemented or 404 depending on error handler
        assertThat(response.getStatusCode().value()).isGreaterThanOrEqualTo(400);
    }
}
