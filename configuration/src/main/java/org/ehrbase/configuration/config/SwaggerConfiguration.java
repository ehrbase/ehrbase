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
package org.ehrbase.configuration.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import java.util.stream.Stream;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    public GroupedOpenApi statusApi() {
        return GroupedOpenApi.builder()
                .group("2. EHRbase Status Endpoint")
                .pathsToMatch("/rest/status")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("3. EHRbase Admin API")
                .pathsToMatch("/rest/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi actuatorApi() {
        return GroupedOpenApi.builder()
                .group("4. Management API")
                .pathsToMatch("/management/**")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "ehrbase.rest.experimental.tags.enabled", havingValue = "true")
    public GroupedOpenApi experimentalApi(
            @Value("${ehrbase.rest.experimental.tags.context-path:/rest/experimental/tags}") String path) {
        return GroupedOpenApi.builder()
                .group("5. Experimental API")
                .pathsToMatch(Stream.of(path)
                        .map(p -> "/%s/**".formatted(p.replaceFirst("/", "").replaceFirst("^/", "")))
                        .toList()
                        .toArray(String[]::new))
                .build();
    }

    @Bean
    public OpenAPI ehrBaseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EHRbase API")
                        .description(
                                "EHRbase implements the [official openEHR REST API](https://specifications.openehr.org/releases/ITS-REST/latest/). "
                                        + "Additionally, EHRbase provides a custom `status` heartbeat endpoint, "
                                        + "an [Admin API](https://docs.ehrbase.org/docs/EHRbase/Explore/Admin-REST) (if activated) "
                                        + "and a [Status and Metrics API](http://localhost:3000/docs/EHRbase/Explore/Status-And-Metrics) (if activated) "
                                        + "for monitoring and maintenance. "
                                        + " "
                                        + "Note: The openEHR REST API is documented in their official documentation, not here. Please refer to their separate documentation.")
                        .version("v1")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://github.com/ehrbase/ehrbase/blob/develop/LICENSE.md")))
                .externalDocs(new ExternalDocumentation()
                        .description("EHRbase Documentation")
                        .url("https://docs.ehrbase.org/"));
    }
}
