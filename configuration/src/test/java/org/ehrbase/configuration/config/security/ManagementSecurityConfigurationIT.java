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
package org.ehrbase.configuration.config.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.ehrbase.configuration.test.EhrbaseConfigurationIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

public class ManagementSecurityConfigurationIT {

    @EhrbaseConfigurationIntegrationTest
    @Nested
    class EndpointsDisabledDefault {

        @Autowired
        private MockMvc mockMvc;

        @ParameterizedTest
        @ValueSource(strings = {"env", "health", "info", "metrics", "prometheus", "loggers"})
        void GET_endpoint_disabled(String endpoint) throws Exception {
            mockMvc.perform(get("/management/" + endpoint)).andExpect(status().isNotFound());
        }
    }

    @EhrbaseConfigurationIntegrationTest
    @TestPropertySource(
            properties = {
                "management.endpoints.enabled-by-default=true",
                "management.endpoints.web.exposure.include=health,info,metrics,prometheus,loggers",
                "management.endpoint.info.enabled=true",
                "management.endpoint.metrics.enabled=true",
                "management.endpoint.prometheus.enabled=true",
                "management.endpoint.loggers.enabled=true",
                "management.endpoint.health.enabled=true",
                "management.health.db.enabled=false", // turn off db health checks for tests
            })
    @Nested
    class EndpointsEnabled {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void GET_env_NOT_FOUND() throws Exception {
            mockMvc.perform(get("/management/env")).andExpect(status().isNotFound());
        }

        @Test
        void GET_health_OK() throws Exception {
            mockMvc.perform(get("/management/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(content().json("{\"status\":\"UP\"}"));
        }

        @Test
        void GET_info_OK() throws Exception {
            mockMvc.perform(get("/management/info"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"));
        }

        @Test
        void GET_metrics_OK() throws Exception {
            mockMvc.perform(get("/management/metrics"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"));
        }

        @Test
        void GET_prometheus_OK() throws Exception {
            mockMvc.perform(get("/management/prometheus"))
                    .andExpect(content().contentTypeCompatibleWith("text/plain;charset=utf-8"));
        }

        @Test
        void GET_loggers_OK() throws Exception {
            mockMvc.perform(get("/management/loggers"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"));
        }
    }

    @EhrbaseConfigurationIntegrationTest
    @TestPropertySource(
            properties = {
                "management.endpoints.web.exposure.include=loggers",
                "management.endpoint.loggers.enabled=true"
            })
    @Nested
    class CSRFEnabledDefault {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void POST_loggers_Forbidden_by_CSRF() throws Exception {
            mockMvc.perform(post("/management/loggers/org.ehrbase.test")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"configuredLevel\":\"debug\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @EhrbaseConfigurationIntegrationTest
    @TestPropertySource(
            properties = {
                "ehrbase.security.management.endpoints.web.csrf-validation-enabled=false",
                "management.endpoints.web.exposure.include=loggers",
                "management.endpoint.loggers.enabled=true"
            })
    @Nested
    class CSRFDisabled {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void POST_loggers_OK() throws Exception {
            mockMvc.perform(post("/management/loggers/org.ehrbase.test")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"configuredLevel\":\"debug\"}"))
                    .andExpect(status().isNoContent());
        }
    }
}
