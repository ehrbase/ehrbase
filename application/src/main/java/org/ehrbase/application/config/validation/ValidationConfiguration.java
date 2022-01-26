/*
 * Copyright (c) 2021 Vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.application.config.validation;

import java.util.Map;
import org.apache.http.client.HttpClient;
import org.ehrbase.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.validation.terminology.ExternalTerminologyValidationChain;
import org.ehrbase.validation.terminology.FhirTerminologyValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link Configuration} for external terminology validation.
 */
@Configuration
@ConditionalOnProperty(name = "validation.external-terminology.enabled", havingValue = "true")
@EnableConfigurationProperties(ValidationProperties.class)
@SuppressWarnings("java:S6212")
public class ValidationConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(ValidationConfiguration.class);

  private final ValidationProperties properties;

  private final HttpClient httpClient;

  public ValidationConfiguration(ValidationProperties properties, HttpClient httpClient) {
    this.properties = properties;
    this.httpClient = httpClient;
  }

  @Bean
  public ExternalTerminologyValidation externalTerminologyValidator() {
    Map<String, ValidationProperties.Provider> providers = properties.getProvider();

    if (providers.isEmpty()) {
      throw new IllegalStateException(
          "At least one external terminology provider must be defined " +
              "if 'validation.external-validation.enabled' is set to 'true'");
    } else if (providers.size() == 1) {
      Map.Entry<String, ValidationProperties.Provider> provider = providers.entrySet().iterator()
          .next();
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
    LOG.info("Initializing '{}' external terminology provider (type: {})", provider.getKey(),
        provider.getValue().getType());
    if (provider.getValue().getType() == ValidationProperties.ProviderType.FHIR) {
      return fhirTerminologyValidation(provider.getValue().getUrl());
    }
    throw new IllegalArgumentException("Invalid provider type: " + provider.getValue().getType());
  }

  private FhirTerminologyValidation fhirTerminologyValidation(String url) {
    return new FhirTerminologyValidation(url, properties.isFailOnError(), httpClient);
  }
}
