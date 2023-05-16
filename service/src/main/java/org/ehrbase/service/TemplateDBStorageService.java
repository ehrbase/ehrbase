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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_TemplateStoreAccess;
import org.ehrbase.dao.access.support.ServiceDataAccess;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.jooq.DSLContext;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TemplateDBStorageService implements TemplateStorage {
    private final DSLContext context;
    private final ServerConfig serverConfig;

    public TemplateDBStorageService(DSLContext context, ServerConfig serverConfig) {
        this.context = context;
        this.serverConfig = serverConfig;
    }

    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        return I_TemplateStoreAccess.fetchAll(getDataAccess());
    }

    @Override
    public Set<String> findAllTemplateIds() {
        return I_TemplateStoreAccess.fetchAllTemplateIds(getDataAccess());
    }

    @Override
    public void storeTemplate(OPERATIONALTEMPLATE template, Short sysTenant) {
        if (readOperationaltemplate(template.getTemplateId().getValue()).isPresent()) {
            I_TemplateStoreAccess.getInstance(getDataAccess(), template, sysTenant)
                    .update();
        } else {
            I_TemplateStoreAccess.getInstance(getDataAccess(), template, sysTenant)
                    .commit();
        }
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> readOperationaltemplate(String templateId) {
        return Optional.ofNullable(I_TemplateStoreAccess.retrieveInstanceByTemplateId(getDataAccess(), templateId)
                .getTemplate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String adminUpdateTemplate(OPERATIONALTEMPLATE template) {
        return I_TemplateStoreAccess.adminUpdateTemplate(getDataAccess(), template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteTemplate(String templateId) {

        return I_TemplateStoreAccess.deleteTemplate(getDataAccess(), templateId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int adminDeleteAllTemplates(List<TemplateMetaData> templateMetaDataList) {
        return I_TemplateStoreAccess.adminDeleteAllTemplates(getDataAccess());
    }

    protected I_DomainAccess getDataAccess() {
        return new ServiceDataAccess(context, null, null, this.serverConfig);
    }
}
