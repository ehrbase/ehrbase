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
package org.ehrbase.application.config.web;

import static java.lang.String.format;
import static org.ehrbase.rest.BaseController.*;

import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.application.util.IsoDateTimeConverter;
import org.ehrbase.rest.openehr.audit.CompositionAuditInterceptor;
import org.ehrbase.rest.openehr.audit.EhrAuditInterceptor;
import org.ehrbase.rest.openehr.audit.QueryAuditInterceptor;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@link Configuration} from Spring Web MVC.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfiguration implements WebMvcConfigurer {

    @Value(API_CONTEXT_PATH_WITH_VERSION)
    protected String apiContextPath;

    private final CorsProperties properties;

    private final AuditContext auditContext;

    private final EhrService ehrService;

    private final CompositionService compositionService;
    private final TenantService tenantService;

    public WebConfiguration(
            CorsProperties properties,
            AuditContext auditContext,
            EhrService ehrService,
            CompositionService compositionService,
            TenantService tenantService) {
        this.properties = properties;
        this.auditContext = auditContext;
        this.ehrService = ehrService;
        this.compositionService = compositionService;
        this.tenantService = tenantService;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new IsoDateTimeConverter()); // Converter for version_at_time and other ISO date params
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").combine(properties.toCorsConfiguration());
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        if (auditContext.isAuditEnabled()) {
            // Composition endpoint
            registry.addInterceptor(new CompositionAuditInterceptor(
                            auditContext, ehrService, compositionService, tenantService))
                    .addPathPatterns(format("%s/**/%s/**", apiContextPath, COMPOSITION));
            // Ehr endpoint
            registry.addInterceptor(new EhrAuditInterceptor(auditContext, ehrService, tenantService))
                    .addPathPatterns(format("%s/%s", apiContextPath, EHR), format("%s/%s/*", apiContextPath, EHR));
            // Query endpoint
            registry.addInterceptor(new QueryAuditInterceptor(auditContext, ehrService, tenantService))
                    .addPathPatterns(format("%s/%s/**", apiContextPath, QUERY));
        }
    }
}
