/*
 * Copyright (c) 2021 Vitasystems GmbH.
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
package org.ehrbase.rest.openehr.audit;

import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.openehr.audit.support.EhrAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.model.AuditMessage;

/**
 * Concrete implementation of {@link OpenEhrAuditInterceptor} for EHR API.
 */
public class EhrAuditInterceptor extends OpenEhrAuditInterceptor<OpenEhrAuditDataset> {

    public EhrAuditInterceptor(AuditContext auditContext, EhrService ehrService) {
        super(auditContext, ehrService);
    }

    @Override
    protected OpenEhrAuditDataset createAuditDataset() {
        return new OpenEhrAuditDataset();
    }

    @Override
    protected AuditMessage[] getAuditMessages(OpenEhrAuditDataset auditDataset) {
        EhrAuditMessageBuilder builder = new EhrAuditMessageBuilder(auditContext, auditDataset);
        if (auditDataset.hasPatientParticipantObjectId()) {
            builder.addPatientParticipantObjectIdentification(auditDataset);
        }
        return builder.getMessages();
    }
}
