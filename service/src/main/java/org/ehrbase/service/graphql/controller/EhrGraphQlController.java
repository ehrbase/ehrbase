/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service.graphql.controller;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.repository.EhrRepository;
import org.ehrbase.service.graphql.subscription.AuditEventSink;
import org.ehrbase.service.graphql.subscription.CompositionChangeSink;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.ArgumentValue;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

/**
 * Annotated GraphQL controller for EHRbase mutations and subscriptions.
 * Uses Spring for GraphQL 2.0.2 annotations instead of raw DataFetcher implementations.
 *
 * <p>Features used:
 * <ul>
 *   <li>{@code @MutationMapping} / {@code @SubscriptionMapping} — type-safe controller methods</li>
 *   <li>{@code ArgumentValue<T>} — distinguishes null from omitted for partial updates</li>
 *   <li>{@code @Argument} — automatic argument binding</li>
 * </ul>
 */
@NullMarked
@Controller
public class EhrGraphQlController {

    private final EhrService ehrService;
    private final EhrRepository ehrRepository;
    private final CompositionService compositionService;
    private final CompositionChangeSink compositionChangeSink;
    private final AuditEventSink auditEventSink;

    public EhrGraphQlController(
            EhrService ehrService,
            EhrRepository ehrRepository,
            CompositionService compositionService,
            CompositionChangeSink compositionChangeSink,
            AuditEventSink auditEventSink) {
        this.ehrService = ehrService;
        this.ehrRepository = ehrRepository;
        this.compositionService = compositionService;
        this.compositionChangeSink = compositionChangeSink;
        this.auditEventSink = auditEventSink;
    }

    // ==================== MUTATIONS ====================

    @MutationMapping
    public Map<String, Object> createEhr(
            @Argument @Nullable String subjectId, @Argument @Nullable String subjectNamespace) {

        EhrStatus status = new EhrStatus();
        status.setArchetypeNodeId("openEHR-EHR-EHR_STATUS.generic.v1");
        status.setName(new DvText("EHR Status"));
        status.setQueryable(true);
        status.setModifiable(true);

        PartySelf subject = new PartySelf();
        if (subjectId != null) {
            subject.setExternalRef(new PartyRef(new HierObjectId(subjectId), subjectNamespace, "PERSON"));
        }
        status.setSubject(subject);

        UUID ehrId = ehrService.create(null, status);

        return Map.of(
                "id",
                ehrId.toString(),
                "subjectId",
                subjectId != null ? subjectId : "",
                "subjectNamespace",
                subjectNamespace != null ? subjectNamespace : "",
                "creationDate",
                ehrRepository
                        .getCreationTime(ehrId)
                        .map(OffsetDateTime::toString)
                        .orElse(""),
                "isModifiable",
                true,
                "isQueryable",
                true);
    }

    @SuppressWarnings("deprecation")
    @MutationMapping
    public Map<String, Object> createComposition(
            @Argument String ehrId, @Argument String templateId, @Argument Object data) {

        UUID ehr = UUID.fromString(ehrId);
        String jsonContent = data instanceof String s ? s : data.toString();
        Composition composition = new CanonicalJson().unmarshal(jsonContent, Composition.class);

        UUID compositionId = compositionService.create(ehr, composition).orElseThrow();

        return Map.of(
                "compositionId",
                compositionId.toString(),
                "ehrId",
                ehrId,
                "templateId",
                templateId,
                "version",
                1,
                "committedAt",
                OffsetDateTime.now().toString());
    }

    /**
     * Updates a composition. Uses {@link ArgumentValue} for the {@code data} argument
     * to distinguish between null (explicitly set to null) and omitted (not provided).
     * This enables partial updates — only provided fields are changed.
     */
    @SuppressWarnings("deprecation")
    @MutationMapping
    public Map<String, Object> updateComposition(
            @Argument String compositionId, @Argument int version, @Argument ArgumentValue<Object> data) {

        UUID compId = UUID.fromString(compositionId);

        if (!data.isPresent()) {
            throw new IllegalArgumentException("data argument is required for updateComposition");
        }

        UUID ehrId = compositionService
                .getEhrIdForComposition(compId)
                .orElseThrow(() -> new IllegalArgumentException("Composition not found: " + compositionId));

        String systemId = "local.ehrbase.org";
        ObjectVersionId targetObjId = new ObjectVersionId(compId + "::" + systemId + "::" + version);

        Object dataValue = data.value();
        String jsonContent = dataValue instanceof String s ? s : dataValue.toString();
        Composition composition = new CanonicalJson().unmarshal(jsonContent, Composition.class);

        UUID resultId =
                compositionService.update(ehrId, targetObjId, composition).orElseThrow();

        String templateId = "";
        if (composition.getArchetypeDetails() != null
                && composition.getArchetypeDetails().getTemplateId() != null) {
            templateId = composition.getArchetypeDetails().getTemplateId().getValue();
        }

        return Map.of(
                "compositionId", resultId.toString(),
                "ehrId", ehrId.toString(),
                "templateId", templateId,
                "version", version + 1,
                "committedAt", OffsetDateTime.now().toString());
    }

    @MutationMapping
    public boolean deleteComposition(@Argument String compositionId, @Argument int version) {
        UUID compId = UUID.fromString(compositionId);

        UUID ehrId = compositionService
                .getEhrIdForComposition(compId)
                .orElseThrow(() -> new IllegalArgumentException("Composition not found: " + compositionId));

        String systemId = "local.ehrbase.org";
        ObjectVersionId targetObjId = new ObjectVersionId(compId + "::" + systemId + "::" + version);

        compositionService.delete(ehrId, targetObjId);
        return true;
    }

    // ==================== SUBSCRIPTIONS ====================

    @SubscriptionMapping
    public Flux<Map<String, Object>> onCompositionChange(@Argument @Nullable String ehrId) {
        if (ehrId != null) {
            UUID ehr = UUID.fromString(ehrId);
            return compositionChangeSink.streamForEhr(ehr).map(CompositionChangeSink.CompositionEvent::toMap);
        }
        return compositionChangeSink.stream().map(CompositionChangeSink.CompositionEvent::toMap);
    }

    @SubscriptionMapping
    public Flux<Map<String, Object>> onAuditEvent(@Argument @Nullable Integer tenantId) {
        return auditEventSink.stream().map(AuditEventSink.AuditEventNotification::toMap);
    }
}
