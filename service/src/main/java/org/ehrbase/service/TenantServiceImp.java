/*
 * Copyright (c) 2019-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.service;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.api.tenant.Tenant;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.dao.access.interfaces.I_TenantAccess;
import org.ehrbase.tenant.DefaultTenantAuthentication;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link TenantService} implementation.
 */
@Service
@Transactional
public class TenantServiceImp extends BaseServiceImp implements TenantService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ERR_GETTING_TENANT = "Could not retrieve system tenant cache for tenant id: %s";
    private static final String WARN_NOT_TENANT_ID =
            "No tenant identifier provided, falling back to default tenant identifier {}";

    private final Cache sysTenantCache;

    public TenantServiceImp(
            @Lazy KnowledgeCacheService knowledgeCacheService,
            CacheManager cacheManager,
            DSLContext context,
            ServerConfig serverConfig) {
        super(knowledgeCacheService, context, serverConfig);
        this.sysTenantCache = cacheManager.getCache(CacheOptions.SYS_TENANT);
    }

    @Override
    public Short getCurrentSysTenant() {
        return retrieveFromCache(getCurrentTenantIdentifier());
    }

    @Override
    public Short getSysTenantByTenantId(String tenantId) {
        return retrieveFromCache(tenantId);
    }

    private Short retrieveFromCache(String tenantId) {
        return Optional.ofNullable(sysTenantCache.get(tenantId))
                .map(Cache.ValueWrapper::get)
                .map(Short.class::cast)
                .orElseThrow(() -> new IllegalArgumentException(String.format(ERR_GETTING_TENANT, tenantId)));
    }

    @Override
    public String getCurrentTenantIdentifier() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(DefaultTenantAuthentication.class::isInstance)
                .map(DefaultTenantAuthentication.class::cast)
                .map(DefaultTenantAuthentication::getTenantId)
                .filter(StringUtils::isNotEmpty)
                .orElseGet(() -> {
                    log.warn(WARN_NOT_TENANT_ID, TenantAuthentication.getDefaultTenantId());
                    return TenantAuthentication.getDefaultTenantId();
                });
    }

    @Override
    public String create(Tenant tenant) {
        I_TenantAccess tenantAccess =
                I_TenantAccess.getNewInstance(getDataAccess().getContext(), tenant);
        Short sysTenant = tenantAccess.commit();
        sysTenantCache.put(tenant.getTenantId(), sysTenant);

        return tenant.getTenantId();
    }

    @Override
    public List<Tenant> getAll() {
        return I_TenantAccess.getAll(getDataAccess().getContext()).stream()
                .map(I_TenantAccess::convert)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Tenant> findBy(String tenantId) {
        return Optional.ofNullable(
                        I_TenantAccess.retrieveInstanceBy(getDataAccess().getContext(), tenantId))
                .map(I_TenantAccess::convert);
    }

    @Override
    public Tenant update(Tenant tenant) {
        return I_TenantAccess.retrieveInstanceBy(getDataAccess().getContext(), tenant.getTenantId())
                .update(tenant);
    }

    @Override
    public Map<String, Short> getSysTenants() {
        return I_TenantAccess.getSysTenants(getDataAccess().getContext());
    }
}
