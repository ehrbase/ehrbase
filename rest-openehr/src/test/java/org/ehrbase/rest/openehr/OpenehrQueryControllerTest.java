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
package org.ehrbase.rest.openehr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashMap;
import java.util.Map;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.AqlQueryService;
import org.ehrbase.api.service.StoredQueryService;
import org.ehrbase.openehr.sdk.response.dto.MetaData;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;

public class OpenehrQueryControllerTest {

    public static final String SAMPLE_QUERY = "SELECT s FROM EHR_STATUS s";
    public static final Map<String, Object> SAMPLE_PARAMETER_MAP = Map.of("key", "value");
    public static final MetaData SAMPLE_META_DATA = new MetaData();

    private final AqlQueryService mockAqlQueryService = mock();

    private final StoredQueryService mockStoredQueryService = mock();

    private final AqlQueryContext mockQueryContext = mock();

    private final OpenehrQueryController spyController =
            spy(new OpenehrQueryController(mockAqlQueryService, mockStoredQueryService, mockQueryContext));

    @BeforeEach
    void setUp() {
        Mockito.reset(mockAqlQueryService, mockStoredQueryService, mockQueryContext, spyController);
        doReturn("https://openehr.test.com/rest").when(spyController).getContextPath();
    }

    @AfterEach
    void tearDown() {
        // ensure the context is clean after each test
        RequestContextHolder.resetRequestAttributes();
    }

    private OpenehrQueryController controller() {
        doReturn(SAMPLE_META_DATA).when(mockQueryContext).createMetaData(any());
        doReturn(new QueryResultDto()).when(mockAqlQueryService).query(any());
        return spyController;
    }

    private OpenehrQueryController controllerStoredQuery() {
        QueryDefinitionResultDto queryDefinitionResultDto = new QueryDefinitionResultDto();
        queryDefinitionResultDto.setQueryText(SAMPLE_QUERY);
        queryDefinitionResultDto.setQualifiedName("test_query");
        doReturn(queryDefinitionResultDto).when(mockStoredQueryService).retrieveStoredQuery(any(), any());
        return controller();
    }

    @ParameterizedTest
    @CsvSource({",", "10,0", "0,25"})
    void GETexecuteAddHocQuery(Integer fetch, Integer offset) {
        ResponseEntity<QueryResponseData> response = controller()
                .executeAdHocQuery(SAMPLE_QUERY, offset, fetch, SAMPLE_PARAMETER_MAP, MediaType.APPLICATION_JSON_VALUE);
        assertMetaData(response);
        assertAqlQueryRequest(new AqlQueryRequest(SAMPLE_QUERY, SAMPLE_PARAMETER_MAP, toLong(fetch), toLong(offset)));
    }

    private Long toLong(Object obj) {
        return switch (obj) {
            case null -> null;
            case Integer i -> i.longValue();
            case String s -> Long.parseLong(s);
            default -> throw new IllegalArgumentException(
                    "unexpected type " + obj.getClass().getName());
        };
    }

    @ParameterizedTest
    @CsvSource({",", "10,0", "0,25", "'1','2'"})
    void POSTexecuteAddHocQuery(Object fetch, Object offset) {
        ResponseEntity<QueryResponseData> response = controller()
                .executeAdHocQuery(
                        sampleAqlQuery(fetch, offset),
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        assertMetaData(response);
        assertAqlQueryRequest(new AqlQueryRequest(SAMPLE_QUERY, SAMPLE_PARAMETER_MAP, toLong(fetch), toLong(offset)));
    }

    private static Map<String, Object> sampleAqlQuery(Object fetch, Object offset) {
        Map<String, Object> map = sampleAqlJson(fetch, offset);
        map.put("q", SAMPLE_QUERY);
        return map;
    }

    private static Map<String, Object> sampleAqlJson(Object fetch, Object offset) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("query_parameters", SAMPLE_PARAMETER_MAP);
        if (fetch != null) {
            map.put("fetch", fetch);
        }
        if (offset != null) {
            map.put("offset", offset);
        }
        return map;
    }

    @Test
    void POSTexecuteAddHocQueryWithFetchInvalid() {

        String message = assertThrowsExactly(InvalidApiParameterException.class, () -> controller()
                        .executeAdHocQuery(
                                sampleAqlQuery("invalid", null),
                                MediaType.APPLICATION_JSON_VALUE,
                                MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .getMessage();
        assertEquals("invalid 'fetch' value 'invalid'", message);
    }

    @Test
    void POSTexecuteAddHocQueryWithOffsetInvalid() {
        String message = assertThrowsExactly(InvalidApiParameterException.class, () -> controller()
                        .executeAdHocQuery(
                                sampleAqlQuery(null, "invalid"),
                                MediaType.APPLICATION_JSON_VALUE,
                                MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .getMessage();
        assertEquals("invalid 'offset' value 'invalid'", message);
    }

    @ParameterizedTest
    @CsvSource({",", "10,0", "0,25"})
    void GETexecuteStoredQuery(Integer fetch, Integer offset) {
        ResponseEntity<QueryResponseData> response = controllerStoredQuery()
                .executeStoredQuery(
                        "my_qualified_query",
                        "v1.0.0",
                        offset,
                        fetch,
                        SAMPLE_PARAMETER_MAP,
                        MediaType.APPLICATION_JSON_VALUE);
        assertMetaData(response);
        assertAqlQueryRequest(new AqlQueryRequest(SAMPLE_QUERY, SAMPLE_PARAMETER_MAP, toLong(fetch), toLong(offset)));
    }

    @ParameterizedTest
    @CsvSource({",", "10,0", "0,25", "'1','2'"})
    void POSTexecuteStoredQuery(Object fetch, Object offset) {
        ResponseEntity<QueryResponseData> response = controllerStoredQuery()
                .executeStoredQuery(
                        "my_qualified_query",
                        "v1.0.0",
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_JSON_VALUE,
                        sampleAqlJson(fetch, offset));
        assertMetaData(response);
        assertAqlQueryRequest(new AqlQueryRequest(SAMPLE_QUERY, SAMPLE_PARAMETER_MAP, toLong(fetch), toLong(offset)));
    }

    @Test
    void POSTexecuteStoredQueryWithFetchInvalid() {

        String message = assertThrowsExactly(InvalidApiParameterException.class, () -> controllerStoredQuery()
                        .executeStoredQuery(
                                "my_qualified_query",
                                "v1.0.0",
                                MediaType.APPLICATION_JSON_VALUE,
                                MediaType.APPLICATION_JSON_VALUE,
                                sampleAqlJson("invalid", null)))
                .getMessage();
        assertEquals("invalid 'fetch' value 'invalid'", message);
    }

    @Test
    void POSTexecuteStoredQueryWithOffsetInvalid() {

        String message = assertThrowsExactly(InvalidApiParameterException.class, () -> controllerStoredQuery()
                        .executeStoredQuery(
                                "my_qualified_query",
                                "v1.0.0",
                                MediaType.APPLICATION_JSON_VALUE,
                                MediaType.APPLICATION_JSON_VALUE,
                                sampleAqlJson(null, "invalid")))
                .getMessage();
        assertEquals("invalid 'offset' value 'invalid'", message);
    }

    private void assertAqlQueryRequest(AqlQueryRequest aqlQueryRequest) {
        ArgumentCaptor<AqlQueryRequest> argument = ArgumentCaptor.forClass(AqlQueryRequest.class);
        verify(mockAqlQueryService).query(argument.capture());
        assertEquals(aqlQueryRequest, argument.getValue());
    }

    private void assertMetaData(ResponseEntity<QueryResponseData> response) {
        QueryResponseData body = response.getBody();
        assertNotNull(body);
        assertSame(SAMPLE_META_DATA, body.getMeta());
        verify(mockQueryContext).createMetaData(any());
    }
}
