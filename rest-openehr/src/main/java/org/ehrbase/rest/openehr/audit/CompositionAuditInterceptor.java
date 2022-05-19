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

import java.net.URI;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.response.ehrscape.CompositionDto;
import org.ehrbase.rest.openehr.audit.support.CompositionAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Concrete implementation of {@link OpenEhrAuditInterceptor} for Composition API.
 */
// TODO: This class will have to be updated when the OpenehrCompositionController will return the correct location
// header
public class CompositionAuditInterceptor extends OpenEhrAuditInterceptor<CompositionAuditDataset> {

    public static final String COMPOSITION_ID_ATTRIBUTE =
            CompositionAuditInterceptor.class.getName() + ".COMPOSITION_ID";

    public static final String VERSION_ATTRIBUTE = CompositionAuditInterceptor.class.getName() + ".VERSION";

    private final CompositionService compositionService;

    public CompositionAuditInterceptor(
            AuditContext auditContext, EhrService ehrService, CompositionService compositionService) {
        super(auditContext, ehrService);
        this.compositionService = compositionService;
    }

    @Override
    protected CompositionAuditDataset createAuditDataset() {
        return new CompositionAuditDataset();
    }

    @Override
    protected void enrichDataset(
            CompositionAuditDataset auditDataset, HttpServletRequest request, HttpServletResponse response) {
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
        if (auditDataset.hasPatientParticipantObjectIds()) {
            builder.addPatientParticipantObjectIdentification(auditDataset);
        }
        return builder.getMessages();
    }

    private String getCompositionUri(HttpServletRequest request) {
        UUID compositionId = (UUID) request.getAttribute(COMPOSITION_ID_ATTRIBUTE);
        if (compositionId != null) {
            UUID ehrId = getUniqueEhrId(request);
            Integer version = (Integer) request.getAttribute(VERSION_ATTRIBUTE);
            if (version == null || version == 0) {
                version = compositionService.getLastVersionNumber(compositionId);
            }
            URI uri = UriComponentsBuilder.fromPath("ehr/{ehrId}/composition/{compositionId}::{nodeName}::{version}")
                    .build(
                            ehrId,
                            compositionId,
                            compositionService.getServerConfig().getNodename(),
                            version);
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
        UUID ehrId = compositionService.getEhrId(compositionId);
        return compositionService
                .retrieve(ehrId, compositionId, version)
                .map(c -> CompositionService.from(ehrId, c))
                .map(CompositionDto::getTemplateId)
                .orElse(null);
    }
}
