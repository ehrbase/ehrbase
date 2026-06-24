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
import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.nedap.archie.rm.datatypes.CodePhrase;
import io.netty.handler.timeout.TimeoutException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.openehr.sdk.validation.ConstraintViolation;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidationException;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;
import org.ehrbase.service.validation.ExternalTerminologyProviderProperties.ValuesetValidationMode;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

/**
 * {@link ExternalTerminologyValidation} that supports FHIR terminology validation.
 */
public class FhirTerminologyValidation implements ExternalTerminologyValidation {

    private static final String SYSTEM_ATT = "system";

    static final String SUPPORTS_CODE_SYS_TEMPL = "/CodeSystem?_summary=true&url=%s";
    static final String SUPPORTS_VALUE_SET_TEMPL = "/ValueSet?_summary=true&url=%s";

    private static final String VALUESET_VALIDATE_CODE_SYSTEM_URL_TPL =
            "/ValueSet/$validate-code?url=%s&code=%s&system=%s";
    private static final String VALUESET_VALIDATE_CODING_URL_TPL = "/ValueSet/$validate-code?url=%s&coding=%s%%7C%s";
    private static final String VALUESET_VALIDATE_EXPAND_URL_TPL = "/ValueSet/$expand?url=%s&offset=%d";
    private static final String CODESYSTEM_VALIDATE_URL_TPL = "/CodeSystem/$validate-code?url=%s&code=%s";

    public static final JsonPath VALIDATE_CODE_RESULT_JSON_PATH =
            JsonPath.compile("$.parameter[?(@.name==\"result\" && @.valueBoolean == true)].valueBoolean");
    public static final JsonPath VALIDATE_CODE_MESSAGE_JSON_PATH =
            JsonPath.compile("$.parameter[?(@.name==\"message\")].valueString");
    public static final JsonPath SUPPORTS_TOTAL_JSON_PATH = JsonPath.compile("$.total");

    private static final int MAX_VALUESET_EXPAND_PAGES = 500_000 / 1_000;

    public static final String FHIR_ACCEPT_HEADER = "application/fhir+json";

    private static final String[] ACCEPTED_FHIR_APIS = {"//fhir.hl7.org", "terminology://fhir.hl7.org", "//hl7.org/fhir"
    };

    private static final Logger LOG = LoggerFactory.getLogger(FhirTerminologyValidation.class);
    private static final String URL_PARAM = "url";

    private final boolean failOnError;
    private final WebClient webClient;
    private final Retry retry;

    private final ValuesetValidationMode valuesetValidationMode;

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
        this.valuesetValidationMode = provider.getValuesetValidationMode();
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
                        .map(p -> p.getParam(URL_PARAM))
                        .map(urlParam -> switch (param.resouceType()) {
                            case VALUE_SET -> SUPPORTS_VALUE_SET_TEMPL.formatted(urlParam);
                            case CODE_SYSTEM -> SUPPORTS_CODE_SYS_TEMPL.formatted(urlParam);
                        })
                        .map(url -> {
                            try {
                                return internalGet(url);
                            } catch (WebClientException e) {
                                if (failOnError) {
                                    throw new ExternalTerminologyValidationException(
                                            "An error occurred while checking if FHIR terminology server supports the referenceSetUri: %s"
                                                    .formatted(url),
                                            e);
                                }
                                LOG.warn(
                                        "FHIR terminology server supports call failed for {}: {}", url, e.getMessage());
                                return null;
                            }
                        })
                        .map(doc -> doc.read(SUPPORTS_TOTAL_JSON_PATH, int.class))
                        .filter(t -> t > 0)
                        .isPresent();
    }

    @Override
    public ConstraintViolation validate(TerminologyParam param) {
        String url = param.getParam(URL_PARAM);
        if (url == null) {
            return new ConstraintViolation("Missing value-set url");
        }

        return validateCode(url, param.codePhrase(), param.resouceType());
    }

    private ConstraintViolation validateCode(
            String fhirTerminologyUri, CodePhrase codePhrase, TerminologyParam.ResouceType resouceType) {

        String code = codePhrase.getCodeString();
        String terminologyId = codePhrase.getTerminologyId().getValue();

        String url =
                switch (resouceType) {
                    case VALUE_SET ->
                        switch (valuesetValidationMode) {
                            case CODE_SYSTEM ->
                                VALUESET_VALIDATE_CODE_SYSTEM_URL_TPL.formatted(
                                        fhirTerminologyUri, code, terminologyId);
                            case CODING ->
                                VALUESET_VALIDATE_CODING_URL_TPL.formatted(fhirTerminologyUri, terminologyId, code);
                            case EXPAND -> VALUESET_VALIDATE_EXPAND_URL_TPL.formatted(fhirTerminologyUri, 0);
                        };
                    case CODE_SYSTEM -> CODESYSTEM_VALIDATE_URL_TPL.formatted(fhirTerminologyUri, code);
                };

        DocumentContext doc = getDocumentForValidateCode(url);
        if (doc == null) {
            return null;
        }

        if (resouceType == TerminologyParam.ResouceType.VALUE_SET
                && valuesetValidationMode == ValuesetValidationMode.EXPAND) {
            return validateCodeInExpandedValueSet(fhirTerminologyUri, url, doc, code, terminologyId);
        } else {
            boolean noResult = doc.<List<?>>read(VALIDATE_CODE_RESULT_JSON_PATH).isEmpty();
            if (noResult) {
                List<String> messages = doc.read(VALIDATE_CODE_MESSAGE_JSON_PATH);
                return new ConstraintViolation(String.join("; ", messages));
            }
        }

        return null;
    }

    private @Nullable DocumentContext getDocumentForValidateCode(String url) {
        try {
            return internalGet(url);
        } catch (WebClientException e) {
            if (failOnError) {
                throw new ExternalTerminologyValidationException(
                        "An error occurred during terminology code validation request", e);
            }
            LOG.warn("FHIR terminology server validate call failed for {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     *
     * @param fhirTerminologyUri for paging
     * @param url for ConstraintViolations
     * @param doc the ValueSet json
     * @param code
     * @param system
     * @return a ConstraintViolation or null
     */
    private ConstraintViolation validateCodeInExpandedValueSet(
            String fhirTerminologyUri, String url, ReadContext doc, String code, String system) {
        // find by expansion/contains/code; 'contains' can be nested
        Filter codeFilter = Filter.filter(Criteria.where("code").eq(code));

        // gathers mismatching systems that support the code, may be null
        List<String> otherSystems = null;

        ReadContext page = doc;
        int pageNr = 1;
        while (page != null) {

            List<Map<String, String>> matchingCodings = page.read("$.expansion..contains[?]", codeFilter);

            boolean matchingSystem =
                    matchingCodings.stream().anyMatch(coding -> Strings.CS.equals(system, coding.get(SYSTEM_ATT)));
            if (matchingSystem) {
                return null;
            }
            if (!matchingCodings.isEmpty()) {
                Stream<String> systemsStream = matchingCodings.stream().map(c -> c.get(SYSTEM_ATT));
                if (otherSystems == null) {
                    otherSystems = systemsStream.collect(Collectors.toList());
                } else {
                    systemsStream.forEach(otherSystems::add);
                }
            }

            if (pageNr > MAX_VALUESET_EXPAND_PAGES) {
                Integer total = doc.read("$.expansion.total", Integer.class);
                Integer offset = doc.read("$.expansion.offset", Integer.class);
                Integer count = Optional.of(
                                doc.<List<Integer>>read("$.expansion.parameter[?(@.name=='count')].valueInteger"))
                        .filter(CollectionUtils::isNotEmpty)
                        .map(ListUtils::getFirst)
                        .orElse(null);
                return new ConstraintViolation(
                        "ValueSet too large: more than %s pages, total: %d, offset: %d, count: %d"
                                .formatted(MAX_VALUESET_EXPAND_PAGES, total, offset, count));
            }

            // continue paging?
            try {
                page = getNextValueSetPage(page, fhirTerminologyUri);
            } catch (ExternalTerminologyValidationException e) {
                // handle ExternalTerminologyValidationException as regular ConstraintViolation
                return new ConstraintViolation(e.getMessage());
            }
            pageNr++;
        }

        if (otherSystems == null) {
            return new ConstraintViolation(
                    "The value %s#%s does not match any option from ValueSet %s".formatted(system, code, url));
        } else {
            String systems = String.join(", ", otherSystems);
            return new ConstraintViolation("The terminology id for code %s must be %s".formatted(code, systems));
        }
    }

    private @Nullable ReadContext getNextValueSetPage(ReadContext doc, String fhirTerminologyUri) {

        int containsCount = doc.read("$.expansion.contains.length()", Integer.class);
        // empty page
        if (containsCount == 0) {
            return null;
        }
        int offset = ObjectUtils.firstNonNull(doc.read("$.expansion.offset", Integer.class), 0);
        int totalRetrieved = containsCount + offset;

        // already last page?
        // rely on trailing empty page if total is not consistent
        Integer total = doc.read("$.expansion.total", Integer.class);
        if (total != null && totalRetrieved == total) {
            return null;
        }

        DocumentContext nextPage = getDocumentForValidateCode(
                VALUESET_VALIDATE_EXPAND_URL_TPL.formatted(fhirTerminologyUri, totalRetrieved));

        if (nextPage == null) {
            return null;
        }

        // check paging works
        int nextOffset = ObjectUtils.firstNonNull(nextPage.read("$.expansion.offset", Integer.class), 0);
        if (!Objects.equals(totalRetrieved, nextOffset)) {
            throw new ExternalTerminologyValidationException(
                    "Inconsistent paging: The ValueSet expansion offset for %s is incomplete; expected %s vs. %s "
                            .formatted(fhirTerminologyUri, totalRetrieved, nextOffset));
        }

        return nextPage;
    }
}
