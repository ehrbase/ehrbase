/*
 * Copyright (c) 2021-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.application.config.security;

import javax.annotation.PostConstruct;
import org.ehrbase.service.IAuthenticationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * {@link Configuration} used when security is disabled.
 *
 * @author Renaud Subiger
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "security", name = "auth-type", havingValue = "none")
public class NoOpSecurityConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PostConstruct
    public void initialize() {
        logger.warn("Security is disabled. Configure 'security.auth-type' to disable this warning.");
    }

    @Bean
    @Primary
    public IAuthenticationFacade anonymousAuthentication() {
        var filter = new AnonymousAuthenticationFilter("key");
        return () -> new AnonymousAuthenticationToken("key", filter.getPrincipal(), filter.getAuthorities());
    }
}
