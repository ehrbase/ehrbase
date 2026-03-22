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
package org.ehrbase.rest.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.1 configuration for the EHRbase REST API v2.
 * Swagger UI available at /api/docs.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI ehrbaseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EHRbase REST API")
                        .version("v2")
                        .description(
                                "openEHR Clinical Data Repository — REST API with normalized PostgreSQL 18+ storage, "
                                        + "SQL views, GraphQL, and full healthcare compliance (GDPR, HIPAA, IHE)")
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
                        .contact(new Contact().name("EHRbase").url("https://ehrbase.org")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(
                                "bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token from OAuth2 provider"))
                        .addSecuritySchemes(
                                "basic",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("HTTP Basic authentication (development only)")));
    }

    @Bean
    public GroupedOpenApi restApiV2() {
        return GroupedOpenApi.builder()
                .group("EHRbase REST API v2")
                .pathsToMatch("/api/v2/**")
                .build();
    }
}
