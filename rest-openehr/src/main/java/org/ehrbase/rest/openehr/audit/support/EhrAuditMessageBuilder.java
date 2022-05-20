/*
 * Copyright (c) 2021 vitasystems GmbH and Hannover Medical School.
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

import org.ehrbase.rest.openehr.audit.OpenEhrAuditDataset;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventActionCode;
import org.openehealth.ipf.commons.audit.codes.EventIdCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectDataLifeCycle;
import org.springframework.http.HttpMethod;

/**
 * Concrete implementation of {@link OpenEhrAuditMessageBuilder} for EHR AuditMessages.
 */
public class EhrAuditMessageBuilder extends OpenEhrAuditMessageBuilder<EhrAuditMessageBuilder> {

    public EhrAuditMessageBuilder(AuditContext auditContext, OpenEhrAuditDataset auditDataset) {
        super(
                auditContext,
                auditDataset,
                resolveEventActionCode(auditDataset.getMethod()),
                EventIdCode.PatientRecord,
                null);
    }

    protected static EventActionCode resolveEventActionCode(HttpMethod method) {
        switch (method) {
            case POST:
            case PUT:
                return EventActionCode.Create;
            case GET:
                return EventActionCode.Read;
            default:
                throw new IllegalArgumentException("Cannot resolve EventActionCode, method not supported");
        }
    }

    public EhrAuditMessageBuilder addPatientParticipantObjectIdentification(OpenEhrAuditDataset auditDataset) {
        delegate.addPatientParticipantObject(
                auditDataset.getUniquePatientParticipantObjectId(),
                null,
                null,
                resolveLifeCycle(auditDataset.getMethod()));
        return this;
    }

    private ParticipantObjectDataLifeCycle resolveLifeCycle(HttpMethod method) {
        switch (method) {
            case POST:
            case PUT:
                return ParticipantObjectDataLifeCycle.Origination;
            case GET:
                return ParticipantObjectDataLifeCycle.Disclosure;
            default:
                throw new IllegalArgumentException(
                        "Cannot resolve ParticipantObjectDataLifeCycle, method not supported");
        }
    }
}
