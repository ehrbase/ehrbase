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
package org.ehrbase.rest.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.RevisionHistory;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.service.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

/**
 * Migrated from OpenehrVersionedEhrStatusControllerTest — equivalent for composition versioned container.
 */
class VersionedCompositionControllerTest {

    private static final UUID EHR_ID = UUID.fromString("7d44b88c-4199-4bad-97dc-d78268e01398");
    private static final UUID COMP_ID = UUID.fromString("a6ddec4c-a68a-49ef-963e-3e0bc1970a28");
    private static final String SYSTEM_ID = "test.versioned.controller";

    private final CompositionService mockCompositionService = mock();
    private final EhrService mockEhrService = mock();
    private final RequestContext mockRequestContext = mock();

    private final VersionedCompositionController controller =
            new VersionedCompositionController(mockCompositionService, mockEhrService, mockRequestContext);

    @BeforeEach
    void setUp() {
        Mockito.reset(mockCompositionService, mockEhrService, mockRequestContext);
    }

    @Test
    void getVersionedComposition() {
        var vc = mock(VersionedComposition.class);
        when(mockCompositionService.getVersionedComposition(EHR_ID, COMP_ID)).thenReturn(vc);

        var response = controller.getVersionedComposition(EHR_ID.toString(), COMP_ID.toString());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vc);
    }

    @Test
    void getVersionedCompositionInvalidEhrId() {
        assertThatThrownBy(() -> controller.getVersionedComposition("invalid", COMP_ID.toString()))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    @Test
    void getRevisionHistory() {
        var history = mock(RevisionHistory.class);
        when(mockCompositionService.getRevisionHistoryOfVersionedComposition(EHR_ID, COMP_ID))
                .thenReturn(history);

        var response = controller.getRevisionHistory(EHR_ID.toString(), COMP_ID.toString());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(history);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getVersionByUid() {
        String versionUid = COMP_ID + "::" + SYSTEM_ID + "::1";
        OriginalVersion<Composition> ov = mock(OriginalVersion.class);
        when(mockCompositionService.getOriginalVersionComposition(eq(EHR_ID), eq(COMP_ID), eq(1)))
                .thenReturn(Optional.of(ov));

        var response = controller.getVersionByUid(EHR_ID.toString(), COMP_ID.toString(), versionUid);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(ov);
    }

    @Test
    void getVersionByUidNotFound() {
        String versionUid = COMP_ID + "::" + SYSTEM_ID + "::99";
        when(mockCompositionService.getOriginalVersionComposition(eq(EHR_ID), eq(COMP_ID), eq(99)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getVersionByUid(EHR_ID.toString(), COMP_ID.toString(), versionUid))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void getVersionByUidInvalidFormat() {
        assertThatThrownBy(() -> controller.getVersionByUid(EHR_ID.toString(), COMP_ID.toString(), "not-a-version-uid"))
                .isInstanceOf(InvalidApiParameterException.class);
    }
}
