/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.application.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public GroupedOpenApi openEhrApi() {
        return GroupedOpenApi.builder()
                .group("1. openEHR API")
                .pathsToMatch("/rest/openehr/**")
                .build();
    }

    @Bean
    public GroupedOpenApi ehrScapeApi() {
        return GroupedOpenApi.builder()
                .group("2. EhrScape API")
                .pathsToMatch("/rest/ecis/**")
                .build();
    }

    @Bean
    public GroupedOpenApi statusApi() {
        return GroupedOpenApi.builder()
                .group("3. EHRbase Status Endpoint")
                .pathsToMatch("/rest/status")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("4. EHRbase Admin API")
                .pathsToMatch("/rest/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi actuatorApi() {
        return GroupedOpenApi.builder()
                .group("5. Management API")
                .pathsToMatch("/management/**")
                .build();
    }

    @Bean
    public OpenAPI ehrBaseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EHRbase API")
                        .description(
                                "EHRbase implements the [official openEHR REST API](https://specifications.openehr.org/releases/ITS-REST/latest/) and "
                                        + "a subset of the [EhrScape API](https://www.ehrscape.com/). "
                                        + "Additionally, EHRbase provides a custom `status` heartbeat endpoint, "
                                        + "an [Admin API](https://ehrbase.readthedocs.io/en/latest/03_development/07_admin/index.html) (if activated) "
                                        + "and a [Status and Metrics API](https://ehrbase.readthedocs.io/en/latest/03_development/08_status_and_metrics/index.html?highlight=status) (if activated) "
                                        + "for monitoring and maintenance. "
                                        + "Please select the definition in the top right."
                                        + " "
                                        + "Note: The openEHR REST API and the EhrScape API are documented in their official documentation, not here. Please refer to their separate documentation.")
                        .version("v1")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://github.com/ehrbase/ehrbase/blob/develop/LICENSE.md")))
                .externalDocs(new ExternalDocumentation()
                        .description("EHRbase Documentation")
                        .url("https://ehrbase.readthedocs.io/"));
    }
}
