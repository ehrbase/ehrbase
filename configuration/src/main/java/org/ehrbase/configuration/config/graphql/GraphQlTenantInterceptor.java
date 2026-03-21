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
package org.ehrbase.configuration.config.graphql;

import org.ehrbase.api.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * GraphQL interceptor that sets {@link TenantContext} for every GraphQL request,
 * including WebSocket subscription frames.
 *
 * <p>For HTTP requests, the {@code TenantSettingFilter} already handles this.
 * This interceptor ensures WebSocket-based subscriptions also have tenant context.
 */
@Component
public class GraphQlTenantInterceptor implements WebGraphQlInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GraphQlTenantInterceptor.class);

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        TenantContext.TenantInfo existing = TenantContext.get();
        if (existing != null) {
            return chain.next(request);
        }

        short tenantId = resolveTenantId();
        String userId = resolveUserId();
        String userRole = resolveUserRole();

        TenantContext.set(tenantId, userId, userRole);
        log.trace("GraphQL tenant context set: tenant={}, user={}, role={}", tenantId, userId, userRole);

        return chain.next(request).doFinally(signal -> TenantContext.clear());
    }

    private short resolveTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Object tenantClaim = jwtAuth.getToken().getClaim("tenant_id");
            if (tenantClaim != null) {
                return Short.parseShort(tenantClaim.toString());
            }
        }
        return TenantContext.DEFAULT_TENANT_ID;
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
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
