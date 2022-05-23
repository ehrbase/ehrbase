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

import java.nio.charset.StandardCharsets;
import org.ehrbase.rest.openehr.audit.OpenEhrEventIdCode;
import org.ehrbase.rest.openehr.audit.QueryAuditDataset;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventActionCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectIdTypeCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectTypeCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectTypeCodeRole;

/**
 * Concrete implementation of {@link OpenEhrAuditMessageBuilder} for Query AuditMessages.
 */
@SuppressWarnings("UnusedReturnValue")
public class QueryAuditMessageBuilder extends OpenEhrAuditMessageBuilder<QueryAuditMessageBuilder> {

    public QueryAuditMessageBuilder(AuditContext auditContext, QueryAuditDataset auditDataset, String patientNumber) {
        super(auditContext, auditDataset, EventActionCode.Execute, OpenEhrEventIdCode.QUERY, null);

        addQueryParticipantObjectIdentification(auditDataset);
        addPatientParticipantObjectIdentification(patientNumber);
    }

    public QueryAuditMessageBuilder addQueryParticipantObjectIdentification(QueryAuditDataset auditDataset) {
        delegate.addParticipantObjectIdentification(
                ParticipantObjectIdTypeCode.SearchCriteria,
                null,
                auditDataset.getQuery() != null ? auditDataset.getQuery().getBytes(StandardCharsets.UTF_8) : null,
                null,
                auditDataset.getQueryId() != null ? auditDataset.getQueryId() : auditContext.getAuditValueIfMissing(),
                ParticipantObjectTypeCode.System,
                ParticipantObjectTypeCodeRole.Query,
                null,
                null);
        return this;
    }

    public QueryAuditMessageBuilder addPatientParticipantObjectIdentification(String patientNumber) {
        delegate.addPatientParticipantObject(patientNumber, null, null, null);
        return this;
    }
}
