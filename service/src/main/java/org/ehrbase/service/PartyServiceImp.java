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
import java.util.UUID;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.repository.PartyProxyRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class PartyServiceImp implements IUserService, PartyService {
    private final IAuthenticationFacade authenticationFacade;
    private final TenantService tenantService;

    private final Cache userIdCache;

    private final PartyProxyRepository partyProxyRepository;

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
        CacheKey<String> key = CacheKey.of(
                authenticationFacade.getAuthentication().getName(), tenantService.getCurrentTenantIdentifier());
        return userIdCache.get(key, () -> getOrCreateCurrentUserIdSync(key));
    }

    private UUID getOrCreateCurrentUserIdSync(CacheKey<String> key) {

        var existingUser = partyProxyRepository.findInternalUserId(key.getVal());

        return existingUser.orElseGet(() -> createUserInternal(key));
    }

    /**
     * Creates a new PARTY_IDENTIFIED corresponding to an authenticated user.
     *
     * @param username username of the user
     * @return the id of the newly created user
     */
    private UUID createUserInternal(CacheKey<String> key) {

        return partyProxyRepository.createInternalUser(key.getVal());
    }

    @Override
    public UUID findOrCreateParty(PartyProxy partyProxy) {

        return partyProxyRepository.findMatching(partyProxy).orElse(partyProxyRepository.create(partyProxy));
    }
}
