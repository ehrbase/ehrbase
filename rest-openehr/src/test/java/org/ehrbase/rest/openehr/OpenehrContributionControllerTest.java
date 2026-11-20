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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;

class OpenehrContributionControllerTest {

    private static final UUID EHR_ID = UUID.fromString("3f1d0f6e-9a4b-4f0e-8a51-6c1f9f8e2b7a");
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final ContributionService mockContributionService = mock();
    private final OpenehrContributionController controller =
            spy(new OpenehrContributionController(mockContributionService));
    private final ContributionCreateDto contribution = new ContributionCreateDto();

    @BeforeEach
    void setUp() {
        doReturn("https://test.ehrbase.org/ehrbase").when(controller).getContextPath();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createContributionChecksTheSystemIdsBeforeTheCommit() {
        doReturn(UUID.randomUUID()).when(mockContributionService).commitContribution(any(), any());

        controller.createContribution(null, null, JSON, null, null, EHR_ID.toString(), contribution);

        InOrder order = inOrder(mockContributionService);
        order.verify(mockContributionService).checkSystemIds(contribution);
        order.verify(mockContributionService).commitContribution(EHR_ID, contribution);
    }

    @Test
    void createContributionCommitsNothingWhenASystemIdDiffers() {
        doThrow(new InvalidApiParameterException("system_id differs"))
                .when(mockContributionService)
                .checkSystemIds(any());

        assertThatThrownBy(() ->
                        controller.createContribution(null, null, JSON, null, null, EHR_ID.toString(), contribution))
                .isInstanceOf(InvalidApiParameterException.class);
        verify(mockContributionService, never()).commitContribution(any(), any());
    }
}
