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

import java.util.UUID;
import org.ehrbase.test.EhrbasePostgreSQLContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for end-to-end integration tests.
 * Boots the full application with {@code RANDOM_PORT} against PG18 testcontainer.
 *
 * <p>Subclasses get a {@link TestRestTemplate} for HTTP requests and helpers for
 * common operations (EHR creation, template upload, composition creation).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class EndToEndTestBase {

    @Autowired
    protected TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configurePg18(DynamicPropertyRegistry registry) {
        EhrbasePostgreSQLContainer pg = EhrbasePostgreSQLContainer.sharedInstance();
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);
    }

    /**
     * Creates an EHR via POST /api/v1/ehrs and returns the EHR ID.
     */
    protected UUID createEhr() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Prefer", "return=representation");

        ResponseEntity<String> response =
                restTemplate.exchange("/api/v1/ehrs", HttpMethod.POST, new HttpEntity<>(null, headers), String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("Failed to create EHR: " + response.getStatusCode());
        }

        String location = response.getHeaders().getLocation().toString();
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    /**
     * Uploads an OPT XML template via POST /api/v1/templates/adl1.4.
     */
    protected void uploadTemplate(String optXml) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/templates/adl1.4", HttpMethod.POST, new HttpEntity<>(optXml, headers), String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("Failed to upload template: " + response.getBody());
        }
    }

    /**
     * Creates a composition via POST /api/v1/ehrs/{ehrId}/compositions.
     */
    protected String createComposition(UUID ehrId, String compositionJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Prefer", "return=representation");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/ehrs/{ehrId}/compositions",
                HttpMethod.POST,
                new HttpEntity<>(compositionJson, headers),
                String.class,
                ehrId);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("Failed to create composition: " + response.getBody());
        }

        return response.getHeaders().getETag();
    }
}
