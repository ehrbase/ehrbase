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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Component
public class AuditInterceptorHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ANY_SEGMENT = "*";
    private static final String ROLLBACK = "rollback";
    private static final String ANY_TRAILING_SEGMENTS = "**";

    @Value(API_CONTEXT_PATH_WITH_VERSION)
    protected String apiContextPath;

    @Value(ADMIN_API_CONTEXT_PATH)
    protected String adminApiContextPath;

    @Autowired(required = false)
    AuditEhrInterceptor ehrInterceptor;

    @Autowired(required = false)
    AuditQueryInterceptor queryInterceptor;

    @Autowired(required = false)
    AuditEhrAdminInterceptor ehrAdminInterceptor;

    @Autowired(required = false)
    AuditEhrStatusInterceptor ehrStatusInterceptor;

    @Autowired(required = false)
    AuditCompositionInterceptor compositionInterceptor;

    @Autowired(required = false)
    AuditContributionInterceptor contributionInterceptor;

    @Autowired(required = false)
    AuditCompensationInterceptor compensationInterceptor;

    @Autowired(required = false)
    AuditDirectoryInterceptor directoryInterceptor;

    public void registerAuditInterceptors(InterceptorRegistry registry) {
        if (shouldRegisterInterceptors()) {
            registerEhrInterceptor(registry);
            registerQueryInterceptor(registry);
            registerEhrAdminInterceptor(registry);
            registerEhrStatusInterceptor(registry);
            registerCompositionInterceptor(registry);
            registerContributionInterceptor(registry);
            registerCompensationInterceptor(registry);
            registerDirectoryInterceptor(registry);
        }
    }

    private void registerEhrStatusInterceptor(InterceptorRegistry registry) {
        if (ehrStatusInterceptor != null) {
            // Ehr Status and Versioned Ehr Status endpoints
            registry.addInterceptor(new AuditHandlerInterceptorDelegator(ehrStatusInterceptor))
                    .addPathPatterns(
                            contextPathPattern(EHR, ANY_SEGMENT, EHR_STATUS),
                            contextPathPattern(EHR, ANY_SEGMENT, EHR_STATUS, ANY_TRAILING_SEGMENTS))
                    .addPathPatterns(
                            contextPathPattern(EHR, ANY_SEGMENT, EHR_STATUS),
                            contextPathPattern(EHR, ANY_SEGMENT, VERSIONED_EHR_STATUS, ANY_TRAILING_SEGMENTS));
        } else {
            log.info("Ehr Status interceptor bean is not available.");
        }
    }

    private void registerQueryInterceptor(InterceptorRegistry registry) {
        if (queryInterceptor != null) {
            // Query endpoint
            registry.addInterceptor(new AuditHandlerInterceptorDelegator(queryInterceptor))
                    .addPathPatterns(contextPathPattern(QUERY, ANY_TRAILING_SEGMENTS))
                    .addPathPatterns(contextPathPattern(DEFINITION, QUERY, ANY_TRAILING_SEGMENTS));
        } else {
            log.info("Query interceptor bean is not available.");
        }
    }

    private void registerEhrAdminInterceptor(InterceptorRegistry registry) {
        if (ehrAdminInterceptor != null) {
            // Ehr admin endpoint
            registry.addInterceptor(new AuditHandlerInterceptorDelegator(ehrAdminInterceptor))
                    .addPathPatterns(contextAdminPathPattern(EHR), contextAdminPathPattern(EHR, ANY_SEGMENT));
        } else {
            log.info("Ehr admin interceptor bean is not available.");
        }
    }

    private void registerEhrInterceptor(InterceptorRegistry registry) {
        if (ehrInterceptor != null) {
            // Ehr endpoint
            registry.addInterceptor(new AuditHandlerInterceptorDelegator(ehrInterceptor))
                    .addPathPatterns(contextPathPattern(EHR), contextPathPattern(EHR, ANY_SEGMENT));
        } else {
            log.info("Ehr interceptor bean is not available.");
        }
    }

    private void registerContributionInterceptor(InterceptorRegistry registry) {
        if (contributionInterceptor != null) {
            // Contribution endpoint
            registry.addInterceptor(new AuditHandlerInterceptorDelegator(contributionInterceptor))
                    .addPathPatterns(contextPathPattern(EHR, ANY_SEGMENT, CONTRIBUTION, ANY_TRAILING_SEGMENTS))
                    .addPathPatterns(contextAdminPathPattern(EHR, ANY_SEGMENT, CONTRIBUTION, ANY_TRAILING_SEGMENTS));
        } else {
            log.info("Contribution interceptor bean is not available.");
        }
    }

    private void registerCompositionInterceptor(InterceptorRegistry registry) {
        if (compositionInterceptor != null) {
            // Composition endpoint
            registry.addInterceptor(new AuditHandlerInterceptorDelegator(compositionInterceptor))
                    .addPathPatterns(contextPathPattern(EHR, ANY_SEGMENT, COMPOSITION, ANY_TRAILING_SEGMENTS))
                    .addPathPatterns(contextPathPattern(EHR, ANY_SEGMENT, VERSIONED_COMPOSITION, ANY_TRAILING_SEGMENTS))
                    .addPathPatterns(contextAdminPathPattern(EHR, ANY_SEGMENT, COMPOSITION, ANY_SEGMENT));
        } else {
            log.info("Composition interceptor bean is not available.");
        }
    }

    private void registerCompensationInterceptor(InterceptorRegistry registry) {
        if (compensationInterceptor != null) {
            // Compensation plugin endpoint
            registry.addInterceptor(new AuditHandlerInterceptorDelegator(compensationInterceptor))
                    .addPathPatterns(getPathPattern("", EHR, ANY_SEGMENT, CONTRIBUTION, ANY_SEGMENT, ROLLBACK));
        } else {
            log.info("Compensation interceptor bean is not available.");
        }
    }

    private void registerDirectoryInterceptor(InterceptorRegistry registry) {
        if (directoryInterceptor != null) {
            // Directory plugin endpoint
            registry.addInterceptor(new AuditHandlerInterceptorDelegator(directoryInterceptor))
                    .addPathPatterns(contextPathPattern(EHR, ANY_SEGMENT, DIRECTORY))
                    .addPathPatterns(contextPathPattern(EHR, ANY_SEGMENT, DIRECTORY, ANY_SEGMENT))
                    .addPathPatterns(contextAdminPathPattern(EHR, ANY_SEGMENT, DIRECTORY, ANY_SEGMENT));
        } else {
            log.info("Directory interceptor bean is not available.");
        }
    }

    private boolean shouldRegisterInterceptors() {
        return compositionInterceptor != null
                || ehrInterceptor != null
                || ehrAdminInterceptor != null
                || queryInterceptor != null
                || contributionInterceptor != null
                || ehrStatusInterceptor != null
                || directoryInterceptor != null
                || compensationInterceptor != null;
    }

    private String contextPathPattern(String... segments) {
        return getPathPattern(apiContextPath, segments);
    }

    private String contextAdminPathPattern(String... segments) {
        return getPathPattern(adminApiContextPath, segments);
    }

    private String getPathPattern(String apiContextPath, String... segments) {
        return Stream.concat(Stream.of(apiContextPath), Arrays.stream(segments)).collect(Collectors.joining("/"));
    }
}
