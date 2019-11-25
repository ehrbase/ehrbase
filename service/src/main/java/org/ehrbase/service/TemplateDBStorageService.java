/*
 *
 *   Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
 *
 *   This file is part of project EHRbase
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.ehrbase.service;

import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_TemplateStoreAccess;
import org.ehrbase.dao.access.support.ServiceDataAccess;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.jooq.DSLContext;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TemplateDBStorageService implements TemplateStorage {
    private final DSLContext context;
    private final ServerConfig serverConfig;

    @Autowired
    public TemplateDBStorageService(DSLContext context, ServerConfig serverConfig) {
        this.context = context;
        this.serverConfig = serverConfig;
    }

    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {

        return I_TemplateStoreAccess.fetchAll(getDataAccess());
    }


    @Override
    public void storeTemplate(OPERATIONALTEMPLATE template) {

        I_TemplateStoreAccess.getInstance(getDataAccess(), template).commit();
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> readOperationaltemplate(String templateId) {
        return Optional.ofNullable(I_TemplateStoreAccess.retrieveInstanceByTemplateId(getDataAccess(), templateId).getTemplate());
    }

    protected I_DomainAccess getDataAccess() {
        return new ServiceDataAccess(context, null, null, this.serverConfig);
    }
}
