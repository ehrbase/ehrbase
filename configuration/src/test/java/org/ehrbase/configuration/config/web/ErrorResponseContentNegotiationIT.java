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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.ehrbase.configuration.exception.DefaultExceptionHandler;
import org.ehrbase.configuration.test.EhrbaseConfigurationIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@EhrbaseConfigurationIntegrationTest
@Import(DefaultExceptionHandler.class)
class ErrorResponseContentNegotiationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void errorResponsesAreJsonForClientsAcceptingAnything() throws Exception {
        mockMvc.perform(get("/rest/openehr/v1/does-not-exist").accept(MediaType.ALL))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void errorResponsesAreJsonWithoutAcceptHeader() throws Exception {
        mockMvc.perform(get("/rest/openehr/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void errorResponsesStayXmlCapableForXmlClients() throws Exception {
        mockMvc.perform(get("/rest/openehr/v1/does-not-exist").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
    }
}
