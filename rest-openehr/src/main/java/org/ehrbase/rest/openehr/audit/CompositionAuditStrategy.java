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

import org.ehrbase.rest.openehr.audit.support.CompositionAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventActionCode;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class CompositionAuditStrategy extends OpenEhrAuditStrategy<CompositionAuditDataset> {

    public static final String COMPOSITION_TEMPLATE_ID = "CompositionAuditStrategy.CompositionTemplateId";

    public CompositionAuditStrategy(AuditContext auditContext) {
        super(auditContext);
    }

    @Override
    protected CompositionAuditDataset createAuditDataset() {
        return new CompositionAuditDataset();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void enrichAuditDataset(CompositionAuditDataset auditDataset, HttpServletRequest request, HttpServletResponse response) {
        super.enrichAuditDataset(auditDataset, request, response);

        Map<String, Object> pathVariables = (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES);
        auditDataset.setEhrId((String) pathVariables.get("ehr_id"));

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        switch (method) {
            case POST:
                auditDataset.setEventActionCode(EventActionCode.Create);
                break;
            case GET:
                auditDataset.setEventActionCode(EventActionCode.Read);
                break;
            case PUT:
                auditDataset.setEventActionCode(EventActionCode.Update);
                break;
            case DELETE:
                auditDataset.setEventActionCode(EventActionCode.Delete);
                break;
            default:
                auditDataset.setEventActionCode(EventActionCode.Execute);
                break;
        }

        String compositionUri = response.getHeader(HttpHeaders.LOCATION);
        if (compositionUri != null) {
            auditDataset.setCompositionUri(compositionUri);
        }

        String compositionTemplateId = (String) request.getAttribute(COMPOSITION_TEMPLATE_ID);
        if (compositionTemplateId != null) {
            auditDataset.setCompositionTemplateId(compositionTemplateId);
        }
    }

    @Override
    protected AuditMessage[] getMessages(CompositionAuditDataset auditDataset) {
        CompositionAuditMessageBuilder builder = new CompositionAuditMessageBuilder(auditContext, auditDataset);
        if (auditDataset.hasComposition()) {
            builder.addComposition(auditDataset);
        }
        if (auditDataset.hasEhrId()) {
            builder.addPatient(auditDataset);
        }
        return builder.getMessages();
    }
}
