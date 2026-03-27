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
package org.ehrbase.configuration.config.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.ehrbase.api.exception.BadGatewayException;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.web.reactive.function.client.WebClientException;

class ValidationConfigurationTest {

    @Mock private CacheProvider cacheProvider;
    @Mock private Cache cache;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Configure the cache provider to return our cache mock for the external FHIR terminology cache
        when(cacheProvider.getCache(eq(CacheProvider.EXTERNAL_FHIR_TERMINOLOGY_CACHE))).thenReturn(cache);
    }

    private ExternalTerminologyValidation buildValidator(boolean failOnError) {
        ExternalValidationProperties properties = new ExternalValidationProperties();
        properties.setEnabled(true);
        properties.setFailOnError(failOnError);

        ExternalValidationProperties.Provider provider = new ExternalValidationProperties.Provider();
        provider.setType(ExternalValidationProperties.ProviderType.FHIR);
        provider.setUrl("https://unit.test/fhir");
        properties.getProvider().put("default", provider);

        // When the cache is asked to load a value, simulate a downstream client failure wrapped by the cache
    when(cache.get(any(), org.mockito.ArgumentMatchers.<Callable<DocumentContext>>any()))
        .thenAnswer(invocation -> {
            Object key = invocation.getArgument(0);
            Callable<DocumentContext> loader = invocation.getArgument(1);
            throw new Cache.ValueRetrievalException(
                key, loader, new WebClientException("downstream error") {});
        });

        ValidationConfiguration cfg = new ValidationConfiguration(properties, cacheProvider, Optional.empty());
        return cfg.externalTerminologyValidator();
    }

    @Test
    void respectsFailOnErrorFalse_returnsFalseNoException() {
        ExternalTerminologyValidation validator = buildValidator(false);
        // Build a FHIR service-api param that passes the initial checks inside FhirTerminologyValidation
        TerminologyParam tp = TerminologyParam.ofFhir("//fhir.hl7.org/ValueSet?url=http://example.org/vs");
        boolean supported = validator.supports(tp);
        assertFalse(supported, "supports() should return false (no exception) when failOnError=false");
    }

    @Test
    void respectsFailOnErrorTrue_throwsBadGateway() {
        ExternalTerminologyValidation validator = buildValidator(true);
        TerminologyParam tp = TerminologyParam.ofFhir("//fhir.hl7.org/ValueSet?url=http://example.org/vs");
        assertThrows(BadGatewayException.class, () -> validator.supports(tp));
    }
}
