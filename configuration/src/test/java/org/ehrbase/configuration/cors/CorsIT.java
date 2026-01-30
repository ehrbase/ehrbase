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
package org.ehrbase.configuration.cors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.ehrbase.configuration.test.EhrbaseConfigurationIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

class CorsIT {

    @EhrbaseConfigurationIntegrationTest
    abstract class CursBaseTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void OPTION_corsHeaderPresent_OK() throws Exception {
            mockMvc.perform(options("/rest/openehr/v1/definition/template/adl1.4")
                            .header("Access-Control-Request-Method", "GET")
                            .header("Origin", "https://client.ehrbase.org"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class NoAuth extends CursBaseTest {}

    @TestPropertySource(properties = {"security.authType=basic"})
    @Nested
    class BasicAuth extends CursBaseTest {}
}
