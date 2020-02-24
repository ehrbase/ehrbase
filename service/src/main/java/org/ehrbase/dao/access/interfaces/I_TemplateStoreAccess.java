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

package org.ehrbase.dao.access.interfaces;

import org.ehrbase.dao.access.jooq.TemplateStoreAccess;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.util.List;

public interface I_TemplateStoreAccess extends I_SimpleCRUD {

    OPERATIONALTEMPLATE getTemplate();

    void setTemplate(OPERATIONALTEMPLATE template);

    static I_TemplateStoreAccess getInstance(I_DomainAccess access, OPERATIONALTEMPLATE operationaltemplate) {
        return new TemplateStoreAccess(access, operationaltemplate);
    }

    static I_TemplateStoreAccess retrieveInstanceByTemplateId(I_DomainAccess domainAccess, String templateId) {
        return TemplateStoreAccess.retrieveInstanceByTemplateId(domainAccess, templateId);
    }

    static List<TemplateMetaData> fetchAll(I_DomainAccess domainAccess) {
        return TemplateStoreAccess.fetchAll(domainAccess);
    }
}
