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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.service.AuditEventService;
import org.ehrbase.service.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class EmergencyAccessControllerTest {

    private static final UUID EHR_ID = UUID.fromString("7d44b88c-4199-4bad-97dc-d78268e01398");

    private final EhrService mockEhrService = mock();
    private final AuditEventService mockAuditService = mock();
    private final RequestContext mockRequestContext = mock();

    private final EmergencyAccessController controller =
            new EmergencyAccessController(mockEhrService, mockAuditService, mockRequestContext);

    @BeforeEach
    void setUp() {
        Mockito.reset(mockEhrService, mockAuditService, mockRequestContext);
        when(mockRequestContext.getUserId()).thenReturn("dr.smith");
    }

    @Test
    void requestEmergencyAccessWithJustification() {
        var body = Map.of("justification", "Patient in critical condition");
        var response = controller.requestEmergencyAccess(EHR_ID.toString(), body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("granted", true);
        assertThat(response.getBody()).containsEntry("ehr_id", EHR_ID.toString());
        assertThat(response.getBody()).containsKey("expires_at");
        verify(mockAuditService)
                .recordEvent(
                        org.mockito.ArgumentMatchers.eq("emergency_access"),
                        org.mockito.ArgumentMatchers.eq("ehr"),
                        org.mockito.ArgumentMatchers.eq(EHR_ID),
                        org.mockito.ArgumentMatchers.eq("emergency_override"),
                        org.mockito.ArgumentMatchers.eq("Patient in critical condition"),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void requestEmergencyAccessWithoutJustificationRejected() {
        assertThatThrownBy(() -> controller.requestEmergencyAccess(EHR_ID.toString(), Map.of()))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessageContaining("justification");
    }

    @Test
    void requestEmergencyAccessBlankJustificationRejected() {
        assertThatThrownBy(() -> controller.requestEmergencyAccess(EHR_ID.toString(), Map.of("justification", "   ")))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessageContaining("justification");
    }

    @Test
    void requestEmergencyAccessInvalidEhrId() {
        assertThatThrownBy(() -> controller.requestEmergencyAccess("invalid", Map.of("justification", "test")))
                .isInstanceOf(InvalidApiParameterException.class);
    }
}
