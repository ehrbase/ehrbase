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

import org.ehrbase.rest.openehr.audit.OpenEhrAuditDataset;
import org.ehrbase.rest.openehr.audit.support.OpenEhrAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventActionCode;
import org.openehealth.ipf.commons.audit.codes.EventIdCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectDataLifeCycle;
import org.springframework.http.HttpMethod;

public class AdminEhrAuditMessageBuilder extends OpenEhrAuditMessageBuilder<AdminEhrAuditMessageBuilder> {

    public AdminEhrAuditMessageBuilder(AuditContext auditContext, OpenEhrAuditDataset auditDataset) {
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
            case DELETE -> EventActionCode.Delete;
            default -> throw new IllegalArgumentException("Cannot resolve EventActionCode, method not supported");
        };
    }

    public AdminEhrAuditMessageBuilder addPatientParticipantObjectIdentification(OpenEhrAuditDataset auditDataset) {
        delegate.addPatientParticipantObject(
                auditDataset.getUniquePatientParticipantObjectId(),
                null,
                null,
                resolveLifeCycle(auditDataset.getMethod()));
        return this;
    }

    protected ParticipantObjectDataLifeCycle resolveLifeCycle(HttpMethod method) {
        return switch (method) {
            case PUT -> ParticipantObjectDataLifeCycle.Amendment;
            case DELETE -> ParticipantObjectDataLifeCycle.PermanentErasure;
            default -> throw new IllegalArgumentException(
                    "Cannot resolve ParticipantObjectDataLifeCycle, method not supported");
        };
    }
}
