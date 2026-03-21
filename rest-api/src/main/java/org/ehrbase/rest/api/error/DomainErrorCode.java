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
package org.ehrbase.rest.api.error;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Machine-readable domain error codes for EHRbase REST API.
 * Each code maps to an HTTP status and RFC 7807 type URI.
 */
public enum DomainErrorCode {
    EHR_NOT_FOUND(HttpStatus.NOT_FOUND, "EHR not found"),
    EHR_NOT_MODIFIABLE(HttpStatus.CONFLICT, "EHR is not modifiable"),
    EHR_NOT_QUERYABLE(HttpStatus.CONFLICT, "EHR is not queryable"),
    EHR_ALREADY_EXISTS(HttpStatus.CONFLICT, "EHR already exists"),

    COMPOSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "Composition not found"),
    COMPOSITION_VERSION_MISMATCH(HttpStatus.PRECONDITION_FAILED, "Composition version mismatch"),
    COMPOSITION_DELETED(HttpStatus.GONE, "Composition has been deleted"),

    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "Template not found"),
    TEMPLATE_VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Template validation failed"),
    TEMPLATE_IN_USE(HttpStatus.CONFLICT, "Template is referenced by existing compositions"),

    CONTRIBUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Contribution not found"),

    CONSENT_REQUIRED(HttpStatus.CONFLICT, "Consent is required for this operation"),
    CONSENT_WITHDRAWN(HttpStatus.CONFLICT, "Consent has been withdrawn"),

    EMERGENCY_JUSTIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "Emergency access requires justification"),

    TERMINOLOGY_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "Terminology service unavailable"),

    ADL2_NOT_ENABLED(HttpStatus.NOT_IMPLEMENTED, "ADL 2.4 support is not enabled"),
    FEATURE_DISABLED(HttpStatus.NOT_FOUND, "Feature is not enabled"),

    INVALID_FORMAT(HttpStatus.NOT_ACCEPTABLE, "Unsupported content format"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "Invalid request parameter"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),

    QUERY_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "Query execution timed out"),

    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private static final String TYPE_BASE = "https://ehrbase.org/errors/";

    private final HttpStatus status;
    private final String title;

    DomainErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public URI typeUri() {
        return URI.create(TYPE_BASE + name().toLowerCase().replace('_', '-'));
    }

    /**
     * Creates an RFC 7807 ProblemDetail with this error code.
     */
    public ProblemDetail toProblemDetail(String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(typeUri());
        pd.setTitle(title);
        pd.setProperty("code", name());
        return pd;
    }

    /**
     * Creates an RFC 7807 ProblemDetail with instance URI.
     */
    public ProblemDetail toProblemDetail(String detail, URI instance) {
        ProblemDetail pd = toProblemDetail(detail);
        pd.setInstance(instance);
        return pd;
    }
}
