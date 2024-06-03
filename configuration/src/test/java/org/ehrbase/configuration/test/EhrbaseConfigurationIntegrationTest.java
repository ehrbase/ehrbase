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
package org.ehrbase.configuration.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.ehrbase.configuration.config.JacksonConfiguration;
import org.ehrbase.configuration.config.SwaggerConfiguration;
import org.ehrbase.configuration.config.client.HttpClientConfiguration;
import org.ehrbase.configuration.config.plugin.PluginConfig;
import org.ehrbase.configuration.config.security.SecurityConfiguration;
import org.ehrbase.configuration.config.web.WebConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Basic configuration for an EHRbase configuration spring boot integration test.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = {
            HttpClientConfiguration.class,
            SecurityConfiguration.class,
            JacksonConfiguration.class,
            SwaggerConfiguration.class,
            PluginConfig.class,
            // ValidationConfiguration.class, // <- needs CacheConfiguration
            // CacheConfiguration.class
            WebConfiguration.class,
        },
        properties = {"spring.cache.type=simple"})
@AutoConfigureMockMvc
@EnableAutoConfiguration(
        exclude = {
            // prevent execution of flyway migrations during startup
            FlywayAutoConfiguration.class
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public @interface EhrbaseConfigurationIntegrationTest {}
