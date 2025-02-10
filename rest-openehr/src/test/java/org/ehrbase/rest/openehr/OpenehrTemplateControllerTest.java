/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
import static org.mockito.Mockito.spy;

import com.nedap.archie.rm.composition.Composition;
import java.util.List;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredStringFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.TemplateMetaDataDto;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class OpenehrTemplateControllerTest {

    private static final String CONTEXT_PATH = "https://template.test/ehrbase/rest";
    private static final String SAMPLE_OPT =
            """
    <?xml version="1.0" encoding="utf-8"?>
    <template xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://schemas.openehr.org/v1">
        <test></test>
    </template>
    """;
    private static final WebTemplate SAMPLE_WEB_TEMPLATE = new WebTemplate();
    private static final String SAMPLE_ID = "test-template";

    private final TemplateService mockTemplateService = mock();

    private final CompositionService mockCompositionService = mock();

    private final OpenehrTemplateController spyController =
            spy(new OpenehrTemplateController(mockTemplateService, mockCompositionService));

    @BeforeEach
    void setUp() {
        Mockito.reset(mockTemplateService, mockCompositionService, spyController);
        doReturn(CONTEXT_PATH).when(spyController).getContextPath();
    }

    private OpenehrTemplateController controller() {
        doReturn(SAMPLE_ID).when(mockTemplateService).create(any());
        doReturn(SAMPLE_OPT).when(mockTemplateService).findOperationalTemplate(any(), any());
        doReturn(SAMPLE_WEB_TEMPLATE).when(mockTemplateService).findWebTemplate(SAMPLE_ID);
        return spyController;
    }

    @ParameterizedTest
    @CsvSource({"application/json", "application/xml"})
    void getTemplatesADL1_4(String accept) {

        TemplateMetaDataDto metaDataDto = new TemplateMetaDataDto();
        metaDataDto.setTemplateId(SAMPLE_ID);

        doReturn(List.of(metaDataDto)).when(mockTemplateService).getAllTemplates();

        var response = controller().getTemplatesClassic("1.0.3", null, accept);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders()).containsEntry(HttpHeaders.CONTENT_TYPE, List.of(accept));
        assertThat(response.getHeaders())
                .containsEntry(HttpHeaders.LOCATION, List.of(CONTEXT_PATH + "/definition/template/adl1.4"));
        assertThat(response.getBody()).isEqualTo(List.of(metaDataDto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "return=minimal", "return=representation"})
    void createTemplateADL1_4(String prefer) {

        var response = controller().createTemplateClassic("1.0.3", null, prefer, SAMPLE_OPT);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders())
                .containsEntry(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_XML_VALUE));
        assertThat(response.getHeaders())
                .containsEntry(
                        HttpHeaders.LOCATION, List.of(CONTEXT_PATH + "/definition/template/adl1.4/" + SAMPLE_ID));

        if ("return=representation".equals(prefer)) {
            assertThat(response.getBody()).isEqualTo(SAMPLE_OPT);
        } else {
            assertThat(response.getBody()).isNull();
        }
    }

    @Test
    void createTemplateADL1_4_OPTInvalidError() {

        OpenehrTemplateController controller = controller();
        assertThatThrownBy(() -> controller.createTemplateClassic("1.0.3", null, null, "not a xml"))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessage("error: Content is not allowed in prolog.");
    }

    @Test
    void getTemplateADL1_4_OPT() {

        ResponseEntity<?> response =
                controller().getTemplateClassic("1.0.3", null, MediaType.APPLICATION_XML_VALUE, SAMPLE_ID);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders()).containsEntry(HttpHeaders.CONTENT_TYPE, List.of("application/xml"));
        assertThat(response.getHeaders())
                .containsEntry(
                        HttpHeaders.LOCATION, List.of(CONTEXT_PATH + "/definition/template/adl1.4/" + SAMPLE_ID));
        assertThat(response.getBody()).isInstanceOf(String.class).isEqualTo(SAMPLE_OPT);
    }

    @ParameterizedTest
    @CsvSource({"application/json", "application/openehr.wt+json"})
    void getTemplateADL1_4_WebTemplate(String accept) {

        ResponseEntity<?> response = controller().getTemplateClassic("1.0.3", null, accept, SAMPLE_ID);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders()).containsEntry(HttpHeaders.CONTENT_TYPE, List.of(accept));
        assertThat(response.getHeaders())
                .containsEntry(
                        HttpHeaders.LOCATION, List.of(CONTEXT_PATH + "/definition/template/adl1.4/" + SAMPLE_ID));
        assertThat(response.getBody()).isInstanceOf(WebTemplate.class).isEqualTo(SAMPLE_WEB_TEMPLATE);
    }

    @ParameterizedTest
    @CsvSource({
        "application/xml",
        "application/json",
        "application/openehr.wt.structured.schema+json",
        "application/openehr.wt.flat.schema+json"
    })
    void getTemplateExample(String accept) {

        Composition composition = new Composition();
        StructuredString structuredString = new StructuredString("\"string\"", StructuredStringFormat.JSON);

        doReturn(composition).when(mockTemplateService).buildExample(SAMPLE_ID);
        doReturn(structuredString).when(mockCompositionService).serialize(any(), any());

        ResponseEntity<?> response = controller().getTemplateExample(accept, SAMPLE_ID, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders()).containsEntry(HttpHeaders.CONTENT_TYPE, List.of(accept));
        assertThat(response.getHeaders())
                .containsEntry(
                        HttpHeaders.LOCATION,
                        List.of(CONTEXT_PATH + "/definition/template/adl1.4/" + SAMPLE_ID + "/example"));
        assertThat(response.getBody()).isEqualTo("\"string\"");
    }

    @ParameterizedTest
    @CsvSource({"application/json", "application/openehr.wt+json"})
    void getWebTemplate(String accept) {

        ResponseEntity<?> response = controller().getWebTemplate(accept, SAMPLE_ID);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders()).containsEntry(HttpHeaders.CONTENT_TYPE, List.of(accept));
        assertThat(response.getHeaders())
                .containsEntry(
                        HttpHeaders.LOCATION,
                        List.of(CONTEXT_PATH + "/definition/template/adl1.4/" + SAMPLE_ID + "/webtemplate"));
        assertThat(response.getHeaders()).containsKey("Deprecated");
        assertThat(response.getHeaders()).containsKey("Link");
        assertThat(response.getBody()).isInstanceOf(WebTemplate.class).isEqualTo(SAMPLE_WEB_TEMPLATE);
    }

    @Test
    void templateADL2NotImplemented() {

        OpenehrTemplateController controller = controller();
        assertThat(controller.getTemplatesNew(null, null, null).getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(controller
                        .createTemplateNew(null, null, null, null, null, null, null)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(controller.getTemplatesNew(null, null, null).getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }
}
