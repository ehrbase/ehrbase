/*
 * Copyright (c) 2021 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "security", name = "auth-type", havingValue = "none")
public class NoOpSecurityConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(NoOpSecurityConfiguration.class);

  @PostConstruct
  public void initialize() {
    LOG.warn("Security is disabled. Configure 'security.auth-type' to disable this warning.");
  }

  @Bean
  @Primary
  public IAuthenticationFacade anonymousAuthentication() {
    var filter = new AnonymousAuthenticationFilter("key");
    return () -> new AnonymousAuthenticationToken("key",
        filter.getPrincipal(),
        filter.getAuthorities());
  }
}
