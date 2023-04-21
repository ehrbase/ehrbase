/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.rest.openehr.audit.admin;

import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.rest.openehr.audit.OpenEhrAuditDataset;
import org.ehrbase.rest.openehr.audit.OpenEhrAuditInterceptor;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.model.AuditMessage;

/**
 * Concrete implementation of {@link OpenEhrAuditInterceptor} for Admin EHR API.
 */
public class AdminEhrAuditInterceptor extends OpenEhrAuditInterceptor<OpenEhrAuditDataset> {

    public AdminEhrAuditInterceptor(AuditContext auditContext, EhrService ehrService, TenantService tenantService) {
        super(auditContext, ehrService, tenantService);
    }

    @Override
    protected OpenEhrAuditDataset createAuditDataset() {
        return new OpenEhrAuditDataset();
    }

    @Override
    protected AuditMessage[] getAuditMessages(OpenEhrAuditDataset auditDataset) {
        AdminEhrAuditMessageBuilder builder = new AdminEhrAuditMessageBuilder(auditContext, auditDataset);

        if (auditDataset.hasPatientParticipantObjectIds()) {
            builder.addPatientParticipantObjectIdentification(auditDataset);
        }

        return builder.getMessages();
    }
}
