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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.api.knowledge.TemplateMetaData;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.SchemaExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TEMPLATEID;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Migrated from OpenehrTemplateControllerTest (8 tests) + new tests.
 */
class TemplateControllerTest {

    private final KnowledgeCacheService mockKnowledgeCache = mock();
    private final TemplateService mockTemplateService = mock();
    private final SchemaExecutorService mockSchemaExecutor = mock();
    private final RequestContext mockRequestContext = mock();

    private final TemplateController spyController = spy(
            new TemplateController(mockKnowledgeCache, mockTemplateService, mockSchemaExecutor, mockRequestContext));

    @BeforeEach
    void setUp() {
        Mockito.reset(mockKnowledgeCache, mockTemplateService, mockSchemaExecutor, mockRequestContext, spyController);
        doReturn(URI.create("https://test/api/v2/templates"))
                .when(spyController)
                .locationUri(any(String[].class));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // Migrated: createTemplateADL1_4 — successful upload
    @Test
    void uploadAdl14() {
        var opt = mock(OPERATIONALTEMPLATE.class);
        when(mockKnowledgeCache.addOperationalTemplate(opt)).thenReturn("test-template");
        when(mockKnowledgeCache.findUuidByTemplateId("test-template")).thenReturn(Optional.of(UUID.randomUUID()));
        when(mockSchemaExecutor.executeSchemaGeneration(any(), any(), org.mockito.ArgumentMatchers.anyShort()))
                .thenReturn("test_template");
        when(mockKnowledgeCache.retrieveOperationalTemplate("test-template")).thenReturn(Optional.of(opt));

        var response = spyController.uploadAdl14(opt);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("templateId");
        assertThat(response.getBody().get("templateId")).isEqualTo("test-template");
    }

    // Migrated: getTemplatesADL1_4 — list templates
    @Test
    void listAdl14Templates() {
        var meta = new TemplateMetaData();
        var opt = mock(OPERATIONALTEMPLATE.class);
        var tid = mock(TEMPLATEID.class);
        when(tid.getValue()).thenReturn("test-template");
        when(opt.getTemplateId()).thenReturn(tid);
        when(opt.getConcept()).thenReturn("Test Concept");
        meta.setOperationalTemplate(opt);
        when(mockKnowledgeCache.listAllOperationalTemplates()).thenReturn(List.of(meta));

        var response = spyController.listAdl14Templates();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).templateId()).isEqualTo("test-template");
    }

    // Migrated: getTemplateADL1_4_OPT — get template as XML
    @Test
    void getAdl14AsXml() {
        var opt = mock(OPERATIONALTEMPLATE.class);
        when(mockKnowledgeCache.retrieveOperationalTemplate("test-template")).thenReturn(Optional.of(opt));

        var response = spyController.getAdl14AsXml("test-template");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(opt);
    }

    // Migrated: getTemplateADL1_4_OPT — template not found
    @Test
    void getAdl14NotFound() {
        when(mockKnowledgeCache.retrieveOperationalTemplate("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spyController.getAdl14AsXml("nonexistent"))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    // Migrated: getTemplateADL1_4_WebTemplate
    @Test
    void getAdl14AsWebTemplate() {
        var wt = mock(WebTemplate.class);
        when(mockKnowledgeCache.getInternalTemplate("test-template")).thenReturn(wt);

        var response = spyController.getAdl14AsWebTemplate("test-template");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(wt);
    }

    // Migrated: getTemplateADL1_4_WebTemplate — not found
    @Test
    void getAdl14WebTemplateNotFound() {
        when(mockKnowledgeCache.getInternalTemplate("nonexistent")).thenReturn(null);

        assertThatThrownBy(() -> spyController.getAdl14AsWebTemplate("nonexistent"))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    // Migrated: templateADL2NotImplemented — 3 ADL2 stubs → UnsupportedOperationException
    @Test
    void uploadAdl2NotImplemented() {
        assertThatThrownBy(() -> spyController.uploadAdl2()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void listAdl2NotImplemented() {
        assertThatThrownBy(() -> spyController.listAdl2()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getAdl2NotImplemented() {
        var plainController =
                new TemplateController(mockKnowledgeCache, mockTemplateService, mockSchemaExecutor, mockRequestContext);
        assertThatThrownBy(() -> plainController.getAdl2("test", "1.0"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // Migrated: deleteTemplate — not found
    @Test
    void deleteTemplateNotFound() {
        when(mockKnowledgeCache.retrieveOperationalTemplate("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spyController.deleteTemplate("nonexistent"))
                .isInstanceOf(ObjectNotFoundException.class);
    }
}
