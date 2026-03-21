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
package org.ehrbase.configuration.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.ehrbase.api.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts tenant, user, and role information from the request and sets them in {@link TenantContext}.
 * The JOOQ connection provider reads these values and executes SET LOCAL for PostgreSQL RLS policies.
 *
 * <p>This filter MUST run after authentication but before any repository access.
 *
 * <p>Tenant resolution order:
 * <ol>
 *   <li>JWT claim "tenant_id"</li>
 *   <li>HTTP header "X-Tenant-Id"</li>
 *   <li>Default tenant (ID 1) for single-tenant deployments</li>
 * </ol>
 */
public class TenantSettingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantSettingFilter.class);

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String JWT_CLAIM_TENANT_ID = "tenant_id";

    private final boolean multiTenantEnabled;

    public TenantSettingFilter(boolean multiTenantEnabled) {
        this.multiTenantEnabled = multiTenantEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            short tenantId = resolveTenantId(request);
            String userId = resolveUserId();
            String userRole = resolveUserRole();

            TenantContext.set(tenantId, userId, userRole);
            log.trace("Tenant context set: tenant={}, user={}, role={}", tenantId, userId, userRole);

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private short resolveTenantId(HttpServletRequest request) {
        // 1. Try JWT claim
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Object tenantClaim = jwtAuth.getToken().getClaim(JWT_CLAIM_TENANT_ID);
            if (tenantClaim != null) {
                return Short.parseShort(tenantClaim.toString());
            }
        }

        // 2. Try HTTP header
        String headerValue = request.getHeader(HEADER_TENANT_ID);
        if (headerValue != null && !headerValue.isBlank()) {
            return Short.parseShort(headerValue.strip());
        }

        // 3. Default for single-tenant
        if (!multiTenantEnabled) {
            return TenantContext.DEFAULT_TENANT_ID;
        }

        // Multi-tenant mode requires explicit tenant
        log.warn("No tenant ID found in request (JWT claim or header) in multi-tenant mode");
        return TenantContext.DEFAULT_TENANT_ID;
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }

    private String resolveUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5).toLowerCase())
                    .findFirst()
                    .orElse("user");
        }
        return "user";
    }
}
