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
import java.util.UUID;
import java.util.stream.Collectors;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.api.tenant.Tenant;
import org.ehrbase.dao.access.interfaces.I_TenantAccess;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link TenantService} implementation.
 */
@Service
@Transactional
public class TenantServiceImp extends BaseServiceImp implements TenantService {

    public TenantServiceImp(
            @Lazy KnowledgeCacheService knowledgeCacheService, DSLContext context, ServerConfig serverConfig) {
        super(knowledgeCacheService, context, serverConfig);
    }

    @Override
    public String getCurrentTenantIdentifier() {
        return I_TenantAccess.currentTenantIdentifier();
    }

    @Override
    public UUID create(Tenant tenant) {
        I_TenantAccess tenantAccess =
                I_TenantAccess.getNewInstance(getDataAccess().getContext(), tenant);
        return tenantAccess.commit();
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
