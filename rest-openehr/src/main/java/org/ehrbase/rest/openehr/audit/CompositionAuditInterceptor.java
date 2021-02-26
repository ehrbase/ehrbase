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

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.response.ehrscape.CompositionDto;
import org.ehrbase.rest.openehr.audit.support.CompositionAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.UUID;

/**
 * Concrete implementation of {@link OpenEhrAuditInterceptor} for Composition API.
 */
// TODO: This class will have to be updated when the OpenehrCompositionController will return the correct location header
public class CompositionAuditInterceptor extends OpenEhrAuditInterceptor<CompositionAuditDataset> {

    public static final String COMPOSITION_ID_ATTRIBUTE = CompositionAuditInterceptor.class.getName() + ".COMPOSITION_ID";

    public static final String VERSION_ATTRIBUTE = CompositionAuditInterceptor.class.getName() + ".VERSION";

    private final CompositionService compositionService;

    public CompositionAuditInterceptor(AuditContext auditContext, EhrService ehrService, CompositionService compositionService) {
        super(auditContext, ehrService);
        this.compositionService = compositionService;
    }

    @Override
    protected CompositionAuditDataset createAuditDataset() {
        return new CompositionAuditDataset();
    }

    @Override
    protected void enrichDataset(CompositionAuditDataset auditDataset, HttpServletRequest request, HttpServletResponse response) {
        super.enrichDataset(auditDataset, request, response);

        auditDataset.setCompositionUri(getCompositionUri(request));
        auditDataset.setTemplateId(getTemplateId(request));
    }

    @Override
    protected AuditMessage[] getAuditMessages(CompositionAuditDataset auditDataset) {
        CompositionAuditMessageBuilder builder = new CompositionAuditMessageBuilder(auditContext, auditDataset);
        if (auditDataset.hasCompositionUri()) {
            builder.addCompositionParticipantObjectIdentification(auditDataset);
        }
        if (auditDataset.hasPatientParticipantObjectId()) {
            builder.addPatientParticipantObjectIdentification(auditDataset);
        }
        return builder.getMessages();
    }

    private String getCompositionUri(HttpServletRequest request) {
        UUID compositionId = (UUID) request.getAttribute(COMPOSITION_ID_ATTRIBUTE);
        if (compositionId != null) {
            UUID ehrId = (UUID) request.getAttribute(EHR_ID_ATTRIBUTE);
            Integer version = (Integer) request.getAttribute(VERSION_ATTRIBUTE);
            if (version == null || version == 0) {
                version = compositionService.getLastVersionNumber(compositionId);
            }
            URI uri = UriComponentsBuilder.fromPath("ehr/{ehrId}/composition/{compositionId}::{nodeName}::{version}")
                    .build(ehrId, compositionId, compositionService.getServerConfig().getNodename(), version);
            return uri.toString();
        } else {
            return StringUtils.remove(request.getRequestURI(), "/ehrbase/rest/openehr/v1/");
        }
    }

    private String getTemplateId(HttpServletRequest request) {
        UUID compositionId = (UUID) request.getAttribute(COMPOSITION_ID_ATTRIBUTE);
        if (compositionId == null) {
            return null;
        }

        Integer version = (Integer) request.getAttribute(VERSION_ATTRIBUTE);
        if (version == null || version == 0) {
            version = compositionService.getLastVersionNumber(compositionId);
        }
        return compositionService.retrieve(compositionId, version)
                .map(CompositionDto::getTemplateId)
                .orElse(null);

    }
}
