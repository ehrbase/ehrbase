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

package org.ehrbase.rest.openehr.audit;

import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.openehr.audit.support.CompositionAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CompositionAuditStrategy extends OpenEhrAuditStrategy<CompositionAuditDataset> {

    public static final String COMPOSITION_TEMPLATE_ID = "CompositionAuditStrategy.CompositionTemplateId";

    private static final String EHR_ID = "ehr_id";

    private final EhrService ehrService;

    public CompositionAuditStrategy(AuditContext auditContext, EhrService ehrService) {
        super(auditContext);
        this.ehrService = ehrService;
    }

    @Override
    protected CompositionAuditDataset createAuditDataset() {
        return new CompositionAuditDataset();
    }

    @Override
    protected void enrichAuditDataset(CompositionAuditDataset auditDataset, HttpServletRequest request, HttpServletResponse response) {
        super.enrichAuditDataset(auditDataset, request, response);

        String compositionUri = response.getHeader(HttpHeaders.LOCATION);
        if (compositionUri != null) {
            auditDataset.setCompositionUri(compositionUri);
        }

        String compositionTemplateId = (String) request.getAttribute(COMPOSITION_TEMPLATE_ID);
        if (compositionTemplateId != null) {
            auditDataset.setTemplateId(compositionTemplateId);
        }

        Optional<String> patientNumber = getPatientNumber(request);
        patientNumber.ifPresent(auditDataset::setPatientNumber);
    }

    @Override
    protected AuditMessage[] getMessages(CompositionAuditDataset auditDataset) {
        CompositionAuditMessageBuilder builder = new CompositionAuditMessageBuilder(auditContext, auditDataset);
        if (auditDataset.hasComposition()) {
            builder.addComposition(auditDataset);
        }
        if (auditDataset.hasPatientNumber()) {
            builder.addPatient(auditDataset);
        }
        return builder.getMessages();
    }

    @SuppressWarnings("unchecked")
    private Optional<String> getPatientNumber(HttpServletRequest request) {
        Map<String, Object> pathVariables = (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES);
        UUID ehrId = UUID.fromString((String) pathVariables.get(EHR_ID));

        return ehrService.getEhrStatus(ehrId)
                .map(ehrStatus -> {
                    PartySelf subject = ehrStatus.getSubject();
                    PartyRef externalRef = subject.getExternalRef();
                    ObjectId objectId = externalRef.getId();
                    return objectId.getValue();
                });
    }
}
