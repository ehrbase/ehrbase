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
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyIdentified;
import org.ehrbase.dao.access.support.ServiceDataAccess;
import org.jooq.DSLContext;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
// This service is not @Transactional since we only want to get DB connections when we really need to and an already
// running transaction is propagated anyway
public class UserService {
    private final IAuthenticationFacade authenticationFacade;
    private final I_DomainAccess dataAccess;
    private final Cache userIdCache;

    public UserService(
            IAuthenticationFacade authenticationFacade,
            KnowledgeCacheService knowledgeCacheService,
            DSLContext context,
            ServerConfig serverConfig,
            CacheManager cacheManager) {
        this.authenticationFacade = authenticationFacade;
        this.dataAccess = new ServiceDataAccess(context, knowledgeCacheService, knowledgeCacheService, serverConfig);
        this.userIdCache = cacheManager.getCache(CacheOptions.USER_ID_CACHE);
    }

    /**
     * Get default user UUID, derived from authenticated user via Spring Security.<br> Internally
     * checks and retrieves the matching user UUID, if it already exists with given info.
     *
     * @return UUID of default user, derived from authenticated user.
     */
    public UUID getCurrentUserId() {
        final String username = authenticationFacade.getAuthentication().getName();
        return userIdCache.get(username, () -> getOrCreateCurrentUserIdSnyc(username));
    }

    private synchronized UUID getOrCreateCurrentUserIdSnyc(String username) {
        var existingUser = new PersistedPartyIdentified(dataAccess).findInternalUserId(username);
        if (existingUser.isEmpty()) {
            return createUserInternal(username);
        }
        return existingUser.get();
    }

    /**
     * Creates a new PARTY_IDENTIFIED corresponding to an authenticated user.
     *
     * @param username username of the user
     * @return the id of the newly created user
     */
    private UUID createUserInternal(String username) {
        DvIdentifier identifier = new DvIdentifier();
        identifier.setId(username);
        identifier.setIssuer(PersistedPartyIdentified.EHRBASE);
        identifier.setAssigner(PersistedPartyIdentified.EHRBASE);
        identifier.setType(PersistedPartyIdentified.SECURITY_USER_TYPE);

        PartyRef externalRef = new PartyRef(
                new GenericId(UUID.randomUUID().toString(), BaseServiceImp.DEMOGRAPHIC), "User", BaseServiceImp.PARTY);
        PartyIdentified user = new PartyIdentified(externalRef, "EHRbase Internal " + username, List.of(identifier));

        return new PersistedPartyIdentified(dataAccess).store(user);
    }
}
