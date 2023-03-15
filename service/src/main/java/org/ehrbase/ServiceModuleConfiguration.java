/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase;

import java.util.List;
import org.ehrbase.api.tenant.TenantIdExtractionStrategy;
import org.ehrbase.tenant.DefaultTenantAspect;
import org.ehrbase.tenant.extraction.AuthenticatedExtractionStrategy.AuthenticationExtractionStrategy;
import org.ehrbase.tenant.extraction.AuthenticatedExtractionStrategy.TokenAuthenticatedExtractionStrategy;
import org.ehrbase.tenant.extraction.DefaultExtractionStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@ComponentScan(basePackages = {"org.ehrbase.service", "org.ehrbase.plugin", "org.ehrbase.repository"})
@EnableAspectJAutoProxy
public class ServiceModuleConfiguration {

    @Bean
    public DefaultTenantAspect tenantAspect(List<TenantIdExtractionStrategy<?>> strategies) {
        return new DefaultTenantAspect(strategies);
    }

    @Bean
    public TenantIdExtractionStrategy<String> defaultStrategy() {
        return new DefaultExtractionStrategy();
    }

    @Bean
    public TenantIdExtractionStrategy<String> tokenAuthenticatedExtractionStrategy() {
        return new TokenAuthenticatedExtractionStrategy();
    }

    @Bean
    public TenantIdExtractionStrategy<String> authenticationExtractionStrategy() {
        return new AuthenticationExtractionStrategy();
    }
}
