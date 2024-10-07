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

import java.util.Optional;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.repository.PartyProxyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImp implements UserService {
    private final IAuthenticationFacade authenticationFacade;

    private final CacheProvider cacheProvider;

    private final PartyProxyRepository partyProxyRepository;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public UserServiceImp(
            IAuthenticationFacade authenticationFacade,
            CacheProvider cacheProvider,
            PartyProxyRepository partyProxyRepository) {
        this.authenticationFacade = authenticationFacade;
        this.cacheProvider = cacheProvider;
        this.partyProxyRepository = partyProxyRepository;
    }

    /**
     * Get default user UUID, derived from authenticated user via Spring Security.<br> Internally
     * checks and retrieves the matching user UUID, if it already exists with given info.
     *
     * @return UUID of default user, derived from authenticated user.
     */
    @Override
    public UserAndCommitterId getCurrentUserAndCommitterId() {
        String key = authenticationFacade.getAuthentication().getName();
        return CacheProvider.USER_ID_CACHE.get(cacheProvider, key, () -> getOrCreateCurrentUserId(key));
    }

    private UserAndCommitterId getOrCreateCurrentUserId(String key) {

        return partyProxyRepository
                .findInternalUserAndCommitterId(key)
                .or(() -> {
                    try {
                        return Optional.of(partyProxyRepository.createInternalUser(key));
                    } catch (DataIntegrityViolationException e) {
                        logger.info(e.getMessage(), e);
                        return partyProxyRepository.findInternalUserAndCommitterId(key);
                    }
                })
                .orElseThrow(() -> new InternalServerException("Cannot create User"));
    }
}
