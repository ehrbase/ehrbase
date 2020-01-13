/*
 * Copyright (c) 2019 Vitasystems GmbH,
 * Jake Smolka (Hannover Medical School),
 * Luis Marco-Ruiz (Hannover Medical School),
 * Stefan Spiska (Vitasystems GmbH).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.service;

import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_PartyIdentifiedAccess;
import org.ehrbase.dao.access.interfaces.I_SystemAccess;
import org.ehrbase.dao.access.support.ServiceDataAccess;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

public class BaseService {

    public static final String DEMOGRAPHIC = "DEMOGRAPHIC";
    public static final String PARTY = "PARTY";

    @Value("${system.type}")
    private String systemType = "POSTGRES";
    @Value("${spring.datasource.url}")
    private String datasourceUrl = "url";
    @Value("${spring.datasource.password}")
    private String datasourcePass = "luis";
    @Value("${spring.datasource.username}")
    private String datasourceUser = "luis";

    private final ServerConfig serverConfig;
    private final KnowledgeCacheService knowledgeCacheService;
    private final DSLContext context;

    public BaseService(KnowledgeCacheService knowledgeCacheService, DSLContext context, ServerConfig serverConfig) {
        this.knowledgeCacheService = knowledgeCacheService;
        this.context = context;
        this.serverConfig = serverConfig;
    }

    protected I_DomainAccess getDataAccess() {
        return new ServiceDataAccess(context, knowledgeCacheService, knowledgeCacheService, this.serverConfig);
    }

    public UUID getSystemUuid() {
        return I_SystemAccess.createOrRetrieveLocalSystem(getDataAccess());
    }

    protected UUID getUserUuid() {
        //@TODO READ from Spring Security
        return I_PartyIdentifiedAccess.getOrCreatePartyByExternalRef(getDataAccess(), null, "cbf741ff-9480-4792-8894-13fc5f818b6d", DEMOGRAPHIC, "User", PARTY);
    }

    public ServerConfig getServerConfig() {
        return this.serverConfig;
    }

}
