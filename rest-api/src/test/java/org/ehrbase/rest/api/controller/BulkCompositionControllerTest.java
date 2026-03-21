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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nedap.archie.rm.composition.Composition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.service.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class BulkCompositionControllerTest {

    private static final UUID EHR_ID = UUID.fromString("7d44b88c-4199-4bad-97dc-d78268e01398");
    private static final String SYSTEM_ID = "test.bulk.controller";

    private final CompositionService mockCompositionService = mock();
    private final EhrService mockEhrService = mock();
    private final SystemService mockSystemService = mock();
    private final RequestContext mockRequestContext = mock();

    private final BulkCompositionController controller = new BulkCompositionController(
            mockCompositionService, mockEhrService, mockSystemService, mockRequestContext);

    @BeforeEach
    void setUp() {
        Mockito.reset(mockCompositionService, mockEhrService, mockSystemService, mockRequestContext);
        when(mockSystemService.getSystemId()).thenReturn(SYSTEM_ID);
    }

    @Test
    void bulkCreateReturns207() {
        var composition = mock(Composition.class);
        UUID compId = UUID.randomUUID();
        when(mockCompositionService.buildComposition(any(), any(), any())).thenReturn(composition);
        when(mockCompositionService.create(eq(EHR_ID), any())).thenReturn(Optional.of(compId));

        var body = Map.of("compositions", (Object) List.of("{\"test\":true}"));
        var response = controller.bulkCreate(EHR_ID.toString(), body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.MULTI_STATUS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().results()).hasSize(1);
        assertThat(response.getBody().results().get(0).status()).isEqualTo(201);
    }

    @Test
    void bulkCreateWithFailureReturns207WithMixedResults() {
        when(mockCompositionService.buildComposition(any(), any(), any()))
                .thenThrow(new RuntimeException("Parse error"));

        var body = Map.of("compositions", (Object) List.of("{\"invalid\"}"));
        var response = controller.bulkCreate(EHR_ID.toString(), body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.MULTI_STATUS);
        assertThat(response.getBody().results()).hasSize(1);
        assertThat(response.getBody().results().get(0).status()).isEqualTo(400);
    }

    @Test
    void bulkCreateEmptyBatch() {
        var body = Map.<String, Object>of("compositions", List.of());
        var response = controller.bulkCreate(EHR_ID.toString(), body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.MULTI_STATUS);
        assertThat(response.getBody().results()).isEmpty();
    }

    @Test
    void bulkUpdateReturns501() {
        var body = Map.of("compositions", (Object) List.of("comp1"));
        var response = controller.bulkUpdate(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.MULTI_STATUS);
        assertThat(response.getBody().results().get(0).status()).isEqualTo(501);
    }

    @Test
    void bulkDeleteReturns501() {
        var body = Map.of("compositions", (Object) List.of("comp1"));
        var response = controller.bulkDelete(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.MULTI_STATUS);
        assertThat(response.getBody().results().get(0).status()).isEqualTo(501);
    }
}
