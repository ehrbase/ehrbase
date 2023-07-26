/*
 * Copyright (c) 2021-2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.application.config.validation;

import com.nedap.archie.rm.datavalues.DvCodedText;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.http.client.HttpClient;
import org.ehrbase.openehr.sdk.util.functional.Try;
import org.ehrbase.openehr.sdk.validation.ConstraintViolation;
import org.ehrbase.openehr.sdk.validation.ConstraintViolationException;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidationChain;
import org.ehrbase.openehr.sdk.validation.terminology.FhirTerminologyValidation;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link Configuration} for external terminology validation.
 *
 * @author Renaud Subiger
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(ValidationProperties.class)
@SuppressWarnings("java:S6212")
public class ValidationConfiguration {
    private static final String ERR_MSG = "External terminology validation is disabled, consider to enable it";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ValidationProperties properties;
    private final HttpClient httpClient;

    //  @Autowired
    @Value("${validation.external-terminology.enabled}")
    private Boolean enableExternalValidation;

    public ValidationConfiguration(ValidationProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Bean
    public ExternalTerminologyValidation externalTerminologyValidator() {
        if (!enableExternalValidation) {
            logger.warn(ERR_MSG);
            return new ExternalTerminologyValidation() {
                private final ConstraintViolation err = new ConstraintViolation(ERR_MSG);

                public Try<Boolean, ConstraintViolationException> validate(TerminologyParam param) {
                    return Try.failure(new ConstraintViolationException(List.of(err)));
                }

                public boolean supports(TerminologyParam param) {
                    return false;
                }

                public List<DvCodedText> expand(TerminologyParam param) {
                    return Collections.emptyList();
                }
            };
        }

        Map<String, ValidationProperties.Provider> providers = properties.getProvider();

        if (providers.isEmpty()) {
            throw new IllegalStateException("At least one external terminology provider must be defined "
                    + "if 'validation.external-validation.enabled' is set to 'true'");
        } else if (providers.size() == 1) {
            Map.Entry<String, ValidationProperties.Provider> provider =
                    providers.entrySet().iterator().next();
            return buildExternalTerminologyValidation(provider);
        } else {
            ExternalTerminologyValidationChain chain = new ExternalTerminologyValidationChain();
            for (Map.Entry<String, ValidationProperties.Provider> provider : providers.entrySet()) {
                chain.addExternalTerminologyValidationSupport(buildExternalTerminologyValidation(provider));
            }
            return chain;
        }
    }

    private ExternalTerminologyValidation buildExternalTerminologyValidation(
            Map.Entry<String, ValidationProperties.Provider> provider) {
        logger.info(
                "Initializing '{}' external terminology provider (type: {})",
                provider.getKey(),
                provider.getValue().getType());
        if (provider.getValue().getType() == ValidationProperties.ProviderType.FHIR) {
            return fhirTerminologyValidation(provider.getValue().getUrl());
        }
        throw new IllegalArgumentException(
                "Invalid provider type: " + provider.getValue().getType());
    }

    private FhirTerminologyValidation fhirTerminologyValidation(String url) {
        return new FhirTerminologyValidation(url, properties.isFailOnError(), httpClient);
    }
}
