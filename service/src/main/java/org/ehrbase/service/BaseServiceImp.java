/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
import org.ehrbase.api.service.BaseService;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_SystemAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyIdentified;
import org.ehrbase.dao.access.support.ServiceDataAccess;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseServiceImp implements BaseService {

    public static final String DEMOGRAPHIC = "DEMOGRAPHIC";
    public static final String PARTY = "PARTY";

    private final ServerConfig serverConfig;
    private final KnowledgeCacheService knowledgeCacheService;
    private final DSLContext context;

    private UUID systemId;

    @Autowired
    private IAuthenticationFacade authenticationFacade;

    public BaseServiceImp(KnowledgeCacheService knowledgeCacheService, DSLContext context, ServerConfig serverConfig) {
        this.knowledgeCacheService = knowledgeCacheService;
        this.context = context;
        this.serverConfig = serverConfig;
    }

    protected I_DomainAccess getDataAccess() {
        return new ServiceDataAccess(context, knowledgeCacheService, knowledgeCacheService, this.serverConfig);
    }

    /**
     * Get default system UUID.<br> Internally makes use of configured local system's node name.
     *
     * @return Default system UUID.
     */
    public UUID getSystemUuid() {
        if (systemId == null) {
            systemId = I_SystemAccess.createOrRetrieveLocalSystem(getDataAccess());
        }
        return systemId;
    }

    /**
     * Get default user UUID, derived from authenticated user via Spring Security.<br> Internally
     * checks and retrieves the matching user UUID, if it already exists with given info.
     *
     * @return UUID of default user, derived from authenticated user.
     */
    protected UUID getCurrentUserId(String tenantIdentifier) {
        var username = authenticationFacade.getAuthentication().getName();
        return new PersistedPartyIdentified(getDataAccess())
                .findInternalUserId(username)
                .orElseGet(() -> createInternalUser(username, tenantIdentifier));
    }

    /**
     * Creates a new PARTY_IDENTIFIED corresponding to an authenticated user.
     *
     * @param username username of the user
     * @return the id of the newly created user
     */
    protected UUID createInternalUser(String username, String tenantIdentifier) {
        var identifier = new DvIdentifier();
        identifier.setId(username);
        identifier.setIssuer(PersistedPartyIdentified.EHRBASE);
        identifier.setAssigner(PersistedPartyIdentified.EHRBASE);
        identifier.setType(PersistedPartyIdentified.SECURITY_USER_TYPE);

        PartyRef externalRef =
                new PartyRef(new GenericId(UuidGenerator.randomUUID().toString(), DEMOGRAPHIC), "User", PARTY);
        PartyIdentified user = new PartyIdentified(externalRef, "EHRbase Internal " + username, List.of(identifier));

        return new PersistedPartyIdentified(getDataAccess()).store(user, tenantIdentifier);
    }

    public ServerConfig getServerConfig() {
        return this.serverConfig;
    }
}
