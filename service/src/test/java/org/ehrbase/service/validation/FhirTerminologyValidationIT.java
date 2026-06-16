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
package org.ehrbase.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.ehrbase.openehr.sdk.validation.ConstraintViolation;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidationException;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;
import org.ehrbase.service.validation.ExternalTerminologyProviderProperties.ProviderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Integration tests for {@link FhirTerminologyValidation} using a MockWebServer
 * to simulate a FHIR terminology server over HTTP without any external dependencies.
 */
class FhirTerminologyValidationIT {

    // --- FHIR JSON response templates ---

    private static final String FHIR_BUNDLE_TOTAL_1 = """
        {
          "resourceType": "Bundle",
          "total": 1
        }
        """;

    private static final String FHIR_BUNDLE_TOTAL_0 = """
        {
          "resourceType": "Bundle",
          "total": 0
        }
        """;

    private static final String FHIR_PARAMETERS_VALID = """
        {
          "resourceType": "Parameters",
          "parameter": [
            { "name": "result", "valueBoolean": true }
          ]
        }
        """;

    private static final String FHIR_PARAMETERS_INVALID = """
        {
          "resourceType": "Parameters",
          "parameter": [
            { "name": "result", "valueBoolean": false },
            { "name": "message", "valueString": "Unknown code 'final' in CodeSystem" }
          ]
        }
        """;

    private static final String FHIR_PARAMETERS_INVALID_MULTI_MESSAGE = """
        {
          "resourceType": "Parameters",
          "parameter": [
            { "name": "result", "valueBoolean": false },
            { "name": "message", "valueString": "First error" },
            { "name": "message", "valueString": "Second error" }
          ]
        }
        """;

    // --- Test infrastructure ---

    private MockWebServer mockWebServer;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString();
        // Remove trailing slash – ExternalTerminologyProviderProperties.getUrl() is used as base URL
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // -------------------------------------------------------------------------
    // supports() – ValueSet
    // -------------------------------------------------------------------------

    @Test
    void supports_ValueSet_ServerReturnsTotal1_ReturnsTrue() {
        mockWebServer.enqueue(fhirJsonResponse(FHIR_BUNDLE_TOTAL_1));

        FhirTerminologyValidation validation = buildValidation(true);

        assertTrue(validation.supports(valueSetParam()));

        RecordedRequest request = takeRequest();
        assertThat(request.getPath())
                .startsWith("/ValueSet?")
                .contains("_summary=true")
                .contains("url=");

        assertFhirHeader(request);
    }

    private static void assertFhirHeader(RecordedRequest request) {
        assertThat(request.getHeader(com.google.common.net.HttpHeaders.ACCEPT))
                .isEqualTo(FhirTerminologyValidation.FHIR_ACCEPT_HEADER);
    }

    @Test
    void supports_ValueSet_ServerReturnsTotal0_ReturnsFalse() {
        mockWebServer.enqueue(fhirJsonResponse(FHIR_BUNDLE_TOTAL_0));

        FhirTerminologyValidation validation = buildValidation(true);

        assertFalse(validation.supports(valueSetParam()));
    }

    @Test
    void supports_CodeSystem_ServerReturnsTotal1_ReturnsTrue() {
        mockWebServer.enqueue(fhirJsonResponse(FHIR_BUNDLE_TOTAL_1));

        FhirTerminologyValidation validation = buildValidation(true);

        assertTrue(validation.supports(codeSystemParam()));

        RecordedRequest request = takeRequest();
        assertThat(request.getPath()).startsWith("/CodeSystem?").contains("_summary=true");
    }

    @Test
    void supports_UnsupportedServiceApi_ReturnsFalseWithoutHttpCall() {
        FhirTerminologyValidation validation = buildValidation(true);
        TerminologyParam param =
                TerminologyParam.ofFhir("//invalid.api/ValueSet/$expand?url=http://snomed.info/sct", null);

        assertFalse(validation.supports(param));
        // No HTTP request should have been made
        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    void supports_MissingUrlParam_ReturnsFalseWithoutHttpCall() {
        FhirTerminologyValidation validation = buildValidation(true);
        TerminologyParam param = TerminologyParam.ofFhir("//fhir.hl7.org/ValueSet/$expand", null);

        assertFalse(validation.supports(param));
        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 408, 429, 500, 502, 504})
    void supports_NonSuccessHttpStatusCodes_FailOnError_ThrowsException(int errorCode) {
        enqueueErrors(errorCode, errorCode);
        FhirTerminologyValidation validation = buildValidation(true);
        TerminologyParam param = valueSetParam();
        assertThatThrownBy(() -> validation.supports(param)).isInstanceOf(ExternalTerminologyValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 408, 429, 500, 502, 504})
    void supports_NonSuccessHttpStatusCodes_FailOnErrorFalse_ReturnsFalse(int errorCode) {
        enqueueErrors(errorCode, errorCode);
        FhirTerminologyValidation validation = buildValidation(false);
        assertFalse(validation.supports(valueSetParam()));
    }

    // -------------------------------------------------------------------------
    // validate() – CodeSystem
    // -------------------------------------------------------------------------

    @Test
    void validate_CodeSystem_ValidCode_ReturnsNull() {
        mockWebServer.enqueue(fhirJsonResponse(FHIR_PARAMETERS_VALID));

        FhirTerminologyValidation validation = buildValidation(true);

        ConstraintViolation result = validation.validate(codeSystemParam());

        assertNull(result);

        RecordedRequest request = takeRequest();
        assertThat(request.getPath())
                .startsWith("/CodeSystem/$validate-code")
                .contains("url=")
                .contains("code=final");

        assertFhirHeader(request);
    }

    @Test
    void validate_CodeSystem_InvalidCode_ReturnsConstraintViolation() {
        mockWebServer.enqueue(fhirJsonResponse(FHIR_PARAMETERS_INVALID));

        FhirTerminologyValidation validation = buildValidation(true);

        ConstraintViolation result = validation.validate(codeSystemParam());

        assertThat(result).isNotNull();
        assertThat(result.getMessage()).contains("Unknown code 'final' in CodeSystem");
    }

    @Test
    void validate_CodeSystem_MultipleMessages_JoinsWithSemicolon() {
        mockWebServer.enqueue(fhirJsonResponse(FHIR_PARAMETERS_INVALID_MULTI_MESSAGE));

        FhirTerminologyValidation validation = buildValidation(true);

        ConstraintViolation result = validation.validate(codeSystemParam());

        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("First error; Second error");
    }

    @Test
    void validate_MissingUrlParam_ReturnsConstraintViolationWithMessage() {
        FhirTerminologyValidation validation = buildValidation(true);
        // No "url=" in the serviceApi query string → extractUrl returns null
        TerminologyParam param = TerminologyParam.ofFhir(
                "//fhir.hl7.org/CodeSystem/$validate-code?code=final",
                new CodePhrase(new TerminologyId("http://hl7.org/fhir/observation-status"), "final"));

        ConstraintViolation result = validation.validate(param);

        assertThat(result).isNotNull();
        assertThat(result.getMessage()).contains("Missing value-set url");
        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    // -------------------------------------------------------------------------
    // validate() – ValueSet
    // -------------------------------------------------------------------------

    @Test
    void validate_ValueSet_ValidCode_ReturnsNull() {
        mockWebServer.enqueue(fhirJsonResponse(FHIR_PARAMETERS_VALID));

        FhirTerminologyValidation validation = buildValidation(true);

        ConstraintViolation result = validation.validate(valueSetParam());

        assertNull(result);

        RecordedRequest request = takeRequest();
        assertThat(request.getPath())
                .startsWith("/ValueSet/$validate-code")
                .contains("url=")
                .contains("code=final")
                .contains("system=");
    }

    @Test
    void validate_ValueSet_InvalidCode_ReturnsConstraintViolation() {
        mockWebServer.enqueue(fhirJsonResponse(FHIR_PARAMETERS_INVALID));

        FhirTerminologyValidation validation = buildValidation(true);

        ConstraintViolation result = validation.validate(valueSetParam());

        assertThat(result).isNotNull();
        assertThat(result.getMessage()).contains("Unknown code 'final' in CodeSystem");
    }

    // -------------------------------------------------------------------------
    // test retries
    // -------------------------------------------------------------------------

    @Test
    void validate_retry() {
        enqueueErrors(502);
        mockWebServer.enqueue(fhirJsonResponse(FHIR_PARAMETERS_VALID));

        FhirTerminologyValidation validation = buildValidation(true);

        assertNull(validation.validate(codeSystemParam()));
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void supports_retry() {
        enqueueErrors(502);
        mockWebServer.enqueue(fhirJsonResponse(FHIR_BUNDLE_TOTAL_1));

        FhirTerminologyValidation validation = buildValidation(true);

        assertTrue(validation.supports(codeSystemParam()));
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // validate() – error handling
    // -------------------------------------------------------------------------

    @Test
    void validate_ServerError_FailOnError_ThrowsException() {
        enqueueErrors(500, 500);

        FhirTerminologyValidation validation = buildValidation(true);
        TerminologyParam param = codeSystemParam();

        assertThatThrownBy(() -> validation.validate(param)).isInstanceOf(ExternalTerminologyValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 408, 429, 500, 502, 504})
    void validate_NonSuccessHttpStatusCodes_FailOnErrorFalse_ReturnsNull(int errorCode) {
        enqueueErrors(errorCode, errorCode);
        FhirTerminologyValidation validation = buildValidation(false);

        assertNull(validation.validate(codeSystemParam()));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 408, 429, 500, 502, 504})
    void validate_NonSuccessHttpStatusCodes(int errorCode) {
        enqueueErrors(errorCode, errorCode);
        FhirTerminologyValidation validation = buildValidation(true);
        TerminologyParam param = codeSystemParam();

        assertThatThrownBy(() -> validation.validate(param)).isInstanceOf(ExternalTerminologyValidationException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FhirTerminologyValidation buildValidation(boolean failOnError) {
        ExternalTerminologyProviderProperties props =
                new ExternalTerminologyProviderProperties(ProviderType.FHIR, baseUrl, false);
        props.setResponseTimeoutSeconds(5);
        props.getRetry().setInitialBackoffMillis(2);
        props.getRetry().setAttempts(1);
        // Use a plain WebClient; the constructor will mutate it with the base URL and headers
        return new FhirTerminologyValidation(props, failOnError, WebClient.create());
    }

    private MockResponse fhirJsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }

    private MockResponse errorResponse(int statusCode) {
        return new MockResponse()
                .setResponseCode(statusCode)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"resourceType\":\"OperationOutcome\"}");
    }

    private static TerminologyParam valueSetParam() {
        String valueSetUrl = "http://hl7.org/fhir/ValueSet/observation-status";
        return TerminologyParam.ofFhir(
                "//fhir.hl7.org/ValueSet/$expand?url=" + valueSetUrl,
                new CodePhrase(new TerminologyId("http://hl7.org/fhir/observation-status"), "final"));
    }

    private static TerminologyParam codeSystemParam() {
        return TerminologyParam.ofFhir(
                "//fhir.hl7.org/CodeSystem?url=http://hl7.org/fhir/observation-status",
                new CodePhrase(new TerminologyId("http://hl7.org/fhir/observation-status"), "final"));
    }

    private void enqueueErrors(int... statusCodes) {
        for (int statusCode : statusCodes) {
            mockWebServer.enqueue(errorResponse(statusCode));
        }
    }

    private RecordedRequest takeRequest() {
        try {
            return mockWebServer.takeRequest(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
