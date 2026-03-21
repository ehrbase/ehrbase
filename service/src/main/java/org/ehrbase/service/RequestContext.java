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
package org.ehrbase.service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.ehrbase.api.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Request-scoped bean providing per-request metadata.
 * Replaces the ThreadLocal-based {@code HttpRestContext}.
 *
 * <p>Compatible with Java 25 virtual threads — Spring manages lifecycle per-request.
 * Testable with {@code @WebMvcTest} Spring test context.
 */
@RequestScope
@Component
public class RequestContext {

    private short tenantId;
    private String userId;
    private String userRole;
    private String ipAddress;
    private String userAgent;

    // Mutable fields set by controllers during request processing
    private UUID ehrId;
    private UUID compositionId;
    private String templateId;

    @PostConstruct
    private void init() {
        // Populate from TenantContext (set by TenantSettingFilter)
        TenantContext.TenantInfo tenantInfo = TenantContext.get();
        if (tenantInfo != null) {
            this.tenantId = tenantInfo.tenantId();
            this.userId = tenantInfo.userId();
            this.userRole = tenantInfo.userRole();
        } else {
            this.tenantId = TenantContext.DEFAULT_TENANT_ID;
            this.userId = resolveUserId();
            this.userRole = resolveUserRole();
        }

        // Populate from HttpServletRequest
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            this.ipAddress = request.getRemoteAddr();
            this.userAgent = request.getHeader("User-Agent");
        }
    }

    public short getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public UUID getEhrId() {
        return ehrId;
    }

    public void setEhrId(UUID ehrId) {
        this.ehrId = ehrId;
    }

    public UUID getCompositionId() {
        return compositionId;
    }

    public void setCompositionId(UUID compositionId) {
        this.compositionId = compositionId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    private static String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }

    private static String resolveUserRole() {
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
