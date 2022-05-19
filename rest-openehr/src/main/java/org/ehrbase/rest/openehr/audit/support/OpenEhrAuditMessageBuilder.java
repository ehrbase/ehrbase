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
import org.openehealth.ipf.commons.audit.event.CustomAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.event.DelegatingAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.types.EventId;
import org.openehealth.ipf.commons.audit.types.EventType;
import org.openehealth.ipf.commons.audit.utils.AuditUtils;

/**
 * Abstract {@link org.openehealth.ipf.commons.audit.event.AuditMessageBuilder AuditMessageBuilder}
 * for building DICOM audit messages as specified in openEHR Audit Event Message Specifications.
 */
@SuppressWarnings("UnusedReturnValue")
public abstract class OpenEhrAuditMessageBuilder<T extends OpenEhrAuditMessageBuilder<T>>
        extends DelegatingAuditMessageBuilder<T, CustomAuditMessageBuilder> {

    protected final AuditContext auditContext;

    protected OpenEhrAuditMessageBuilder(
            AuditContext auditContext,
            OpenEhrAuditDataset auditDataset,
            EventActionCode eventActionCode,
            EventId eventId,
            EventType eventType) {
        super(new CustomAuditMessageBuilder(
                auditDataset.getEventOutcomeIndicator(),
                auditDataset.getEventOutcomeDescription(),
                eventActionCode,
                eventId,
                eventType));
        this.auditContext = auditContext;

        addSourceActiveParticipant(auditDataset);
        addDestinationActiveParticipant();

        delegate.setAuditSource(auditContext);
    }

    protected final T addSourceActiveParticipant(OpenEhrAuditDataset auditDataset) {
        delegate.addSourceActiveParticipant(
                auditDataset.getSourceParticipantUserId() != null
                        ? auditDataset.getSourceParticipantUserId()
                        : auditContext.getAuditValueIfMissing(),
                null,
                null,
                auditDataset.getSourceParticipantNetworkId() != null
                        ? auditDataset.getSourceParticipantNetworkId()
                        : auditContext.getAuditValueIfMissing(),
                true);
        return self();
    }

    protected final T addDestinationActiveParticipant() {
        delegate.addDestinationActiveParticipant(
                auditContext.getAuditSourceId(), null, null, AuditUtils.getLocalIPAddress(), false);
        return self();
    }
}
