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
package org.ehrbase.service;

import com.nedap.archie.rm.generic.PartyProxy;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.repository.PartyProxyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class PartyServiceImp implements IUserService, PartyService {
    private final IAuthenticationFacade authenticationFacade;
    private final TenantService tenantService;

    private final Cache userIdCache;

    private final PartyProxyRepository partyProxyRepository;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public PartyServiceImp(
            IAuthenticationFacade authenticationFacade,
            TenantService tenantService,
            CacheManager cacheManager,
            PartyProxyRepository partyProxyRepository) {
        this.authenticationFacade = authenticationFacade;
        this.tenantService = tenantService;
        this.partyProxyRepository = partyProxyRepository;

        this.userIdCache = cacheManager.getCache(CacheOptions.USER_ID_CACHE);
    }

    /**
     * Get default user UUID, derived from authenticated user via Spring Security.<br> Internally
     * checks and retrieves the matching user UUID, if it already exists with given info.
     * This operation is tenant aware.
     *
     * @return UUID of default user, derived from authenticated user.
     */
    @Override
    public UUID getCurrentUserId() {
        CacheKey<String> key =
                CacheKey.of(authenticationFacade.getAuthentication().getName(), tenantService.getCurrentSysTenant());
        return userIdCache.get(key, () -> getOrCreateCurrentUserIdSync(key));
    }

    private UUID getOrCreateCurrentUserIdSync(CacheKey<String> key) {

        return partyProxyRepository
                .findInternalUserId(key.getVal())
                .or(() -> {
                    try {
                        return Optional.of(partyProxyRepository.createInternalUser(key.getVal()));
                    } catch (DataIntegrityViolationException ex) {
                        logger.info(ex.getMessage(), ex.getMessage());
                        return partyProxyRepository.findInternalUserId(key.getVal());
                    }
                })
                .orElseThrow(() -> new InternalServerException("Cannot create User"));
    }

    @Override
    public UUID findOrCreateParty(PartyProxy partyProxy) {

        return partyProxyRepository.findMatching(partyProxy).orElse(partyProxyRepository.create(partyProxy));
    }
}
