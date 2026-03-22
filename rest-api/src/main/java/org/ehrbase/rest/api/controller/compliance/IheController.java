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
import org.ehrbase.rest.api.controller.BaseApiController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v2 controller for IHE profile integration stubs.
 * ATNA (Audit Trail and Node Authentication) and PIX/PDQ (Patient Identity).
 * Full integration deferred — stubs return 501 with descriptive messages.
 */
@RestController
@RequestMapping("/api/v2/ihe")
@Tag(name = "IHE Profiles", description = "ATNA audit export and PIX/PDQ patient identity stubs")
@ConditionalOnProperty(name = "ehrbase.features.ihe", havingValue = "true")
public class IheController extends BaseApiController {

    @GetMapping("/atna/export")
    @Operation(
            summary = "Export audit trail in ATNA format",
            description = "Maps ehr_system.audit_event to DICOM/IHE ATNA audit message format")
    public ResponseEntity<Void> exportAtna() {
        throw new UnsupportedOperationException("IHE ATNA audit format export deferred to future release");
    }

    @GetMapping("/atna/syslog")
    @Operation(summary = "Syslog export for ATNA audit repository")
    public ResponseEntity<Void> syslogExport() {
        throw new UnsupportedOperationException("IHE ATNA syslog export deferred to future release");
    }

    @GetMapping("/pix/{patient_id}")
    @Operation(
            summary = "Cross-reference patient IDs (PIX)",
            description = "Integration with external Master Patient Index")
    public ResponseEntity<Void> pixLookup(@PathVariable("patient_id") String patientId) {
        throw new UnsupportedOperationException(
                "IHE PIX patient identity cross-referencing deferred to future release");
    }

    @GetMapping("/pdq")
    @Operation(summary = "Patient demographics query (PDQ)")
    public ResponseEntity<Void> pdqSearch(@RequestParam(value = "name", required = false) String name) {
        throw new UnsupportedOperationException("IHE PDQ patient demographics query deferred to future release");
    }
}
