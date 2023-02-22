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

import java.util.UUID;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.service.BaseService;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_SystemAccess;
import org.ehrbase.dao.access.support.ServiceDataAccess;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

public class BaseServiceImp implements BaseService {

    public static final String DEMOGRAPHIC = "DEMOGRAPHIC";
    public static final String PARTY = "PARTY";

    private final ServerConfig serverConfig;
    private final KnowledgeCacheService knowledgeCacheService;
    private final DSLContext context;

    private UUID systemId;

    @Lazy
    @Autowired
    private IUserService userService;

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
    protected UUID getCurrentUserId() {
        return userService.getCurrentUserId();
    }

    public ServerConfig getServerConfig() {
        return this.serverConfig;
    }
}
