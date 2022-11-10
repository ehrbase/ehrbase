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
package org.ehrbase.rest;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.api.tenant.ThreadLocalSupplier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan(basePackages = {"org.ehrbase.rest", "org.ehrbase.rest.admin", "org.ehrbase.rest.openehr"})
@EnableAspectJAutoProxy
public class RestModuleConfiguration implements WebMvcConfigurer {
    public static final String HTTP_HEADER_TENANT_ID = "Tenant-Id";

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HttpRequestSupplierInterceptor());
    }

    public static class HttpRequestSupplierInterceptor implements HandlerInterceptor {

        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
            ThreadLocalSupplier<HttpServletRequest> threadLocalSupplier =
                    ThreadLocalSupplier.supplyFor(HttpServletRequest.class);
            threadLocalSupplier.accept(request);
            return true;
        }

        public void afterCompletion(
                HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
                throws Exception {

            ThreadLocalSupplier<HttpServletRequest> threadLocalSupplier =
                    ThreadLocalSupplier.supplyFor(HttpServletRequest.class);
            threadLocalSupplier.reset();

            extractTenantId().ifPresent(id -> response.setHeader(HTTP_HEADER_TENANT_ID, id));
        }

        private Optional<String> extractTenantId() {
            SecurityContext secCtx = SecurityContextHolder.getContext();
            return Optional.ofNullable(secCtx.getAuthentication())
                    .filter(TenantAuthentication.class::isInstance)
                    .map(auth -> (TenantAuthentication<?>) auth)
                    .map(TenantAuthentication::getTenantId);
        }
    }
}
