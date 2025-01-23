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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.UnsupportedMediaTypeException;
import org.ehrbase.api.service.StoredQueryService;
import org.ehrbase.openehr.sdk.response.dto.QueryDefinitionResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;

public class OpenehrDefinitionQueryControllerTest {
    public static final String SAMPLE_QUERY = "SELECT e/ehr_id/vale FROM EHR e";
    private static final String CONTEXT_PATH = "https://test.definitions.controller/ehrbase/rest";

    private final StoredQueryService mockStoredQueryService = mock();
    private final OpenehrDefinitionQueryController spyController =
            spy(new OpenehrDefinitionQueryController(mockStoredQueryService));

    private OpenehrDefinitionQueryController controller() {
        doReturn(CONTEXT_PATH).when(spyController).getContextPath();
        return spyController;
    }

    private QueryDefinitionResultDto resultDto(String name, String version, String query) {
        QueryDefinitionResultDto resultDto = new QueryDefinitionResultDto();
        resultDto.setQueryText(query);
        resultDto.setQualifiedName(name);
        resultDto.setVersion(version);
        resultDto.setType("AQL");
        resultDto.setSaved(ZonedDateTime.now());
        return resultDto;
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(mockStoredQueryService);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void storeQueryErrorUnsupportedMediaType() {

        assertThatThrownBy(() -> testStoreQuery(
                        MediaType.APPLICATION_XML_VALUE, "some-name", SAMPLE_QUERY, SAMPLE_QUERY, null, response -> {}))
                .isInstanceOf(UnsupportedMediaTypeException.class)
                .hasMessage("application/xml");
    }

    @Test
    void storeQueryErrorEmptyQuery() {

        assertThatThrownBy(() ->
                        testStoreQuery(MediaType.APPLICATION_JSON_VALUE, "some-name", "", "", null, response -> {}))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessage("no aql query provided");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1.0.0", "0.42.0"})
    void storeQueryJson(String version) {

        String name = "test-query";
        String query = "SELECT es FROM EHR_STATUS es";

        testStoreQuery(
                MediaType.APPLICATION_JSON_VALUE,
                name,
                query,
                "{\"q\": \"%s\"}".formatted(query),
                version,
                response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull().satisfies(dto -> {
                        assertThat(dto.getType()).isEqualTo("AQL");
                        assertThat(dto.getName()).isEqualTo(name);
                        assertThat(dto.getVersion()).isEqualTo(version);
                        assertThat(dto.getQuery()).isEqualTo(query);
                        assertThat(dto.getSaved()).isNotNull().isNotBlank();
                    });
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1.0.0", "0.42.0"})
    void storeQueryText(String version) {

        String name = "test-query";
        String query = "SELECT es FROM EHR_STATUS es";

        testStoreQuery(MediaType.TEXT_PLAIN_VALUE, name, query, query, version, response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNull();
        });
    }

    void testStoreQuery(
            String contentType,
            String name,
            String query,
            String payload,
            String version,
            Consumer<ResponseEntity<QueryDefinitionResponseData>> check) {

        QueryDefinitionResultDto resultDto = resultDto(name, version, query);

        doReturn(resultDto).when(mockStoredQueryService).createStoredQuery(name, version, query);

        ResponseEntity<QueryDefinitionResponseData> response = controller()
                .putStoredQuery(
                        contentType,
                        MediaType.APPLICATION_JSON_VALUE,
                        name,
                        Optional.ofNullable(version),
                        "AQL",
                        payload);
        assertThat(response).satisfies(check);
    }

    @ParameterizedTest
    @ValueSource(strings = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    void getStoredQueryListEmpty(String accept) {

        doReturn(List.of()).when(mockStoredQueryService).retrieveStoredQueries("some-query");

        ResponseEntity<List<QueryDefinitionResponseData>> response =
                controller().getStoredQueryList(accept, "some-query");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().satisfies(dto -> assertThat(dto)
                .isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    void getStoredQueryList(String accept) {

        doReturn(List.of(resultDto("test-query", "1.0.0", SAMPLE_QUERY)))
                .when(mockStoredQueryService)
                .retrieveStoredQueries("test-query");

        ResponseEntity<List<QueryDefinitionResponseData>> response =
                controller().getStoredQueryList(accept, "test-query");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().satisfies(dto -> assertThat(dto)
                .hasSize(1));
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                "application/json|",
                "application/json|0.42.1",
                "application/json|1.0.0",
                "application/xml|",
                "application/json|0.1.0",
                "application/json|2.0.1"
            },
            delimiter = '|')
    void getStoredQueryVersion(String accept, String version) {

        String name = "test-query";
        doReturn(resultDto(name, version, SAMPLE_QUERY))
                .when(mockStoredQueryService)
                .retrieveStoredQuery(name, version);

        ResponseEntity<QueryDefinitionResponseData> response =
                controller().getStoredQueryVersion(accept, name, Optional.ofNullable(version));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().satisfies(dto -> {
            assertThat(dto.getType()).isEqualTo("AQL");
            assertThat(dto.getName()).isEqualTo(name);
            assertThat(dto.getVersion()).isEqualTo(version);
            assertThat(dto.getQuery()).isEqualTo(SAMPLE_QUERY);
            assertThat(dto.getSaved()).isNotNull().isNotBlank();
        });
    }
}
