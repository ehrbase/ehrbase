/*
 * Copyright (c) 2021 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.application.abac;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.ehrbase.api.exception.InternalServerException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(name = "abac.enabled")
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "abac")
@SuppressWarnings("java:S6212")
public class AbacConfig {

    public enum AbacType {
        EHR,
        EHR_STATUS,
        COMPOSITION,
        CONTRIBUTION,
        QUERY
    }

    public enum PolicyParameter {
        ORGANIZATION,
        PATIENT,
        TEMPLATE
    }

    static class Policy {
        private String name;
        private PolicyParameter[] parameters;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public PolicyParameter[] getParameters() {
            return parameters;
        }

        public void setParameters(PolicyParameter[] parameters) {
            this.parameters = parameters;
        }
    }

    private URI server;
    private String organizationClaim;
    private String patientClaim;
    private Map<AbacType, Policy> policy;

    @Bean
    public AbacCheck abacCheck(HttpClient httpClient) {
        return new AbacCheck(httpClient);
    }

    public URI getServer() {
        return server;
    }

    public void setServer(URI server) {
        this.server = server;
    }

    public String getOrganizationClaim() {
        return organizationClaim;
    }

    public void setOrganizationClaim(String organizationClaim) {
        this.organizationClaim = organizationClaim;
    }

    public String getPatientClaim() {
        return patientClaim;
    }

    public void setPatientClaim(String patientClaim) {
        this.patientClaim = patientClaim;
    }

    public Map<AbacType, Policy> getPolicy() {
        return policy;
    }

    public void setPolicy(Map<AbacType, Policy> policy) {
        this.policy = policy;
    }

    /*
     This class has only some extracted methods to handle ABAC server connection and requests.
     It is mainly a separate class so it can be overwritten by a MockBean in the context of tests.
    */
    public static class AbacCheck {

        private final HttpClient httpClient;

        public AbacCheck(HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        /**
         * Helper to build and send the actual HTTP request to the ABAC server.
         *
         * @param url     URL for ABAC server request
         * @param bodyMap Map of attributes for the request
         * @return HTTP response
         * @throws IOException          On error during attribute or HTTP handling
         */
        public boolean execute(String url, Map<String, String> bodyMap) throws IOException {
            return evaluateResponse(send(url, bodyMap));
        }

        private HttpResponse send(String url, Map<String, String> bodyMap) throws IOException {
            // convert bodyMap to JSON
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bodyMap);

            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

            try {
                return httpClient.execute(request);
            } catch (Exception e) {
                throw new InternalServerException(
                        "ABAC: Connection with ABAC server failed. Check configuration. Error: " + e.getMessage());
            }
        }

        private boolean evaluateResponse(HttpResponse response) {
            return response.getStatusLine().getStatusCode() == 200;
        }
    }
}
