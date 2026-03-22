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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.ViewCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class TemplateIntrospectionControllerTest {

    private final KnowledgeCacheService mockKnowledgeCache = mock();
    private final ViewCatalogService mockViewCatalogService = mock();
    private final RequestContext mockRequestContext = mock();

    private final TemplateIntrospectionController controller =
            new TemplateIntrospectionController(mockKnowledgeCache, mockViewCatalogService, mockRequestContext);

    @BeforeEach
    void setUp() {
        Mockito.reset(mockKnowledgeCache, mockViewCatalogService, mockRequestContext);
    }

    @Test
    void getWebTemplate() {
        var wt = mock(WebTemplate.class);
        when(mockKnowledgeCache.getInternalTemplate("test-template")).thenReturn(wt);

        var response = controller.getWebTemplate("test-template");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(wt);
    }

    @Test
    void getWebTemplateNotFound() {
        when(mockKnowledgeCache.getInternalTemplate("missing")).thenReturn(null);

        assertThatThrownBy(() -> controller.getWebTemplate("missing")).isInstanceOf(ObjectNotFoundException.class);
    }
}
