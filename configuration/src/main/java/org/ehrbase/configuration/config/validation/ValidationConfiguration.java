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

import com.jayway.jsonpath.DocumentContext;
import java.util.Map;
import java.util.Optional;
import org.ehrbase.api.exception.BadGatewayException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidationChain;
import org.ehrbase.service.validation.FhirTerminologyValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

/**
 * {@link Configuration} for external terminology validation.
 */
@Configuration
@EnableConfigurationProperties(ExternalValidationProperties.class)
@SuppressWarnings("java:S6212")
public class ValidationConfiguration {

    private static final String ERR_MSG = "External terminology validation is disabled, consider to enable it";
    private final Logger logger = LoggerFactory.getLogger(ValidationConfiguration.class);
    private final ExternalValidationProperties properties;
    private final CacheProvider cacheProvider;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public ValidationConfiguration(
            ExternalValidationProperties properties,
            CacheProvider cacheProvider,
            @Nullable OAuth2AuthorizedClientManager authorizedClientManager) {
        this.properties = properties;
        this.cacheProvider = cacheProvider;
        this.authorizedClientManager = authorizedClientManager;
    }

    @Bean
    public ExternalTerminologyValidation externalTerminologyValidator() {
        if (!properties.isEnabled()) {
            logger.warn(ERR_MSG);
            return nopTerminologyValidation();
        }

        final Map<String, ExternalValidationProperties.Provider> providers = properties.getProvider();

        if (providers.isEmpty()) {
            throw new IllegalStateException("At least one external terminology provider must be defined "
                    + "if 'validation.external-validation.enabled' is set to 'true'");
        } else if (providers.size() == 1) {
            return buildExternalTerminologyValidation(
                    providers.entrySet().iterator().next());
        } else {
            ExternalTerminologyValidationChain chain = new ExternalTerminologyValidationChain();
            for (Map.Entry<String, ExternalValidationProperties.Provider> namedProvider : providers.entrySet()) {
                chain.addExternalTerminologyValidationSupport(buildExternalTerminologyValidation(namedProvider));
            }
            return chain;
        }
    }

    private ExternalTerminologyValidation buildExternalTerminologyValidation(
            Map.Entry<String, ExternalValidationProperties.Provider> namedProvider) {

        final String name = namedProvider.getKey();
        final ExternalValidationProperties.Provider provider = namedProvider.getValue();
        String oauth2Client = provider.getOauth2Client();

        logger.info(
                "Initializing '{}' external terminology provider (type: {}) at {} {}",
                name,
                provider.getType(),
                provider.getUrl(),
                Optional.ofNullable(oauth2Client)
                        .map(" secured by oauth2 client '%s'"::formatted)
                        .orElse(""));

        final WebClient webClient = buildWebClient(oauth2Client);

        if (provider.getType() == ExternalValidationProperties.ProviderType.FHIR) {
            return fhirTerminologyValidation(provider.getUrl(), webClient);
        }
        throw new IllegalArgumentException("Invalid provider type: " + provider.getType());
    }

    private WebClient buildWebClient(String clientId) {
        WebClient.Builder builder = WebClient.builder();
        if (clientId != null) {
            // sanity checks
            if (authorizedClientManager == null) {
                throw new IllegalArgumentException(
                        "Attempt to create an oauth2 client with id 'spring.security.oauth2.registration.%s' but no clients are registered."
                                .formatted(clientId));
            }
            ServletOAuth2AuthorizedClientExchangeFilterFunction filter =
                    new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
            filter.setDefaultClientRegistrationId(clientId);
            builder = builder.apply(filter.oauth2Configuration());
        }
        return builder.build();
    }

    public static ExternalTerminologyValidation nopTerminologyValidation() {
        return new NopExternalTerminologyValidation(ERR_MSG);
    }

    private FhirTerminologyValidation fhirTerminologyValidation(String url, WebClient webClient) {
        return new FhirTerminologyValidation(url, properties.isFailOnError(), webClient) {

            @Override
            protected DocumentContext internalGet(String uri) throws WebClientException {
                try {
                    return CacheProvider.EXTERNAL_FHIR_TERMINOLOGY_CACHE.get(
                            cacheProvider, uri, () -> super.internalGet(uri));
                } catch (Cache.ValueRetrievalException e) {
                    final Throwable cause = e.getCause();
                    // Something went wrong during downstream request - Forward as bad Gateway. We could also catch
                    // WebClientResponseException and add our own error message. The WebClientException happens also
                    // in case the connection is refused or the DNS lookup fails.
                    if (cause instanceof WebClientException) {
                        throw new BadGatewayException(cause.getMessage(), cause);
                    } else {
                        throw new InternalServerException(
                                "Failure during fhir terminology request: %s".formatted(cause.getMessage()), cause);
                    }
                }
            }
        };
    }
}
