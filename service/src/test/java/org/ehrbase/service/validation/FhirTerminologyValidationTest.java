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
package org.ehrbase.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.internal.JsonContext;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.net.URI;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidationException;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;
import org.ehrbase.service.validation.ExternalTerminologyProviderProperties.ProviderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

class FhirTerminologyValidationTest {

    @Test
    void supports_ValidParams_ReturnsTrue() {
        String baseUrl = "http://terminology.local";
        String valueSetUrl = "http://snomed.info/sct?fhir_vs=ecl/%3C306206005";

        FhirTerminologyValidation validation = spy(new FhirTerminologyValidation(
                new ExternalTerminologyProviderProperties(ProviderType.FHIR, baseUrl, false),
                true,
                WebClient.create()));
        TerminologyParam param = TerminologyParam.ofFhir(
                "//fhir.hl7.org/ValueSet/$expand?url=" + valueSetUrl + "&activeOnly=true", null);

        DocumentContext documentContext = mock(DocumentContext.class);
        when(documentContext.read(FhirTerminologyValidation.SUPPORTS_TOTAL_JSON_PATH, int.class))
                .thenReturn(1);

        doReturn(documentContext).when(validation).internalGet(Mockito.anyString());

        assertTrue(validation.supports(param));
        verify(validation)
                .internalGet(FhirTerminologyValidation.SUPPORTS_VALUE_SET_TEMPL.formatted(param.getParam("url")));
    }

    @Test
    void supports_InvalidParams_ReturnsFalse() {
        FhirTerminologyValidation validation = spy(new FhirTerminologyValidation(
                new ExternalTerminologyProviderProperties(ProviderType.FHIR, "http://terminology.local", false),
                true,
                WebClient.create()));

        assertFalse(
                validation.supports(TerminologyParam.ofFhir("//fhir.hl7.org/ValueSet/$expand?activeOnly=true", null)));
        assertFalse(validation.supports(TerminologyParam.ofFhir(
                "//invalid/ValueSet/$expand?url=http://snomed.info/sct?fhir_vs=ecl/%3C306206005&activeOnly=true",
                null)));

        verify(validation, never()).internalGet(Mockito.anyString());
    }

    @Test
    void normalizeBaseUrl() {
        assertThat(FhirTerminologyValidation.normalizeBaseUrl("")).isEmpty();
        assertThat(FhirTerminologyValidation.normalizeBaseUrl("http://localhost/fhir/"))
                .isEqualTo("http://localhost/fhir");
        assertThat(FhirTerminologyValidation.normalizeBaseUrl("http://localhost/fhir"))
                .isEqualTo("http://localhost/fhir");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testFailOnError(boolean failOnError) {
        String baseUrl = "http://terminology.local";
        String valueSetUrl = "http://snomed.info/sct?fhir_vs=ecl/%3C306206005";

        var codePhrase = new CodePhrase(new TerminologyId("http://hl7.org/fhir/observation-status"), "B");

        FhirTerminologyValidation validation = spy(new FhirTerminologyValidation(
                new ExternalTerminologyProviderProperties(ProviderType.FHIR, baseUrl, false),
                failOnError,
                WebClient.create()));
        TerminologyParam param = TerminologyParam.ofFhir(
                "//fhir.hl7.org/ValueSet/$expand?url=" + valueSetUrl + "&activeOnly=true", codePhrase);

        JsonContext jsonContext = mock(JsonContext.class);
        when(jsonContext.read(FhirTerminologyValidation.SUPPORTS_TOTAL_JSON_PATH, int.class))
                .thenReturn(1);

        doThrow(new WebClientRequestException(
                        new RuntimeException(""), HttpMethod.GET, URI.create(""), HttpHeaders.EMPTY))
                .when(validation)
                .internalGet(Mockito.anyString());

        if (failOnError) {
            assertThatThrownBy(() -> validation.validate(param))
                    .isInstanceOf(ExternalTerminologyValidationException.class);
        } else {
            assertThat(validation.validate(param)).isNull();
        }
    }
}
