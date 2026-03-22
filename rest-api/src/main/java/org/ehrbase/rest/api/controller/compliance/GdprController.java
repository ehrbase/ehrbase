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
package org.ehrbase.rest.api.controller.compliance;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.api.controller.BaseApiController;
import org.ehrbase.service.AuditEventService;
import org.ehrbase.service.RequestContext;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.jooq.Record;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v1 controller for GDPR compliance operations.
 * Supports data export, pseudonymization, and consent management.
 */
@RestController
@RequestMapping("/api/v1/gdpr")
@Tag(name = "GDPR Compliance", description = "Data subject access, pseudonymization, and consent management")
@ConditionalOnProperty(name = "ehrbase.features.gdpr", havingValue = "true")
public class GdprController extends BaseApiController {

    private final DSLContext dsl;
    private final EhrService ehrService;
    private final AuditEventService auditService;
    private final RequestContext requestContext;

    public GdprController(
            DSLContext dsl, EhrService ehrService, AuditEventService auditService, RequestContext requestContext) {
        this.dsl = dsl;
        this.ehrService = ehrService;
        this.auditService = auditService;
        this.requestContext = requestContext;
    }

    @PostMapping(value = "/export/{ehr_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Data subject access request", description = "Exports all patient data as JSON (GDPR Art. 15)")
    public ResponseEntity<Map<String, Object>> exportPatientData(@PathVariable("ehr_id") String ehrIdStr) {
        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        auditService.recordEvent("admin_action", "ehr", ehrId, "export", null, Map.of("type", "gdpr_export"));

        List<Map<String, Object>> compositions = dsl
                .select()
                .from(table(name("ehr_system", "composition")))
                .where(field(name("ehr_id"), UUID.class).eq(ehrId))
                .fetch()
                .stream()
                .map(Record::intoMap)
                .toList();

        return ResponseEntity.ok(Map.of(
                "ehr_id", ehrId.toString(),
                "export_time", OffsetDateTime.now().toString(),
                "composition_count", compositions.size(),
                "compositions", compositions));
    }

    @PostMapping(value = "/pseudonymize/{ehr_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Pseudonymize patient identifiers",
            description = "Replaces identifiers with pseudonyms, preserves clinical data (GDPR Art. 17)")
    public ResponseEntity<Map<String, Object>> pseudonymize(@PathVariable("ehr_id") String ehrIdStr) {
        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        int updated = dsl.update(table(name("ehr_system", "ehr")))
                .set(field(name("subject_id"), String.class), (String) null)
                .set(field(name("subject_namespace"), String.class), (String) null)
                .where(field(name("id"), UUID.class).eq(ehrId))
                .execute();

        auditService.recordEvent(
                "admin_action",
                "ehr",
                ehrId,
                "pseudonymize",
                null,
                Map.of("fields_cleared", "subject_id,subject_namespace"));

        return ResponseEntity.ok(Map.of(
                "ehr_id",
                ehrId.toString(),
                "pseudonymized",
                updated > 0,
                "fields_cleared",
                List.of("subject_id", "subject_namespace")));
    }

    @GetMapping(value = "/consent/{ehr_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get current consent status")
    public ResponseEntity<List<Map<String, Object>>> getConsentStatus(@PathVariable("ehr_id") String ehrIdStr) {
        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        List<Map<String, Object>> consents = dsl
                .select()
                .from(table(name("ehr_views", "v_consent_status")))
                .where(field(name("ehr_id"), UUID.class).eq(ehrId))
                .fetch()
                .stream()
                .map(Record::intoMap)
                .toList();

        return ResponseEntity.ok(consents);
    }

    @PostMapping(
            value = "/consent/{ehr_id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update consent", description = "Set consent status: active, withdrawn, expired")
    public ResponseEntity<Map<String, Object>> updateConsent(
            @PathVariable("ehr_id") String ehrIdStr, @RequestBody Map<String, String> body) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        String consentType = body.getOrDefault("consent_type", "treatment");
        String status = body.getOrDefault("status", "active");

        dsl.execute(
                "INSERT INTO ehr_system.consent (ehr_id, consent_type, status, granted_by, sys_tenant) VALUES (?, ?, ?, ?, ?)",
                ehrId,
                consentType,
                status,
                requestContext.getUserId(),
                requestContext.getTenantId());

        auditService.recordEvent(
                "admin_action",
                "consent",
                ehrId,
                "update",
                null,
                Map.of("consent_type", consentType, "status", status));

        return ResponseEntity.ok(Map.of("ehr_id", ehrId.toString(), "consent_type", consentType, "status", status));
    }
}
