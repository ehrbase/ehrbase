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

import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.util.List;
import java.util.UUID;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyIdentified;
import org.ehrbase.dao.access.support.ServiceDataAccess;
import org.ehrbase.rest.util.AuthHelper;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
// This service is not @Transactional since we only want to get DB connections when we really need to and an already
// running transaction is propagated anyway
public class UserService implements IUserService {
    private final IAuthenticationFacade authenticationFacade;
    private final TenantService tenantService;
    private final I_DomainAccess dataAccess;
    private final Cache userIdCache;

    public UserService(
            IAuthenticationFacade authenticationFacade,
            TenantService tenantService,
            KnowledgeCacheService knowledgeCacheService,
            DSLContext context,
            ServerConfig serverConfig,
            CacheManager cacheManager) {
        this.authenticationFacade = authenticationFacade;
        this.tenantService = tenantService;
        this.dataAccess = new ServiceDataAccess(context, knowledgeCacheService, knowledgeCacheService, serverConfig);
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
                AuthHelper.getCurrentAuthenticatedUsername(authenticationFacade.getAuthentication()),
                tenantService.getCurrentTenantIdentifier());
        return userIdCache.get(key, () -> getOrCreateCurrentUserIdSync(key));
    }

    private UUID getOrCreateCurrentUserIdSync(CacheKey<String> key) {
        var existingUser = new PersistedPartyIdentified(dataAccess).findInternalUserId(key.getVal());
        return existingUser.orElseGet(() -> createUserInternal(key));
    }

    /**
     * Creates a new PARTY_IDENTIFIED corresponding to an authenticated user.
     *
     * @param username username of the user
     * @return the id of the newly created user
     */
    private UUID createUserInternal(CacheKey<String> key) {
        DvIdentifier identifier = new DvIdentifier();
        identifier.setId(key.getVal());
        identifier.setIssuer(PersistedPartyIdentified.EHRBASE);
        identifier.setAssigner(PersistedPartyIdentified.EHRBASE);
        identifier.setType(PersistedPartyIdentified.SECURITY_USER_TYPE);

        PartyRef externalRef = new PartyRef(
                new GenericId(UuidGenerator.randomUUID().toString(), BaseServiceImp.DEMOGRAPHIC),
                "User",
                BaseServiceImp.PARTY);
        PartyIdentified user =
                new PartyIdentified(externalRef, "EHRbase Internal " + key.getVal(), List.of(identifier));

        return new PersistedPartyIdentified(dataAccess).store(user, key.getTenantId());
    }
}
