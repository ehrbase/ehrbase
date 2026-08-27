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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;

class OpenehrCompositionControllerTest {

    private static final String CONTEXT_PATH = "https://test.composition.controller/ehrbase/rest";

    private static final String SYSTEM_ID = "test.composition.controller";

    private final CompositionService mockCompositionService = mock();

    private final SystemService mockSystemService = mock();

    private final OpenehrCompositionController spyController =
            spy(new OpenehrCompositionController(mockCompositionService, mockSystemService));

    @BeforeEach
    void setUp() {
        Mockito.reset(mockCompositionService, mockSystemService, spyController);
        doReturn(CONTEXT_PATH).when(spyController).getContextPath();
    }

    @AfterEach
    void tearDown() {
        // ensure the context is clean after each test
        RequestContextHolder.resetRequestAttributes();
    }

    private OpenehrCompositionController controller() {
        return spyController;
    }

    @ParameterizedTest
    @ValueSource(strings = {"%s", "\"%s\""})
    void updateCompositionAcceptsQuotedAndUnquotedIfMatch(String ifMatchTemplate) {

        UUID ehrId = UUID.fromString("d83a16ae-2644-4706-8911-282772c10137");
        UUID compositionId = UUID.fromString("305eb2fd-c228-445c-ada7-5429d852fbb2");
        ObjectVersionId currentVersionId = new ObjectVersionId(compositionId.toString(), SYSTEM_ID, "2");

        String requestBody = "{}";
        Composition composition = new Composition();

        doReturn(composition).when(mockCompositionService).buildComposition(requestBody, CompositionFormat.JSON, null);
        doReturn(Optional.of(compositionId)).when(mockCompositionService).update(ehrId, currentVersionId, composition);
        doReturn(SYSTEM_ID).when(mockSystemService).getSystemId();

        String ifMatch = ifMatchTemplate.formatted(currentVersionId.getValue());
        ResponseEntity<?> response = controller()
                .updateComposition(
                        null,
                        null,
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_JSON_VALUE,
                        null,
                        ifMatch,
                        ehrId.toString(),
                        compositionId.toString(),
                        null,
                        null,
                        requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"%s::%s::3\"".formatted(compositionId, SYSTEM_ID));
        assertThat(response.getHeaders().getLocation())
                .hasToString(CONTEXT_PATH + "/ehr/" + ehrId + "/composition/" + compositionId);
    }

    /**
     * An unusable <code>If-Match</code> must be rejected with 412 <em>before</em> the composition is written,
     * so that the client never receives an error for an update that has already been committed.
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "*",
                "\"*\"",
                "W/\"305eb2fd-c228-445c-ada7-5429d852fbb2::test.composition.controller::2\"",
                "\"305eb2fd-c228-445c-ada7-5429d852fbb2::test.composition.controller::2",
                "305eb2fd-c228-445c-ada7-5429d852fbb2::test.composition.controller::2\"",
                "305eb2fd-c228-445c-ada7-5429d852fbb2",
                "305eb2fd-c228-445c-ada7-5429d852fbb2::test.composition.controller::",
                "305eb2fd-c228-445c-ada7-5429d852fbb2::test.composition.controller::2::3"
            })
    void updateCompositionRejectsUnusableIfMatch(String ifMatch) {

        UUID ehrId = UUID.fromString("d83a16ae-2644-4706-8911-282772c10137");
        UUID compositionId = UUID.fromString("305eb2fd-c228-445c-ada7-5429d852fbb2");

        String requestBody = "{}";

        doReturn(new Composition())
                .when(mockCompositionService)
                .buildComposition(requestBody, CompositionFormat.JSON, null);

        OpenehrCompositionController controller = controller();
        assertThatThrownBy(() -> controller.updateComposition(
                        null,
                        null,
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_JSON_VALUE,
                        null,
                        ifMatch,
                        ehrId.toString(),
                        compositionId.toString(),
                        null,
                        null,
                        requestBody))
                .isInstanceOf(PreconditionFailedException.class);

        verify(mockCompositionService, never()).update(any(), any(), any());
    }
}
