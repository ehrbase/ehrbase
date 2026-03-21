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

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Defense-in-depth tenant validation. Every repository method should call
 * {@link #assertTenantMatch(short)} on returned entities to verify that
 * the entity's tenant matches the current request tenant.
 *
 * <p>This is the application-layer belt to RLS's suspenders.
 */
@Component
public class TenantGuard {

    private static final Logger log = LoggerFactory.getLogger(TenantGuard.class);

    private final RequestContext requestContext;
    private final AuditEventService auditEventService;

    public TenantGuard(RequestContext requestContext, AuditEventService auditEventService) {
        this.requestContext = requestContext;
        this.auditEventService = auditEventService;
    }

    /**
     * Asserts that the entity's tenant matches the current request tenant.
     * Throws {@link SecurityException} on mismatch and logs a security violation audit event.
     *
     * @param entityTenant the tenant ID from the database entity
     * @throws SecurityException if the tenant does not match
     */
    public void assertTenantMatch(short entityTenant) {
        short requestTenant = requestContext.getTenantId();
        if (entityTenant != requestTenant) {
            log.error(
                    "SECURITY VIOLATION: tenant mismatch — entity tenant={}, request tenant={}, user={}",
                    entityTenant,
                    requestTenant,
                    requestContext.getUserId());

            auditEventService.recordEvent(
                    "security_violation",
                    "tenant",
                    null,
                    "tenant_mismatch",
                    null,
                    Map.of(
                            "entity_tenant", entityTenant,
                            "request_tenant", requestTenant,
                            "user_id", requestContext.getUserId()));

            throw new SecurityException("Tenant mismatch: entity tenant=%d does not match request tenant=%d"
                    .formatted(entityTenant, requestTenant));
        }
    }
}
