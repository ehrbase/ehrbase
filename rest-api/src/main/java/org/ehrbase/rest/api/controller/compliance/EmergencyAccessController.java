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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.api.controller.BaseApiController;
import org.ehrbase.service.AuditEventService;
import org.ehrbase.service.RequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v1 controller for emergency/break-glass access.
 * Overrides normal access control with mandatory justification and full audit trail.
 */
@RestController
@RequestMapping("/api/v1/emergency")
@Tag(name = "Emergency Access", description = "Break-glass access with mandatory justification and audit")
public class EmergencyAccessController extends BaseApiController {

    private final EhrService ehrService;
    private final AuditEventService auditService;
    private final RequestContext requestContext;

    @Value("${ehrbase.emergency.timeout-minutes:30}")
    private int timeoutMinutes;

    public EmergencyAccessController(
            EhrService ehrService, AuditEventService auditService, RequestContext requestContext) {
        this.ehrService = ehrService;
        this.auditService = auditService;
        this.requestContext = requestContext;
    }

    @PostMapping(
            value = "/access/{ehr_id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Request emergency access",
            description =
                    "Overrides access control. Requires non-empty justification. Auto-expires after configured timeout.")
    public ResponseEntity<Map<String, Object>> requestEmergencyAccess(
            @PathVariable("ehr_id") String ehrIdStr, @RequestBody Map<String, String> body) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        String justification = body.get("justification");
        if (justification == null || justification.isBlank()) {
            throw new InvalidApiParameterException("Emergency access requires a non-empty justification");
        }

        auditService.recordEvent(
                "emergency_access",
                "ehr",
                ehrId,
                "emergency_override",
                justification,
                Map.of("timeout_minutes", timeoutMinutes, "actor", requestContext.getUserId()));

        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(timeoutMinutes);

        return ResponseEntity.ok(Map.of(
                "ehr_id",
                ehrId.toString(),
                "granted",
                true,
                "granted_to",
                requestContext.getUserId(),
                "justification",
                justification,
                "expires_at",
                expiresAt.toString(),
                "timeout_minutes",
                timeoutMinutes));
    }
}
