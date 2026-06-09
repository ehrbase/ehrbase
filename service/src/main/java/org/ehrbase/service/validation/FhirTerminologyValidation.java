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

import com.google.common.net.HttpHeaders;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.nedap.archie.rm.datatypes.CodePhrase;
import io.netty.handler.timeout.TimeoutException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.net.ssl.SSLException;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.openehr.sdk.validation.ConstraintViolation;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidationException;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

/**
 * {@link ExternalTerminologyValidation} that supports FHIR terminology validation.
 */
public class FhirTerminologyValidation implements ExternalTerminologyValidation {

    static final String SUPPORTS_CODE_SYS_TEMPL = "/CodeSystem?_summary=true&url=%s";
    static final String SUPPORTS_VALUE_SET_TEMPL = "/ValueSet?_summary=true&url=%s";
    private static final String VALUESET_VALIDATE_URL_TPL = "/ValueSet/$validate-code?url=%s&code=%s&system=%s";
    private static final String CODESYSTEM_VALIDATE_URL_TPL = "/CodeSystem/$validate-code?url=%s&code=%s";

    public static final JsonPath VALIDATE_CODE_RESULT_JSON_PATH =
            JsonPath.compile("$.parameter[?(@.name==\"result\" && @.valueBoolean == true)].valueBoolean");
    public static final JsonPath VALIDATE_CODE_MESSAGE_JSON_PATH =
            JsonPath.compile("$.parameter[?(@.name==\"message\")].valueString");
    public static final JsonPath SUPPORTS_TOTAL_JSON_PATH = JsonPath.compile("$.total");

    private static final String ERR_SUPPORTS =
            "An error occurred while checking if FHIR terminology server supports the referenceSetUri: %s";

    public static final String FHIR_ACCEPT_HEADER = "application/fhir+json";

    private static final String[] ACCEPTED_FHIR_APIS = {"//fhir.hl7.org", "terminology://fhir.hl7.org", "//hl7.org/fhir"
    };

    private static final Logger LOG = LoggerFactory.getLogger(FhirTerminologyValidation.class);

    private final boolean failOnError;
    private final WebClient webClient;
    private final Retry retry;

    public FhirTerminologyValidation(
            ExternalTerminologyProviderProperties provider, boolean failOnError, WebClient webClient) {
        this.failOnError = failOnError;
        this.retry = Retry.backoff(
                        provider.getRetry().getAttempts(),
                        Duration.ofMillis(provider.getRetry().getInitialBackoffMillis()))
                .filter(FhirTerminologyValidation::mustRetry);
        ConnectionProvider httpConnectionProvider = ConnectionProvider.builder(provider.getUrl())
                .maxConnections(provider.getMaxConnections())
                .pendingAcquireMaxCount(provider.getMaxPendingConnections())
                .maxIdleTime(Duration.ofSeconds(provider.getMaxConnectionIdleTimeSeconds()))
                .maxLifeTime(Duration.ofMinutes(provider.getMaxConnectionLifeTimeSeconds()))
                .build();
        HttpClient httpClient = HttpClient.create(httpConnectionProvider)
                .metrics(provider.isEnableMetrics(), Function.identity())
                .responseTimeout(Duration.ofSeconds(provider.getResponseTimeoutSeconds()));
        this.webClient = webClient
                .mutate()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, FHIR_ACCEPT_HEADER)
                .baseUrl(normalizeBaseUrl(provider.getUrl()))
                .build();
    }

    static String normalizeBaseUrl(String url) {
        if (StringUtils.isEmpty(url) || url.charAt(url.length() - 1) != '/') {
            return url;
        }
        return url.substring(0, url.length() - 1);
    }

    private static boolean mustRetry(Throwable throwable) {
        return switch (throwable) {
            case WebClientResponseException wcre -> mustRetry(wcre.getStatusCode());
            case WebClientException _, TimeoutException _, ConnectException _, SSLException _ -> true;
            case null, default -> false;
        };
    }

    private static boolean mustRetry(HttpStatusCode statusCode) {
        return switch (statusCode) {
            case HttpStatus.BAD_GATEWAY,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    HttpStatus.GATEWAY_TIMEOUT,
                    HttpStatus.REQUEST_TIMEOUT,
                    HttpStatus.TOO_MANY_REQUESTS,
                    HttpStatus.NOT_FOUND -> true;
            default -> false;
        };
    }

    static String extractUrl(String queryParamsString) {
        if (queryParamsString == null) {
            return null;
        }

        UriComponents uriComponents = UriComponentsBuilder.fromUriString("/?%s".formatted(queryParamsString))
                .build();
        MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
        return queryParams.getFirst("url");
    }

    protected DocumentContext internalGet(String uri)
            throws WebClientException, ExternalTerminologyValidationException {
        // timeout is managed by web client
        try {
            return getAsMono(uri)
                    .map(FhirTerminologyValidation::toJsonDocument)
                    .blockOptional()
                    .orElseThrow(() -> new InternalServerException("could not connect to external Terminology Server"));
        } catch (RuntimeException e) {
            // unwrap WebClientException
            if (Exceptions.isRetryExhausted(e) && e.getCause() instanceof WebClientException wce) {
                throw wce;
            } else {
                throw e;
            }
        }
    }

    private static DocumentContext toJsonDocument(ResponseEntity<String> respEntity) {
        if (respEntity == null) {
            return null;
        }

        HttpStatusCode statusCode = respEntity.getStatusCode();
        if (statusCode.is2xxSuccessful()) {
            return JsonPath.parse(respEntity.getBody());
        } else {
            throw new ExternalTerminologyValidationException(
                    "Error response received from the terminology server. HTTP status: %s. Body: %s"
                            .formatted(statusCode, respEntity.getBody()));
        }
    }

    private Mono<ResponseEntity<String>> getAsMono(String uri) {
        return webClient.get().uri(uri).retrieve().toEntity(String.class).retryWhen(retry);
    }

    private boolean isValidTerminology(String url) {
        boolean valid = url != null && Arrays.stream(ACCEPTED_FHIR_APIS).anyMatch(url::startsWith);
        if (!valid) {
            LOG.warn("Unsupported service-api: {}", url);
        }
        return valid;
    }

    @Override
    public boolean supports(TerminologyParam param) {
        Optional<TerminologyParam> paramOptional = Optional.ofNullable(param);
        return paramOptional
                        .map(TerminologyParam::serviceApi)
                        .filter(this::isValidTerminology)
                        .isPresent()
                && paramOptional
                        .map(TerminologyParam::parameter)
                        .map(FhirTerminologyValidation::extractUrl)
                        .map(urlParam -> switch (param.resouceType()) {
                            case VALUE_SET -> SUPPORTS_VALUE_SET_TEMPL.formatted(urlParam);
                            case CODE_SYSTEM -> SUPPORTS_CODE_SYS_TEMPL.formatted(urlParam);
                        })
                        .map(url -> {
                            try {
                                return internalGet(url);
                            } catch (WebClientException e) {
                                if (failOnError) {
                                    throw new ExternalTerminologyValidationException(ERR_SUPPORTS.formatted(url), e);
                                }
                                LOG.warn("The following error occurred: {}", e.getMessage());
                                return null;
                            }
                        })
                        .map(doc -> doc.read(SUPPORTS_TOTAL_JSON_PATH, int.class))
                        .filter(t -> t > 0)
                        .isPresent();
    }

    @Override
    public ConstraintViolation validate(TerminologyParam param) {
        String url = extractUrl(param.parameter());
        if (url == null) {
            return new ConstraintViolation("Missing value-set url");
        }

        return validateCode(url, param.codePhrase(), param.resouceType());
    }

    private ConstraintViolation validateCode(
            String fhirTerminologyUri, CodePhrase codePhrase, TerminologyParam.ResouceType resouceType) {
        String code = codePhrase.getCodeString();

        String url =
                switch (resouceType) {
                    case VALUE_SET -> {
                        String system = codePhrase.getTerminologyId().getValue();
                        yield VALUESET_VALIDATE_URL_TPL.formatted(fhirTerminologyUri, code, system);
                    }
                    case CODE_SYSTEM -> CODESYSTEM_VALIDATE_URL_TPL.formatted(fhirTerminologyUri, code);
                };

        DocumentContext context;
        try {
            context = internalGet(url);
        } catch (WebClientException e) {
            if (failOnError) {
                throw new ExternalTerminologyValidationException("An error occurred during $validate-code request", e);
            }
            LOG.warn("An error occurred while validating the code in CodeSystem: {}", e.getMessage());
            return null;
        }

        boolean noResult = context.<List<?>>read(VALIDATE_CODE_RESULT_JSON_PATH).isEmpty();
        if (noResult) {
            List<String> message = context.read(VALIDATE_CODE_MESSAGE_JSON_PATH);
            return new ConstraintViolation(String.join("; ", message));
        }

        return null;
    }
}
