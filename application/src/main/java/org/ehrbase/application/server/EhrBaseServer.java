/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.application.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(
        scanBasePackages = {
            "org.ehrbase.application.server",
            "org.ehrbase.configuration",
            "org.ehrbase.service",
            "org.ehrbase.cache",
            "org.ehrbase.repository",
            "org.ehrbase.rest.api",
            "org.ehrbase.plugin"
        },
        exclude = {ManagementWebSecurityAutoConfiguration.class, SecurityAutoConfiguration.class})
@SuppressWarnings("java:S1118")
public class EhrBaseServer {

    public static SpringApplication build(String[] args) {
        return new SpringApplicationBuilder(EhrBaseServer.class)
                .web(WebApplicationType.SERVLET)
                .build(args);
    }
}
