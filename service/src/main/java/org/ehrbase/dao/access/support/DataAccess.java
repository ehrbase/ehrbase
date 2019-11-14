/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School), Luis Marco-Ruiz (Hannover Medical School).

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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
package org.ehrbase.dao.access.support;

import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.IntrospectService;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

import java.sql.Connection;

/**
 * Created by Christian Chevalley on 4/21/2015.
 */
public abstract class DataAccess implements I_DomainAccess {

    private final DSLContext context;
    private final I_KnowledgeCache knowledgeManager;
    private final IntrospectService introspectService;

    private final ServerConfig serverConfig;

    public DataAccess(DSLContext context, I_KnowledgeCache knowledgeManager, IntrospectService introspectService, ServerConfig serverConfig) {
        this.context = context;
        this.knowledgeManager = knowledgeManager;
        this.introspectService = introspectService;
        this.serverConfig = serverConfig;
    }

    public DataAccess(I_DomainAccess domainAccess) {
        this.context = domainAccess.getContext();
        this.knowledgeManager = domainAccess.getKnowledgeManager();
        this.introspectService = domainAccess.getIntrospectService();
        this.serverConfig = domainAccess.getServerConfig();
    }

    @Override
    public SQLDialect getDialect() {
        return context.dialect();
    }

    @Override
    public Connection getConnection() {
        return context.configuration().connectionProvider().acquire();
    }

    @Override
    public DSLContext getContext() {
        return context;
    }

    @Override
    public I_KnowledgeCache getKnowledgeManager() {
        return knowledgeManager;
    }

    @Override
    public IntrospectService getIntrospectService() {
        return introspectService;
    }

    @Override
    public ServerConfig getServerConfig() {
        return this.serverConfig;
    }

}
