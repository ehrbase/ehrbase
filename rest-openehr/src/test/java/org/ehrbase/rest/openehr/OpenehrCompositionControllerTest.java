/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.rest.openehr;

import static org.ehrbase.rest.openehr.OpenehrEhrControllerTest.assertSentOnlyDuringTheCommit;
import static org.ehrbase.rest.openehr.OpenehrEhrControllerTest.auditHeaders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.archetyped.TemplateId;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ArchetypeID;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.ehrbase.api.audit.CommitAuditHeaders;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.SystemService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;

class OpenehrCompositionControllerTest {

    private static final String EHR_ID = "1c5ea8a0-2e3d-4bd5-a8b5-9bb7e3f0c2a1";
    private static final UUID COMPOSITION_ID = UUID.fromString("0b6a3bd4-1a4d-46a5-9a1e-3c3ad57f1e0d");
    private static final String VERSION_ID = COMPOSITION_ID + "::test.ehr.controller::1";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final CompositionService mockCompositionService = mock();
    private final SystemService mockSystemService = mock();
    private final OpenehrCompositionController controller =
            spy(new OpenehrCompositionController(mockCompositionService, mockSystemService));
    private final AtomicReference<CommitAuditHeaders> sent = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        doReturn("https://test.ehrbase.org/ehrbase").when(controller).getContextPath();
        doReturn("test.ehr.controller").when(mockSystemService).getSystemId();
        doReturn(composition()).when(mockCompositionService).buildComposition(any(), any(), any());
        doReturn("test.template").when(mockCompositionService).retrieveTemplateId(any());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createCompositionBindsTheAuditHeadersForTheCommit() {
        doAnswer(captureSent(Optional.of(COMPOSITION_ID)))
                .when(mockCompositionService)
                .create(any(), any());

        controller.createComposition(auditHeaders(), JSON, null, null, EHR_ID, null, null, "{}");

        assertSentOnlyDuringTheCommit(sent);
    }

    @Test
    void updateCompositionBindsTheAuditHeadersForTheCommit() {
        doAnswer(captureSent(Optional.of(COMPOSITION_ID)))
                .when(mockCompositionService)
                .update(any(), any(), any());

        controller.updateComposition(
                auditHeaders(), JSON, null, null, VERSION_ID, EHR_ID, COMPOSITION_ID.toString(), null, null, "{}");

        assertSentOnlyDuringTheCommit(sent);
    }

    @Test
    void deleteCompositionBindsTheAuditHeadersForTheCommit() {
        doAnswer(captureSent(null)).when(mockCompositionService).delete(any(), any());

        controller.deleteComposition(auditHeaders(), EHR_ID, VERSION_ID);

        assertSentOnlyDuringTheCommit(sent);
    }

    /// Answers the service call with `result` and remembers the bound `CommitAuditHeaders.SENT`.
    private <T> Answer<T> captureSent(T result) {
        return invocation -> {
            sent.set(CommitAuditHeaders.SENT.get());
            return result;
        };
    }

    private static Composition composition() {
        Archetyped archetypeDetails = new Archetyped();
        archetypeDetails.setArchetypeId(new ArchetypeID("openEHR-EHR-COMPOSITION.report.v1"));
        TemplateId templateId = new TemplateId();
        templateId.setValue("test.template");
        archetypeDetails.setTemplateId(templateId);
        archetypeDetails.setRmVersion("1.0.4");
        Composition composition = new Composition();
        composition.setArchetypeDetails(archetypeDetails);
        return composition;
    }
}
