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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.repository.ContributionRepository;
import org.ehrbase.repository.ContributionRepository.ContributionRecord;
import org.ehrbase.service.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;

class ContributionControllerTest {

    private static final UUID EHR_ID = UUID.fromString("7d44b88c-4199-4bad-97dc-d78268e01398");
    private static final UUID CONTRIB_ID = UUID.fromString("a6ddec4c-a68a-49ef-963e-3e0bc1970a28");

    private final ContributionRepository mockRepo = mock();
    private final EhrService mockEhrService = mock();
    private final RequestContext mockRequestContext = mock();

    private final ContributionController spyController =
            spy(new ContributionController(mockRepo, mockEhrService, mockRequestContext));

    @BeforeEach
    void setUp() {
        Mockito.reset(mockRepo, mockEhrService, mockRequestContext, spyController);
        doReturn(URI.create("https://test/api/v1/ehrs/" + EHR_ID + "/contributions"))
                .when(spyController)
                .locationUri(any(String[].class));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createContribution() {
        when(mockRepo.createContribution(EHR_ID, "composition", "creation")).thenReturn(CONTRIB_ID);
        var record = mock(ContributionRecord.class);
        when(mockRepo.findById(CONTRIB_ID)).thenReturn(Optional.of(record));

        var response = spyController.createContribution(EHR_ID.toString(), "composition", "creation");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(record);
    }

    @Test
    void createContributionEhrNotModifiable() {
        doThrow(new org.ehrbase.api.exception.StateConflictException("EHR not modifiable"))
                .when(mockEhrService)
                .checkEhrExistsAndIsModifiable(EHR_ID);

        assertThatThrownBy(() -> spyController.createContribution(EHR_ID.toString(), "composition", "creation"))
                .isInstanceOf(org.ehrbase.api.exception.StateConflictException.class);
    }

    @Test
    void getContribution() {
        var record = mock(ContributionRecord.class);
        when(mockRepo.findById(CONTRIB_ID)).thenReturn(Optional.of(record));

        var response = spyController.getContribution(EHR_ID.toString(), CONTRIB_ID.toString());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(record);
    }

    @Test
    void getContributionNotFound() {
        when(mockRepo.findById(CONTRIB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spyController.getContribution(EHR_ID.toString(), CONTRIB_ID.toString()))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void getContributionInvalidId() {
        assertThatThrownBy(() -> spyController.getContribution(EHR_ID.toString(), "not-a-uuid"))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    @Test
    void listContributions() {
        when(mockRepo.findByEhr(EHR_ID)).thenReturn(List.of());

        var response = spyController.listContributions(EHR_ID.toString());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
