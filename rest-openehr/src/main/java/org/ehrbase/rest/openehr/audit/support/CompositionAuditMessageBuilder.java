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
        return switch (method) {
            case POST -> EventActionCode.Create;
            case GET -> EventActionCode.Read;
            case PUT -> EventActionCode.Update;
            case DELETE -> EventActionCode.Delete;
            default -> throw new IllegalArgumentException("Cannot resolve EventActionCode, method not supported");
        };
    }

    private static EventType resolveEventType(HttpMethod method) {
        return switch (method) {
            case POST -> OpenEhrEventTypeCode.CREATE;
            case GET -> null;
            case PUT -> OpenEhrEventTypeCode.UPDATE;
            case DELETE -> OpenEhrEventTypeCode.DELETE;
            default -> throw new IllegalArgumentException("Cannot resolve EventType, method not supported");
        };
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
        return switch (method) {
            case POST -> ParticipantObjectDataLifeCycle.Origination;
            case PUT -> ParticipantObjectDataLifeCycle.Amendment;
            case GET -> ParticipantObjectDataLifeCycle.Disclosure;
            case DELETE -> null;
            default -> throw new IllegalArgumentException(
                    "Cannot resolve ParticipantObjectDataLifeCycle, method not supported");
        };
    }
}
