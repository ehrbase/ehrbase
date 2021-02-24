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

import org.ehrbase.rest.openehr.audit.OpenEhrAuditDataset;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventActionCode;
import org.openehealth.ipf.commons.audit.event.CustomAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.event.DelegatingAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.types.EventId;
import org.openehealth.ipf.commons.audit.types.EventType;
import org.openehealth.ipf.commons.audit.utils.AuditUtils;
import org.springframework.http.HttpMethod;

@SuppressWarnings("UnusedReturnValue")
public abstract class OpenEhrAuditMessageBuilder extends DelegatingAuditMessageBuilder<OpenEhrAuditMessageBuilder, CustomAuditMessageBuilder> {

    private final AuditContext auditContext;

    protected OpenEhrAuditMessageBuilder(AuditContext auditContext, OpenEhrAuditDataset auditDataset,
                                         EventId eventId, EventType eventType) {
        this(auditContext, auditDataset, resolveEventActionCode(auditDataset.getMethod()), eventId, eventType);
    }

    protected OpenEhrAuditMessageBuilder(AuditContext auditContext, OpenEhrAuditDataset auditDataset,
                                         EventActionCode eventActionCode, EventId eventId, EventType eventType) {
        super(
                new CustomAuditMessageBuilder(
                        auditDataset.getEventOutcomeIndicator(),
                        auditDataset.getEventOutcomeDescription(),
                        eventActionCode,
                        eventId,
                        eventType));

        this.auditContext = auditContext;

        setAuditSource();
        setSourceActiveParticipant(auditDataset);
        setDestinationActiveParticipant();
    }

    private static EventActionCode resolveEventActionCode(HttpMethod method) {
        switch (method) {
            case POST:
                return EventActionCode.Create;
            case GET:
                return EventActionCode.Read;
            case PUT:
            case PATCH:
                return EventActionCode.Update;
            case DELETE:
                return EventActionCode.Delete;
            default:
                return EventActionCode.Execute;
        }
    }

    protected OpenEhrAuditMessageBuilder setAuditSource() {
        delegate.setAuditSource(auditContext);
        return this;
    }

    protected OpenEhrAuditMessageBuilder setSourceActiveParticipant(OpenEhrAuditDataset auditDataset) {
        delegate.addSourceActiveParticipant(auditDataset.getSourceUserId() != null ? auditDataset.getSourceUserId() : auditContext.getAuditValueIfMissing(),
                null,
                null,
                auditDataset.getSourceAddress(),
                true);
        return this;
    }

    protected OpenEhrAuditMessageBuilder setDestinationActiveParticipant() {
        delegate.addDestinationActiveParticipant(auditContext.getAuditSourceId(),
                null,
                null,
                AuditUtils.getLocalIPAddress(),
                false);
        return this;

    }
}
