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
package org.ehrbase.rest.openehr.audit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.openehr.audit.support.QueryAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.model.AuditMessage;

/**
 * Concrete implementation of {@link OpenEhrAuditInterceptor} for Query API.
 */
public class QueryAuditInterceptor extends OpenEhrAuditInterceptor<QueryAuditDataset> {

    public static final String QUERY_ATTRIBUTE = CompositionAuditInterceptor.class.getName() + ".QUERY";

    public static final String QUERY_ID_ATTRIBUTE = CompositionAuditInterceptor.class.getName() + ".QUERY_ID";

    public QueryAuditInterceptor(AuditContext auditContext, EhrService ehrService) {
        super(auditContext, ehrService);
    }

    @Override
    protected QueryAuditDataset createAuditDataset() {
        return new QueryAuditDataset();
    }

    @Override
    protected void enrichDataset(
            QueryAuditDataset auditDataset, HttpServletRequest request, HttpServletResponse response) {
        super.enrichDataset(auditDataset, request, response);

        auditDataset.setQuery((String) request.getAttribute(QUERY_ATTRIBUTE));
        auditDataset.setQueryId((String) request.getAttribute(QUERY_ID_ATTRIBUTE));
    }

    @Override
    protected AuditMessage[] getAuditMessages(QueryAuditDataset auditDataset) {
        return auditDataset.getPatientParticipantObjectIds().stream()
                .map(patientNumber ->
                        new QueryAuditMessageBuilder(auditContext, auditDataset, patientNumber).getMessage())
                .toArray(AuditMessage[]::new);
    }
}
