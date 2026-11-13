/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.configuration.config.web;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.ehrbase.api.service.StatusService;
import org.ehrbase.configuration.test.EhrbaseConfigurationIntegrationTest;
import org.ehrbase.rest.admin.AdminApiConfiguration;
import org.ehrbase.rest.openehr.OpenehrSystemController;
import org.ehrbase.rest.openehr.SystemOptionsProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class OpenehrSystemControllerIT {

    private static final String SYSTEM_PATH = "/rest/openehr/v1";

    @EhrbaseConfigurationIntegrationTest
    @Import({OpenehrSystemController.class, AdminApiConfiguration.class})
    @EnableConfigurationProperties(SystemOptionsProperties.class)
    abstract class SystemOptionsTest {

        @Autowired
        protected MockMvc mockMvc;

        @MockitoBean
        protected StatusService statusService;
    }

    @Nested
    class NoAuth extends SystemOptionsTest {

        @ParameterizedTest
        @ValueSource(strings = {SYSTEM_PATH, SYSTEM_PATH + "/"})
        void returnsTheOptionsAndConformanceStatement(String path) throws Exception {
            when(statusService.getEhrbaseVersion()).thenReturn("2.38.0");

            mockMvc.perform(options(path))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ALLOW, "GET,POST,PUT,DELETE,OPTIONS"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.solution").value("EHRbase"))
                    .andExpect(jsonPath("$.solution_version").value("v2.38.0"))
                    .andExpect(jsonPath("$.vendor").value("Vitagroup"))
                    .andExpect(jsonPath("$.restapi_specs_version").value("1.0.3"))
                    .andExpect(jsonPath("$.conformance_profile").value("STANDARD"))
                    .andExpect(jsonPath("$.endpoints", contains("/ehr", "/definition", "/query", "/admin")));
        }
    }

    @Nested
    @TestPropertySource(properties = "security.authType=basic")
    class BasicAuth extends SystemOptionsTest {

        @Test
        void requiresAuthentication() throws Exception {
            mockMvc.perform(options(SYSTEM_PATH)).andExpect(status().isUnauthorized());
        }
    }
}
