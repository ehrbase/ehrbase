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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.ehrbase.api.dto.AqlExecutionInfo;
import org.ehrbase.api.dto.AqlExecutionOption;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.api.dto.AqlQueryResult;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.AqlQueryService;
import org.ehrbase.api.service.StatusService;
import org.ehrbase.api.service.StoredQueryService;
import org.ehrbase.openehr.sdk.response.dto.MetaData;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;
import org.ehrbase.rest.http.EHRbaseHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class OpenehrQueryControllerTest {

    private final AqlQueryService mockAqlQueryService = mock();

    private final StoredQueryService mockStoredQueryService = mock();

    private final StatusService mockStatusService = mock();

    private final OpenehrQueryController spyController =
            spy(new OpenehrQueryController(mockAqlQueryService, mockStoredQueryService, mockStatusService));

    @BeforeEach
    void setUp() {
        Mockito.reset(mockAqlQueryService, mockStoredQueryService, mockStatusService, spyController);
        doReturn("https://openehr.test.com/rest").when(spyController).getContextPath();

        spyController.generatorDetailsEnabled = false;
    }

    @AfterEach
    void tearDown() {
        // ensure the context is clean after each test
        RequestContextHolder.resetRequestAttributes();
    }

    private OpenehrQueryController controller(Supplier<QueryResultDto> resultSupplier) {
        return controller(resultSupplier, AqlExecutionInfo.None);
    }

    private OpenehrQueryController controller(Supplier<QueryResultDto> resultSupplier, AqlExecutionInfo executionInfo) {
        AqlQueryResult aqlQueryResult = new AqlQueryResult(resultSupplier.get(), executionInfo);
        doReturn(aqlQueryResult).when(mockAqlQueryService).query(any());
        return spyController;
    }

    private OpenehrQueryController controllerStoredQuery(String storedQuery, Supplier<QueryResultDto> resultSupplier) {
        return controllerStoredQuery(storedQuery, resultSupplier, AqlExecutionInfo.None);
    }

    private OpenehrQueryController controllerStoredQuery(
            String storedQuery, Supplier<QueryResultDto> resultSupplier, AqlExecutionInfo executionInfo) {
        QueryDefinitionResultDto queryDefinitionResultDto = new QueryDefinitionResultDto();
        queryDefinitionResultDto.setQueryText(storedQuery);
        queryDefinitionResultDto.setQualifiedName("test_query");
        doReturn(queryDefinitionResultDto).when(mockStoredQueryService).retrieveStoredQuery(any(), any());
        return controller(resultSupplier, executionInfo);
    }

    private OpenehrQueryController controllerAdHocQueryWithExecutionOptions(
            boolean dryRun, String executedSQL, Map<String, Object> queryPlan) {

        AqlExecutionInfo executionInfo = new AqlExecutionInfo(dryRun, executedSQL, queryPlan);
        OpenehrQueryController controller = controller(QueryResultDto::new, executionInfo);
        configureExecutionOptions(controller, dryRun, executedSQL, queryPlan);
        return controller;
    }

    private OpenehrQueryController controllerStoredQueryWithExecutionOptions(
            String storedQuery, boolean dryRun, String executedSQL, Map<String, Object> queryPlan) {
        AqlExecutionInfo executionInfo = new AqlExecutionInfo(dryRun, executedSQL, queryPlan);
        OpenehrQueryController controller = controllerStoredQuery(storedQuery, QueryResultDto::new, executionInfo);
        configureExecutionOptions(controller, dryRun, executedSQL, queryPlan);
        return controller;
    }

    private void configureExecutionOptions(
            OpenehrQueryController controller, boolean dryRun, String executedSQL, Map<String, Object> queryPlan) {

        controller.executionOptionsEnabled = true;

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(EHRbaseHeader.AQL_DRY_RUN, dryRun);
        request.addHeader(EHRbaseHeader.AQL_EXECUTED_SQL, executedSQL != null);
        request.addHeader(EHRbaseHeader.AQL_QUERY_PLAN, queryPlan != null);

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private MetaData expectedMetaData(Consumer<MetaData> customize) {

        MetaData expected = new MetaData();
        expected.setHref("https://openehr.test.com/rest/query/aql");
        expected.setType("RESULTSET");
        expected.setSchemaVersion("1.0.4");
        expected.setCreated(OffsetDateTime.now());
        expected.setAdditionalProperty(MetaData.AdditionalProperty.resultSize, 0);
        customize.accept(expected);
        return expected;
    }

    @Test
    void GETexecuteAddHocQuery() {

        controller(QueryResultDto::new)
                .executeAdHocQuery("SELECT s FROM EHR_STATUS s", null, null, null, MediaType.APPLICATION_JSON_VALUE);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), null, null, AqlExecutionOption.None));
    }

    @Test
    void GETexecuteAddHocQueryWithFetch() {

        controller(QueryResultDto::new)
                .executeAdHocQuery("SELECT s FROM EHR_STATUS s", null, 10, null, MediaType.APPLICATION_JSON_VALUE);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), 10L, null, AqlExecutionOption.None));
    }

    @Test
    void GETexecuteAddHocQueryWithOffset() {

        controller(QueryResultDto::new)
                .executeAdHocQuery("SELECT s FROM EHR_STATUS s", 25, null, null, MediaType.APPLICATION_JSON_VALUE);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), null, 25L, AqlExecutionOption.None));
    }

    @Test
    void GETexecuteAddHocQueryWithParameter() {

        controller(QueryResultDto::new)
                .executeAdHocQuery(
                        "SELECT s FROM EHR_STATUS s",
                        null,
                        null,
                        Map.of("key", "value"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertAqlQueryRequest(new AqlQueryRequest(
                "SELECT s FROM EHR_STATUS s", Map.of("key", "value"), null, null, AqlExecutionOption.None));
    }

    @Test
    void GETexecuteAddHocReturnMeta() {

        OpenehrQueryController controller = controller(QueryResultDto::new);
        controller.generatorDetailsEnabled = false;

        ResponseEntity<QueryResponseData> response = controller(QueryResultDto::new)
                .executeAdHocQuery("SELECT s FROM EHR_STATUS s", null, null, null, MediaType.APPLICATION_JSON_VALUE);

        assertResponseMeta(response, expectedMetaData(expected -> {}));
    }

    @Test
    void GETexecuteAddHocReturnMetaWithGenerator() {

        OpenehrQueryController controller = controller(QueryResultDto::new);
        controller.generatorDetailsEnabled = true;

        ResponseEntity<QueryResponseData> response = controller.executeAdHocQuery(
                "SELECT s FROM EHR_STATUS s", null, null, null, MediaType.APPLICATION_JSON_VALUE);

        assertResponseMeta(response, expectedMetaData(expected -> {
            expected.setGenerator("EHRBase/"); // version is ignored for test
        }));
    }

    @Test
    void GETexecuteAdHocWithExecutionOptionsDisable() {

        OpenehrQueryController controller = controller(QueryResultDto::new);
        assertFalse(controller.executionOptionsEnabled);

        ResponseEntity<QueryResponseData> response = controller.executeAdHocQuery(
                "SELECT s FROM EHR_STATUS s", null, null, null, MediaType.APPLICATION_JSON_VALUE);

        MetaData metaData = responseMeta(response);
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.dryRun));
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.executedSQL));
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.queryPlan));
    }

    @Test
    void GETexecuteAdHocWithExecutionOptionsEnabled() {

        OpenehrQueryController controller =
                controllerAdHocQueryWithExecutionOptions(true, "SELECT TRUE", Map.of("ad", "hoc"));

        ResponseEntity<QueryResponseData> response = controller.executeAdHocQuery(
                "SELECT s FROM EHR_STATUS s", null, null, null, MediaType.APPLICATION_JSON_VALUE);

        MetaData metaData = responseMeta(response);
        assertEquals(Boolean.TRUE, metaData.getAdditionalProperty(MetaData.AdditionalProperty.dryRun));
        assertEquals("SELECT TRUE", metaData.getAdditionalProperty(MetaData.AdditionalProperty.executedSQL));
        assertEquals(Map.of("ad", "hoc"), metaData.getAdditionalProperty(MetaData.AdditionalProperty.queryPlan));
    }

    @Test
    void POSTexecuteAddHocQuery() {

        controller(QueryResultDto::new)
                .executeAdHocQuery(
                        Map.of("q", "SELECT s FROM EHR_STATUS s"),
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), null, null, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteAddHocQueryWithFetch() {

        controller(QueryResultDto::new)
                .executeAdHocQuery(
                        Map.of("q", "SELECT s FROM EHR_STATUS s", "fetch", 10),
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), 10L, null, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteAddHocQueryWithFetchAsString() {

        controller(QueryResultDto::new)
                .executeAdHocQuery(
                        Map.of("q", "SELECT s FROM EHR_STATUS s", "fetch", "10"),
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), 10L, null, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteAddHocQueryWithFetchInvalid() {

        String message = assertThrowsExactly(InvalidApiParameterException.class, () -> controller(QueryResultDto::new)
                        .executeAdHocQuery(
                                Map.of("q", "SELECT s FROM EHR_STATUS s", "fetch", "invalid"),
                                MediaType.APPLICATION_JSON_VALUE,
                                MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .getMessage();
        assertEquals("invalid 'fetch' value 'invalid'", message);
    }

    @Test
    void POSTexecuteAddHocQueryWithOffset() {

        controller(QueryResultDto::new)
                .executeAdHocQuery(
                        Map.of("q", "SELECT s FROM EHR_STATUS s", "offset", 30),
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), null, 30L, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteAddHocQueryWithOffsetAsString() {

        controller(QueryResultDto::new)
                .executeAdHocQuery(
                        Map.of("q", "SELECT s FROM EHR_STATUS s", "offset", "30"),
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), null, 30L, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteAddHocQueryWithOffsetInvalid() {

        String message = assertThrowsExactly(InvalidApiParameterException.class, () -> controller(QueryResultDto::new)
                        .executeAdHocQuery(
                                Map.of("q", "SELECT s FROM EHR_STATUS s", "offset", "invalid"),
                                MediaType.APPLICATION_JSON_VALUE,
                                MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .getMessage();
        assertEquals("invalid 'offset' value 'invalid'", message);
    }

    @Test
    void POSTexecuteAddHocQueryWithParameters() {

        controller(QueryResultDto::new)
                .executeAdHocQuery(
                        Map.of("q", "SELECT s FROM EHR_STATUS s", "query_parameters", Map.of("key", "value")),
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        assertAqlQueryRequest(new AqlQueryRequest(
                "SELECT s FROM EHR_STATUS s", Map.of("key", "value"), null, null, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteAddHocReturnMeta() {

        OpenehrQueryController controller = controller(QueryResultDto::new);
        controller.generatorDetailsEnabled = false;

        ResponseEntity<QueryResponseData> response = controller.executeAdHocQuery(
                Map.of("q", "SELECT s FROM EHR_STATUS s"),
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        assertResponseMeta(response, expectedMetaData(expected -> {
            expected.setHref(null); // no _href for POST results
        }));
    }

    @Test
    void POSTexecuteAddHocReturnMetaWithGenerator() {

        OpenehrQueryController controller = controller(QueryResultDto::new);
        controller.generatorDetailsEnabled = true;

        ResponseEntity<QueryResponseData> response = controller.executeAdHocQuery(
                Map.of("q", "SELECT s FROM EHR_STATUS s"),
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        assertResponseMeta(response, expectedMetaData(expected -> {
            expected.setHref(null); // no _href for POST results
            expected.setGenerator("EHRBase/"); // version is ignored for test
        }));
    }

    @Test
    void POSTexecuteAdHocWithExecutionOptionsDisable() {

        OpenehrQueryController controller = controller(QueryResultDto::new);
        assertFalse(controller.executionOptionsEnabled);

        ResponseEntity<QueryResponseData> response = controller.executeAdHocQuery(
                Map.of("q", "SELECT c FROM CONTRIBUTION s"),
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        MetaData metaData = responseMeta(response);
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.dryRun));
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.executedSQL));
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.queryPlan));
    }

    @Test
    void POSTexecuteAdHocWithExecutionOptionsEnabled() {

        OpenehrQueryController controller =
                controllerAdHocQueryWithExecutionOptions(true, "SELECT TRUE", Map.of("ad", "hoc"));

        ResponseEntity<QueryResponseData> response = controller.executeAdHocQuery(
                Map.of("q", "SELECT c FROM CONTRIBUTION s"),
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        MetaData metaData = responseMeta(response);
        assertEquals(Boolean.TRUE, metaData.getAdditionalProperty(MetaData.AdditionalProperty.dryRun));
        assertEquals("SELECT TRUE", metaData.getAdditionalProperty(MetaData.AdditionalProperty.executedSQL));
        assertEquals(Map.of("ad", "hoc"), metaData.getAdditionalProperty(MetaData.AdditionalProperty.queryPlan));
    }

    @Test
    void GETexecuteStoredQuery() {

        controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                .executeStoredQuery("my_qualified_query", "v1.0.0", null, null, null, MediaType.APPLICATION_JSON_VALUE);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), null, null, AqlExecutionOption.None));
    }

    @Test
    void GETexecuteStoredQueryWithFetch() {

        controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                .executeStoredQuery("my_qualified_query", "v1.0.0", null, 15, null, MediaType.APPLICATION_JSON_VALUE);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), 15L, null, AqlExecutionOption.None));
    }

    @Test
    void GETexecuteStoredQueryWithOffset() {

        controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                .executeStoredQuery("my_qualified_query", "v1.0.0", 25, null, null, MediaType.APPLICATION_JSON_VALUE);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), null, 25L, AqlExecutionOption.None));
    }

    @Test
    void GETexecuteStoredQueryWithParameter() {

        controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                .executeStoredQuery(
                        "my_qualified_query",
                        "v1.0.0",
                        null,
                        null,
                        Map.of("key", "value"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertAqlQueryRequest(new AqlQueryRequest(
                "SELECT s FROM EHR_STATUS s", Map.of("key", "value"), null, null, AqlExecutionOption.None));
    }

    @Test
    void GETexecuteStoredQueryReturnMeta() {

        OpenehrQueryController controller = controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new);
        controller.generatorDetailsEnabled = false;

        ResponseEntity<QueryResponseData> response = controller.executeStoredQuery(
                "my_qualified_query", "v1.0.0", null, null, null, MediaType.APPLICATION_JSON_VALUE);

        assertResponseMeta(response, expectedMetaData(expected -> {
            expected.setHref("https://openehr.test.com/rest/query/my_qualified_query/v1.0.0");
        }));
    }

    @Test
    void GETexecuteStoredQueryReturnMetaWithGenerator() {

        OpenehrQueryController controller = controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new);
        controller.generatorDetailsEnabled = true;

        ResponseEntity<QueryResponseData> response = controller.executeStoredQuery(
                "my_qualified_query", "v1.0.0", null, null, null, MediaType.APPLICATION_JSON_VALUE);

        assertResponseMeta(response, expectedMetaData(expected -> {
            expected.setHref("https://openehr.test.com/rest/query/my_qualified_query/v1.0.0");
            expected.setGenerator("EHRBase/"); // version is ignored for test
        }));
    }

    @Test
    void GETexecuteStoredQueryWithExecutionOptionsDisable() {

        OpenehrQueryController controller = controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new);
        assertFalse(controller.executionOptionsEnabled);

        ResponseEntity<QueryResponseData> response = controller.executeStoredQuery(
                "my_qualified_query", "v1.0.0", null, null, null, MediaType.APPLICATION_JSON_VALUE);

        MetaData metaData = responseMeta(response);
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.dryRun));
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.executedSQL));
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.queryPlan));
    }

    @Test
    void GETexecuteStoredQueryWithExecutionOptionsEnabled() {

        OpenehrQueryController controller = controllerStoredQueryWithExecutionOptions(
                "SELECT s FROM EHR_STATUS s", false, "SELECT FALSE", Map.of("stored", "query"));

        ResponseEntity<QueryResponseData> response = controller.executeStoredQuery(
                "my_qualified_query", "v1.0.0", null, null, null, MediaType.APPLICATION_JSON_VALUE);

        MetaData metaData = responseMeta(response);
        assertEquals(Boolean.FALSE, metaData.getAdditionalProperty(MetaData.AdditionalProperty.dryRun));
        assertEquals("SELECT FALSE", metaData.getAdditionalProperty(MetaData.AdditionalProperty.executedSQL));
        assertEquals(Map.of("stored", "query"), metaData.getAdditionalProperty(MetaData.AdditionalProperty.queryPlan));
    }

    @Test
    void POSTexecuteStoredQuery() {

        controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                .executeStoredQuery(
                        "my_qualified_query",
                        "v1.0.0",
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_JSON_VALUE,
                        null);
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), null, null, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteStoredQueryWithFetch() {

        controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                .executeStoredQuery(
                        "my_qualified_query",
                        "v1.0.0",
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_JSON_VALUE,
                        Map.of("fetch", 30));
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), 30L, null, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteStoredQueryWithFetchAsString() {

        controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                .executeStoredQuery(
                        "my_qualified_query",
                        "v1.0.0",
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_JSON_VALUE,
                        Map.of("fetch", "30"));
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), 30L, null, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteStoredQueryWithFetchInvalid() {

        String message = assertThrowsExactly(InvalidApiParameterException.class, () -> controllerStoredQuery(
                                "SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                        .executeStoredQuery(
                                "my_qualified_query",
                                "v1.0.0",
                                MediaType.APPLICATION_JSON_VALUE,
                                MediaType.APPLICATION_JSON_VALUE,
                                Map.of("fetch", "invalid")))
                .getMessage();
        assertEquals("invalid 'fetch' value 'invalid'", message);
    }

    @Test
    void POSTexecuteStoredQueryWithOffset() {

        controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                .executeStoredQuery(
                        "my_qualified_query",
                        "v1.0.0",
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_JSON_VALUE,
                        Map.of("offset", 15));
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), null, 15L, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteStoredQueryWithOffsetAsString() {

        controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                .executeStoredQuery(
                        "my_qualified_query",
                        "v1.0.0",
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_JSON_VALUE,
                        Map.of("offset", "15"));
        assertAqlQueryRequest(
                new AqlQueryRequest("SELECT s FROM EHR_STATUS s", Map.of(), null, 15L, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteStoredQueryWithOffsetInvalid() {

        String message = assertThrowsExactly(InvalidApiParameterException.class, () -> controllerStoredQuery(
                                "SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                        .executeStoredQuery(
                                "my_qualified_query",
                                "v1.0.0",
                                MediaType.APPLICATION_JSON_VALUE,
                                MediaType.APPLICATION_JSON_VALUE,
                                Map.of("offset", "invalid")))
                .getMessage();
        assertEquals("invalid 'offset' value 'invalid'", message);
    }

    @Test
    void POSTexecuteStoredQueryWithParameter() {

        controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new)
                .executeStoredQuery(
                        "my_qualified_query",
                        "v1.0.0",
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_JSON_VALUE,
                        Map.of("query_parameters", Map.of("key", "value")));
        assertAqlQueryRequest(new AqlQueryRequest(
                "SELECT s FROM EHR_STATUS s", Map.of("key", "value"), null, null, AqlExecutionOption.None));
    }

    @Test
    void POSTexecuteStoredQueryReturnMeta() {

        OpenehrQueryController controller = controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new);
        controller.generatorDetailsEnabled = false;

        ResponseEntity<QueryResponseData> response = controller.executeStoredQuery(
                "my_qualified_query",
                "v1.0.0",
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                Map.of());

        assertResponseMeta(response, expectedMetaData(expected -> {
            expected.setHref(null); // no _href for POST results
        }));
    }

    @Test
    void POSTexecuteStoredQueryReturnMetaWithGenerator() {

        OpenehrQueryController controller = controllerStoredQuery("SELECT s FROM EHR_STATUS s", QueryResultDto::new);
        controller.generatorDetailsEnabled = true;

        ResponseEntity<QueryResponseData> response = controller.executeStoredQuery(
                "my_qualified_query",
                "v1.0.0",
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                Map.of());

        assertResponseMeta(response, expectedMetaData(expected -> {
            expected.setHref(null); // no _href for POST results
            expected.setGenerator("EHRBase/"); // version is ignored for test
        }));
    }

    @Test
    void POSTexecuteStoredQueryWithExecutionOptionsDisable() {

        OpenehrQueryController controller = controllerStoredQuery("SELECT c FROM CONTRIBUTION c", QueryResultDto::new);
        assertFalse(controller.executionOptionsEnabled);

        ResponseEntity<QueryResponseData> response = controller.executeStoredQuery(
                "my_qualified_query",
                "v1.0.0",
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                Map.of());

        MetaData metaData = responseMeta(response);
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.dryRun));
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.executedSQL));
        assertNull(metaData.getAdditionalProperty(MetaData.AdditionalProperty.queryPlan));
    }

    @Test
    void POSTexecuteStoredQueryWithExecutionOptionsEnabled() {

        OpenehrQueryController controller = controllerStoredQueryWithExecutionOptions(
                "SELECT c FROM CONTRIBUTION c", false, null, Map.of("stored", "query"));

        ResponseEntity<QueryResponseData> response = controller.executeStoredQuery(
                "my_qualified_query",
                "v1.0.0",
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                Map.of());

        MetaData metaData = responseMeta(response);
        assertEquals(Boolean.FALSE, metaData.getAdditionalProperty(MetaData.AdditionalProperty.dryRun));
        assertEquals(null, metaData.getAdditionalProperty(MetaData.AdditionalProperty.executedSQL));
        assertEquals(Map.of("stored", "query"), metaData.getAdditionalProperty(MetaData.AdditionalProperty.queryPlan));
    }

    private void assertAqlQueryRequest(AqlQueryRequest aqlQueryRequest) {
        ArgumentCaptor<AqlQueryRequest> argument = ArgumentCaptor.forClass(AqlQueryRequest.class);
        verify(mockAqlQueryService).query(argument.capture());
        assertEquals(aqlQueryRequest, argument.getValue());
    }

    private static MetaData responseMeta(ResponseEntity<QueryResponseData> response) {
        QueryResponseData responseData = response.getBody();
        assertNotNull(responseData);

        return responseData.getMeta();
    }

    private static void assertResponseMeta(ResponseEntity<QueryResponseData> response, MetaData expected) {

        MetaData metaData = responseMeta(response);

        assertEquals(expected.getHref(), metaData.getHref(), "_href does not match");
        assertEquals(expected.getType(), metaData.getType(), "_type does not match");
        assertEquals(expected.getSchemaVersion(), metaData.getSchemaVersion(), "_schema_version does not match");
        assertEquals(expected.getExecutedAql(), metaData.getExecutedAql(), "_executed_aql does not match");
        assertTrue(expected.getCreated().equals(metaData.getCreated())
                || expected.getCreated().isAfter(metaData.getCreated()));

        String generator = expected.getGenerator();
        if (generator != null) {
            assertTrue(
                    metaData.getGenerator().startsWith(generator),
                    "_generator does not start with %s".formatted(generator));
        } else {
            assertNull(metaData.getGenerator(), "_generator is not null");
        }

        assertEquals(
                expected.getAdditionalProperty(MetaData.AdditionalProperty.resultSize),
                metaData.getAdditionalProperty(MetaData.AdditionalProperty.resultSize));
        assertEquals(
                expected.getAdditionalProperty(MetaData.AdditionalProperty.fetch),
                metaData.getAdditionalProperty(MetaData.AdditionalProperty.fetch));
        assertEquals(
                expected.getAdditionalProperty(MetaData.AdditionalProperty.offset),
                metaData.getAdditionalProperty(MetaData.AdditionalProperty.offset));
    }
}
