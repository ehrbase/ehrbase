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
package org.ehrbase.rest.openehr.audit;

import static org.apache.commons.lang3.StringUtils.isBlank;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.audit.support.EhrStatusAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.springframework.beans.factory.annotation.Value;

/**
 * Concrete implementation of {@link OpenEhrAuditInterceptor} for EHR Status API.
 */
public class EhrStatusAuditInterceptor extends OpenEhrAuditInterceptor<EhrStatusAuditDataset> {

    public static final String VERSION_ATTRIBUTE = EhrStatusAuditInterceptor.class.getName() + ".VERSION";

    @Value(BaseController.API_CONTEXT_PATH_WITH_VERSION)
    protected String apiContextPathWithVersion;

    public EhrStatusAuditInterceptor(AuditContext auditContext, EhrService ehrService, TenantService tenantService) {
        super(auditContext, ehrService, tenantService);
    }

    @Override
    protected EhrStatusAuditDataset createAuditDataset() {
        return new EhrStatusAuditDataset();
    }

    @Override
    protected void enrichDataset(
            EhrStatusAuditDataset auditDataset, HttpServletRequest request, HttpServletResponse response) {
        super.enrichDataset(auditDataset, request, response);

        String location = response.getHeader("Location");
        auditDataset.setEhrStatusUri(
                !isBlank(location)
                        ? location
                        : StringUtils.remove(request.getRequestURI(), "/ehrbase/rest/openehr/v1/"));
    }

    @Override
    protected AuditMessage[] getAuditMessages(EhrStatusAuditDataset auditDataset) {
        EhrStatusAuditMessageBuilder builder = new EhrStatusAuditMessageBuilder(auditContext, auditDataset);

        if (auditDataset.hasEhrStatusUri()) {
            builder.addEhrStatusParticipantObjectIdentification(auditDataset);
        }

        if (auditDataset.hasPatientParticipantObjectIds()) {
            builder.addPatientParticipantObjectIdentification(auditDataset);
        }

        return builder.getMessages();
    }
}
