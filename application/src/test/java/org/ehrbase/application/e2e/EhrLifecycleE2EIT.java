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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end integration test: EHR lifecycle.
 * Full HTTP request → controller → service → repository → PG18 → response.
 */
class EhrLifecycleE2EIT extends EndToEndTestBase {

    @Test
    void createAndGetEhr() {
        UUID ehrId = createEhr();
        assertThat(ehrId).isNotNull();

        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/ehrs/{ehrId}", String.class, ehrId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(ehrId.toString());
    }

    @Test
    void createEhrWithPreferMinimal() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Prefer", "return=minimal");

        ResponseEntity<String> response =
                restTemplate.exchange("/api/v1/ehrs", HttpMethod.POST, new HttpEntity<>(null, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody()).isNull();
    }

    @Test
    void createEhrWithPreferRepresentation() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Prefer", "return=representation");

        ResponseEntity<String> response =
                restTemplate.exchange("/api/v1/ehrs", HttpMethod.POST, new HttpEntity<>(null, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("ehrId");
    }

    @Test
    void getEhrNotFound() {
        UUID randomId = UUID.randomUUID();
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/ehrs/{ehrId}", String.class, randomId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getEhrInvalidId() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/ehrs/{ehrId}", String.class, "not-a-uuid");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createEhrReturnsLocationHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response =
                restTemplate.exchange("/api/v1/ehrs", HttpMethod.POST, new HttpEntity<>(null, headers), String.class);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().toString()).contains("/api/v1/ehrs/");
    }
}
