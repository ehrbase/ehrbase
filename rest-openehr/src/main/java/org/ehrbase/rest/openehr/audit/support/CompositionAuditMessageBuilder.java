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

import org.ehrbase.rest.openehr.audit.CompositionAuditDataset;
import org.ehrbase.rest.openehr.audit.OpenEhrEventIdCode;
import org.ehrbase.rest.openehr.audit.OpenEhrEventTypeCode;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventActionCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectDataLifeCycle;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectIdTypeCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectTypeCode;
import org.openehealth.ipf.commons.audit.types.EventType;
import org.springframework.http.HttpMethod;

/**
 * Concrete implementation of {@link OpenEhrAuditMessageBuilder} for Composition AuditMessages.
 */
@SuppressWarnings("UnusedReturnValue")
public class CompositionAuditMessageBuilder extends OpenEhrAuditMessageBuilder<CompositionAuditMessageBuilder> {

    public CompositionAuditMessageBuilder(AuditContext auditContext, CompositionAuditDataset auditDataset) {
        super(
                auditContext,
                auditDataset,
                resolveEventActionCode(auditDataset.getMethod()),
                OpenEhrEventIdCode.COMPOSITION,
                resolveEventType(auditDataset.getMethod()));
    }

    private static EventActionCode resolveEventActionCode(HttpMethod method) {
        switch (method) {
            case POST:
                return EventActionCode.Create;
            case GET:
                return EventActionCode.Read;
            case PUT:
                return EventActionCode.Update;
            case DELETE:
                return EventActionCode.Delete;
            default:
                throw new IllegalArgumentException("Cannot resolve EventActionCode, method not supported");
        }
    }

    private static EventType resolveEventType(HttpMethod method) {
        switch (method) {
            case POST:
                return OpenEhrEventTypeCode.CREATE;
            case GET:
                return null;
            case PUT:
                return OpenEhrEventTypeCode.UPDATE;
            case DELETE:
                return OpenEhrEventTypeCode.DELETE;
            default:
                throw new IllegalArgumentException("Cannot resolve EventType, method not supported");
        }
    }

    public CompositionAuditMessageBuilder addCompositionParticipantObjectIdentification(
            CompositionAuditDataset auditDataset) {
        delegate.addParticipantObjectIdentification(
                ParticipantObjectIdTypeCode.URI,
                auditDataset.getTemplateId(),
                null,
                null,
                auditDataset.getCompositionUri(),
                ParticipantObjectTypeCode.System,
                null,
                resolveLifeCycle(auditDataset.getMethod()),
                null);
        return this;
    }

    public CompositionAuditMessageBuilder addPatientParticipantObjectIdentification(
            CompositionAuditDataset auditDataset) {
        delegate.addPatientParticipantObject(auditDataset.getUniquePatientParticipantObjectId(), null, null, null);
        return this;
    }

    private ParticipantObjectDataLifeCycle resolveLifeCycle(HttpMethod method) {
        switch (method) {
            case POST:
                return ParticipantObjectDataLifeCycle.Origination;
            case PUT:
                return ParticipantObjectDataLifeCycle.Amendment;
            case GET:
                return ParticipantObjectDataLifeCycle.Disclosure;
            case DELETE:
                return null;
            default:
                throw new IllegalArgumentException(
                        "Cannot resolve ParticipantObjectDataLifeCycle, method not supported");
        }
    }
}
