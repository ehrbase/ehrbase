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
package org.ehrbase.api.tenant;

/**
 * Holds the current tenant context for the request.
 * Used by {@link org.ehrbase.configuration.config.security.TenantSettingFilter} to propagate
 * tenant information to the JOOQ connection provider, which executes SET LOCAL for RLS policies.
 *
 * <p>ThreadLocal is safe with Java 25 virtual threads — each virtual thread has its own state.
 */
public final class TenantContext {

    public static final short DEFAULT_TENANT_ID = 1;

    private static final ThreadLocal<TenantInfo> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(short tenantId, String userId, String userRole) {
        CURRENT.set(new TenantInfo(tenantId, userId, userRole));
    }

    public static TenantInfo get() {
        return CURRENT.get();
    }

    public static short getTenantId() {
        TenantInfo info = CURRENT.get();
        return info != null ? info.tenantId() : DEFAULT_TENANT_ID;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record TenantInfo(short tenantId, String userId, String userRole) {}
}
