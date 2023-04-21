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
package org.ehrbase.rest.openehr.audit.support;

import org.ehrbase.rest.openehr.audit.EhrStatusAuditDataset;
import org.ehrbase.rest.openehr.audit.OpenEhrAuditDataset;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.*;
import org.springframework.http.HttpMethod;

/**
 * Concrete implementation of {@link OpenEhrAuditMessageBuilder} for EHR Status AuditMessages.
 */
public class EhrStatusAuditMessageBuilder extends OpenEhrAuditMessageBuilder<EhrStatusAuditMessageBuilder> {

    public EhrStatusAuditMessageBuilder(AuditContext auditContext, OpenEhrAuditDataset auditDataset) {
        super(
                auditContext,
                auditDataset,
                resolveEventActionCode(auditDataset.getMethod()),
                EventIdCode.PatientRecord,
                null);
    }

    protected static EventActionCode resolveEventActionCode(HttpMethod method) {
        return switch (method) {
            case PUT -> EventActionCode.Update;
            case GET -> EventActionCode.Read;
            default -> throw new IllegalArgumentException("Cannot resolve EventActionCode, method not supported");
        };
    }

    public void addPatientParticipantObjectIdentification(EhrStatusAuditDataset auditDataset) {
        delegate.addPatientParticipantObject(
                auditDataset.getUniquePatientParticipantObjectId(),
                null,
                null,
                resolveLifeCycle(auditDataset.getMethod()));
    }

    public void addEhrStatusParticipantObjectIdentification(EhrStatusAuditDataset auditDataset) {
        delegate.addParticipantObjectIdentification(
                ParticipantObjectIdTypeCode.URI,
                null,
                null,
                null,
                auditDataset.getEhrStatusUri(),
                ParticipantObjectTypeCode.System,
                null,
                resolveLifeCycle(auditDataset.getMethod()),
                null);
    }

    private ParticipantObjectDataLifeCycle resolveLifeCycle(HttpMethod method) {
        return switch (method) {
            case PUT -> ParticipantObjectDataLifeCycle.Amendment;
            case GET -> ParticipantObjectDataLifeCycle.Disclosure;
            default -> throw new IllegalArgumentException(
                    "Cannot resolve ParticipantObjectDataLifeCycle, method not supported");
        };
    }
}
