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

import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.service.EhrService;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Base {@link HandlerInterceptor} that provides the common logic for handling audit feature.
 */
public abstract class OpenEhrAuditInterceptor<T extends OpenEhrAuditDataset> implements HandlerInterceptor {

    public static final String EHR_ID_ATTRIBUTE = OpenEhrAuditInterceptor.class.getName() + ".EHR_ID";

    public static final String START_TIME_ATTRIBUTE = OpenEhrAuditInterceptor.class.getName() + ".START_TIME";

    protected final AuditContext auditContext;

    protected final EhrService ehrService;

    protected OpenEhrAuditInterceptor(AuditContext auditContext, EhrService ehrService) {
        this.auditContext = auditContext;
        this.ehrService = ehrService;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
            throws Exception {
        request.setAttribute(START_TIME_ATTRIBUTE, Instant.now());
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        T auditDataset = createAuditDataset();
        enrichDataset(auditDataset, request, response);
        AuditMessage[] messages = getAuditMessages(auditDataset);
        auditContext.audit(messages);
    }

    protected abstract T createAuditDataset();

    protected void enrichDataset(T auditDataset, HttpServletRequest request, HttpServletResponse response) {
        auditDataset.setMethod(HttpMethod.valueOf(request.getMethod()));

        // SourceParticipant
        auditDataset.setSourceParticipantUserId(getCurrentAuthenticatedUsername(request));
        auditDataset.setSourceParticipantNetworkId(getClientIpAddress(request));

        // EventOutcomeIndicator and EventOutcomeDescription
        HttpStatus status = HttpStatus.valueOf(response.getStatus());
        if (!status.isError()) {
            auditDataset.setEventOutcomeIndicator(EventOutcomeIndicator.Success);
            auditDataset.setEventOutcomeDescription("Operation performed successfully");
        } else {
            if (status.is4xxClientError()) {
                auditDataset.setEventOutcomeIndicator(EventOutcomeIndicator.SeriousFailure);
            } else {
                auditDataset.setEventOutcomeIndicator(EventOutcomeIndicator.MajorFailure);
            }
            String eventOutcomeDescription = "Operation failed";
            Throwable ex = (Throwable) request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);
            if (ex != null) {
                eventOutcomeDescription += "; " + ex.getMessage();
            }
            auditDataset.setEventOutcomeDescription(eventOutcomeDescription);
        }

        // Patient ParticipantObjectIdentification
        auditDataset.addPatientParticipantObjectIds(getPatientNumbers(request));
    }

    protected abstract AuditMessage[] getAuditMessages(T auditDataset);

    protected String getCurrentAuthenticatedUsername(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return null;
        }
        return principal.getName();
    }

    protected String getClientIpAddress(HttpServletRequest request) {
        String address = request.getHeader("X-Forwarded-For");
        if (StringUtils.isEmpty(address)) {
            address = request.getRemoteAddr();
        }
        return address;
    }

    protected UUID getUniqueEhrId(HttpServletRequest request) {
        Set<UUID> ehrIds = getEhrIds(request);
        if (ehrIds.isEmpty()) {
            return null;
        } else if (ehrIds.size() == 1) {
            return ehrIds.iterator().next();
        } else {
            throw new InternalServerException("Non unique EhrId result");
        }
    }

    @SuppressWarnings("unchecked")
    protected Set<UUID> getEhrIds(HttpServletRequest request) {
        Set<UUID> ehrIds = (Set<UUID>) request.getAttribute(EHR_ID_ATTRIBUTE);
        if (ehrIds == null) {
            return Collections.emptySet();
        }
        return ehrIds;
    }

    protected Set<String> getPatientNumbers(HttpServletRequest request) {
        return getEhrIds(request).stream()
                .map(ehrId -> ehrService.getSubjectExtRef(ehrId.toString()))
                .collect(Collectors.toSet());
    }
}
