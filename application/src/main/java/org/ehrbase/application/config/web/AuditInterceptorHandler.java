/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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

import static org.ehrbase.rest.BaseController.*;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ehrbase.api.audit.interceptor.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Component
public class AuditInterceptorHandler {

    private static final String ANY_SEGMENT = "*";
    private static final String ANY_TRAILING_SEGMENTS = "**";

    @Value(API_CONTEXT_PATH_WITH_VERSION)
    protected String apiContextPath;

    @Value(ADMIN_API_CONTEXT_PATH)
    protected String adminApiContextPath;

    @Autowired(required = false)
    AuditCompositionHandlerInterceptor сompositionInterceptor;

    @Autowired(required = false)
    AuditEhrHandlerInterceptor ehrInterceptor;

    @Autowired(required = false)
    AuditEhrStatusHandlerInterceptor ehrStatusInterceptor;

    @Autowired(required = false)
    AuditQueryHandlerInterceptor queryInterceptor;

    @Autowired(required = false)
    AuditEhrAdminHandlerInterceptor ehrAdminInterceptor;

    public void registerAuditInterceptors(InterceptorRegistry registry) {
        if (shouldRegisterInterceptors()) {
            // Composition endpoint
            registry.addInterceptor(сompositionInterceptor)
                    .addPathPatterns(contextPathPattern(EHR, ANY_SEGMENT, COMPOSITION, ANY_TRAILING_SEGMENTS));
            // Ehr endpoint
            registry.addInterceptor(ehrInterceptor)
                    .addPathPatterns(contextPathPattern(EHR), contextPathPattern(EHR, ANY_SEGMENT));
            // Ehr admin endpoint
            registry.addInterceptor(ehrAdminInterceptor)
                    .addPathPatterns(contextAdminPathPattern(EHR), contextAdminPathPattern(EHR, ANY_SEGMENT));
            // Query endpoint
            registry.addInterceptor(queryInterceptor).addPathPatterns(contextPathPattern(QUERY, ANY_TRAILING_SEGMENTS));
            // Ehr Status and Versioned Ehr Status endpoints
            registry.addInterceptor(ehrStatusInterceptor)
                    .addPathPatterns(
                            contextPathPattern(EHR, ANY_SEGMENT, EHR_STATUS),
                            contextPathPattern(EHR, ANY_SEGMENT, EHR_STATUS, ANY_TRAILING_SEGMENTS))
                    .addPathPatterns(
                            contextPathPattern(EHR, ANY_SEGMENT, EHR_STATUS),
                            contextPathPattern(EHR, ANY_SEGMENT, VERSIONED_EHR_STATUS, ANY_TRAILING_SEGMENTS));
        }
    }

    private boolean shouldRegisterInterceptors() {
        return сompositionInterceptor != null
                || ehrInterceptor != null
                || ehrAdminInterceptor != null
                || queryInterceptor != null
                || ehrStatusInterceptor != null;
    }

    private String contextPathPattern(String... segments) {
        return getPathPattern(apiContextPath, segments);
    }

    private String contextAdminPathPattern(String... segments) {
        return getPathPattern(adminApiContextPath, segments);
    }

    private String getPathPattern(String apiContextPath, String[] segments) {
        return Stream.concat(Stream.of(apiContextPath), Arrays.stream(segments)).collect(Collectors.joining("/"));
    }
}
