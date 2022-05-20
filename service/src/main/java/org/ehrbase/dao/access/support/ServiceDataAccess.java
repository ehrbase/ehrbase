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
package org.ehrbase.dao.access.support;

import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.IntrospectService;
import org.jooq.DSLContext;

/**
 * A simple wrapper to encapsulate resource accesses from services
 * ETHERCIS Project ehrservice
 * Created by Christian Chevalley on 6/30/2015.
 */
public class ServiceDataAccess extends DataAccess {

    public ServiceDataAccess(
            DSLContext context,
            I_KnowledgeCache knowledgeManager,
            IntrospectService introspectService,
            ServerConfig serverConfig) {
        super(context, knowledgeManager, introspectService, serverConfig);
    }

    public ServiceDataAccess(I_DomainAccess dataAccess) {
        super(dataAccess);
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }
}
