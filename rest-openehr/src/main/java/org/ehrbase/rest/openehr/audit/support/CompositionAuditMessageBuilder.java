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
package org.ehrbase.rest.openehr.audit.support;

import org.ehrbase.rest.openehr.audit.CompositionAuditDataset;
import org.ehrbase.rest.openehr.audit.CompositionEventTypeCode;
import org.ehrbase.rest.openehr.audit.OpenEhrEventIdCode;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectDataLifeCycle;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectIdTypeCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectTypeCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectTypeCodeRole;

@SuppressWarnings("UnusedReturnValue")
public class CompositionAuditMessageBuilder extends OpenEhrAuditMessageBuilder {

    public CompositionAuditMessageBuilder(AuditContext auditContext, CompositionAuditDataset auditDataset) {
        super(auditContext, auditDataset, OpenEhrEventIdCode.COMPOSITION, CompositionEventTypeCode.resolve(auditDataset.getMethod()));
    }

    public CompositionAuditMessageBuilder addComposition(CompositionAuditDataset auditDataset) {
        delegate.addParticipantObjectIdentification(
                ParticipantObjectIdTypeCode.URI,
                auditDataset.getTemplateId(),
                null,
                null,
                auditDataset.getCompositionUri(),
                ParticipantObjectTypeCode.System,
                null,
                resolveCompositionObjectDataLifeCycle(auditDataset),
                null);
        return this;
    }

    public CompositionAuditMessageBuilder addPatient(CompositionAuditDataset auditDataset) {
        delegate.addParticipantObjectIdentification(
                ParticipantObjectIdTypeCode.PatientNumber,
                null,
                null,
                null,
                auditDataset.getPatientNumber(),
                ParticipantObjectTypeCode.Person,
                ParticipantObjectTypeCodeRole.Patient,
                null,
                null);
        return this;
    }

    private ParticipantObjectDataLifeCycle resolveCompositionObjectDataLifeCycle(CompositionAuditDataset auditDataset) {
        switch (auditDataset.getMethod()) {
            case POST:
                return ParticipantObjectDataLifeCycle.Origination;
            case PUT:
                return ParticipantObjectDataLifeCycle.Amendment;
            case GET:
                return ParticipantObjectDataLifeCycle.Disclosure;
            default:
                return null;
        }
    }
}
