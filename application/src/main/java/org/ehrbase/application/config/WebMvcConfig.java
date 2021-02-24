/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.application.config;

import org.ehrbase.api.service.EhrService;
import org.ehrbase.application.util.IsoDateTimeConverter;
import org.ehrbase.application.util.StringToEnumConverter;
import org.ehrbase.rest.openehr.audit.CompositionAuditStrategy;
import org.ehrbase.rest.openehr.audit.EhrAuditStrategy;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter implements WebMvcConfigurer {

    private final AuditContext auditContext;

    private final EhrService ehrService;

    public WebMvcConfig(AuditContext auditContext, EhrService ehrService) {
        this.auditContext = auditContext;
        this.ehrService = ehrService;
    }

    /**
     * This config allows paths parameters to be of form "uuid::domain::version" - more specifically, it allows "." in domains.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer matcher) {
        matcher.setUseSuffixPatternMatch(false);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToEnumConverter());
        registry.addConverter(new IsoDateTimeConverter()); // Converter for version_at_time and other ISO date params
    }

    // enables CORS requests from any origin to any endpoint (see: https://www.baeldung.com/spring-cors)
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**");
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        if (auditContext.isAuditEnabled()) {
            registry
                    .addInterceptor(new EhrAuditStrategy(auditContext))
                    .addPathPatterns("/rest/openehr/v1/ehr", "/rest/openehr/v1/ehr/*");
            registry
                    .addInterceptor(new CompositionAuditStrategy(auditContext, ehrService))
                    .addPathPatterns("/rest/openehr/v1/composition/**", "/rest/openehr/v1/versioned_composition/**");
        }
    }
}
