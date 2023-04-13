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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.api.tenant.Tenant;
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.dao.access.interfaces.I_TenantAccess;
import org.jooq.DSLContext;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link TenantService} implementation.
 */
@Service
@Transactional
public class TenantServiceImp extends BaseServiceImp implements TenantService {

    private final Cache sysTenant;

    public TenantServiceImp(
            @Lazy KnowledgeCacheService knowledgeCacheService,
            CacheManager cacheManager,
            DSLContext context,
            ServerConfig serverConfig) {
        super(knowledgeCacheService, context, serverConfig);
        this.sysTenant = cacheManager.getCache(CacheOptions.SYS_TENANT);
    }

    @Override
    public Short getCurrentSysTenant() {
        String tenantId = I_TenantAccess.currentTenantIdentifier();
        return getTenantByTenantId(
                tenantId,
                tenant -> I_TenantAccess.retrieveSysTenantByTenantId(
                        super.getDataAccess().getContext(), tenantId));
    }

    private Short getTenantByTenantId(String tenantId, Function<String, Short> provider) {
        return sysTenant.get(tenantId, () -> provider.apply(tenantId));
    }

    @Override
    public String getCurrentTenantIdentifier() {
        return I_TenantAccess.currentTenantIdentifier();
    }

    @Override
    public Short create(Tenant tenant) {
        I_TenantAccess tenantAccess =
                I_TenantAccess.getNewInstance(getDataAccess().getContext(), tenant);
        Short savedSysTenant = tenantAccess.commit();
        sysTenant.put(tenant.getTenantId(), savedSysTenant);

        return savedSysTenant;
    }

    @Override
    public List<Tenant> getAll() {
        return I_TenantAccess.getAll(getDataAccess().getContext()).stream()
                .map(ta -> ta.convert())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Tenant> findBy(String tenantId) {
        return Optional.ofNullable(
                        I_TenantAccess.retrieveInstanceBy(getDataAccess().getContext(), tenantId))
                .map(acc -> acc.convert());
    }

    @Override
    public Tenant update(Tenant tenant) {
        return I_TenantAccess.retrieveInstanceBy(getDataAccess().getContext(), tenant.getTenantId())
                .update(tenant);
    }
}
