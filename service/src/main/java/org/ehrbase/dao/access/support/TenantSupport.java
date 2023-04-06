/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.dao.access.support;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.dao.access.interfaces.I_TenantAccess;
import org.ehrbase.functional.Try;
import org.ehrbase.service.ApplicationContextProvider;
import org.ehrbase.tenant.DefaultTenantAuthentication;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TenantSupport {

    private TenantSupport() {
        // NOP
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantSupport.class);

    private static final String WARN_NOT_TENANT_ID =
            "No tenant identifier provided, falling back to default tenant identifier {}";
    private static final String COULD_NOT_FOUND_TENANT = "Could not found tenant by tenant identifier provided %s";
    private static final String WARN_NOT_SYS_TENANT =
            "Could not found sys tenant by tenant identifier provided, falling back to default sys tenant {}";

    public static String currentTenantIdentifier() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Objects::nonNull)
                .filter(DefaultTenantAuthentication.class::isInstance)
                .map(DefaultTenantAuthentication.class::cast)
                .map(DefaultTenantAuthentication::getTenantId)
                .filter(StringUtils::isNotEmpty)
                .orElseGet(() -> {
                    LOGGER.warn(WARN_NOT_TENANT_ID, TenantAuthentication.getDefaultTenantId());
                    return TenantAuthentication.getDefaultTenantId();
                });
    }

    private static final String ERR_TENANT_ID_MISSMATCH = "Provided tenant id[%s] does not match session tenant id[%s]";

    public static Try<Short, InternalServerException> isValidTenantId(Short tenantId, Supplier<Short> currentTenant) {
        Short currentTenantIdentifier = currentTenant.get();

        return currentTenantIdentifier.equals(tenantId)
                ? Try.success(tenantId)
                : Try.failure(new InternalServerException(
                        String.format(ERR_TENANT_ID_MISSMATCH, tenantId, currentTenantIdentifier)));
    }

    public static Short currentSysTenant(DSLContext ctx) {

        String tenantId = currentTenantIdentifier();
        Short sysTenant;
        Optional<Cache> tenantCache = Optional.ofNullable(ApplicationContextProvider.getApplicationContext())
                .map(applicationContext -> applicationContext.getBean(CacheManager.class))
                .map(cacheManager -> cacheManager.getCache(CacheOptions.SYS_TENANT));

        if (tenantCache.isPresent()) {
            try {
                sysTenant = tenantCache
                        .get()
                        .get(tenantId, () -> I_TenantAccess.retrieveSysTenantByTenantId(ctx, tenantId));
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format(COULD_NOT_FOUND_TENANT, tenantId));
            }
        } else {
            // TODO:: clarify.....this is reproducible if run this method from the tenant service...during the cache
            // initialization post processors still not initialized so  getApplicationContext will return null
            try {
                sysTenant = I_TenantAccess.retrieveSysTenantByTenantId(ctx, tenantId);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format(COULD_NOT_FOUND_TENANT, tenantId));
            }
        }

        if (sysTenant != null) {
            return sysTenant;
        }

        LOGGER.warn(WARN_NOT_SYS_TENANT, TenantAuthentication.getDefaultSysTenant());
        return TenantAuthentication.getDefaultSysTenant();
    }
}
