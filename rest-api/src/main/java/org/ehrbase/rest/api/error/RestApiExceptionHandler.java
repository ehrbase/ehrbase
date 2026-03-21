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

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the REST API v1.
 * Maps all domain exceptions to RFC 7807 ProblemDetail responses.
 */
@RestControllerAdvice(basePackages = "org.ehrbase.rest.api")
public class RestApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestApiExceptionHandler.class);

    @ExceptionHandler(ObjectNotFoundException.class)
    public ProblemDetail handleNotFound(ObjectNotFoundException ex, HttpServletRequest request) {
        DomainErrorCode code = resolveNotFoundCode(ex.getMessage());
        return code.toProblemDetail(ex.getMessage(), URI.create(request.getRequestURI()));
    }

    @ExceptionHandler(PreconditionFailedException.class)
    public ProblemDetail handlePreconditionFailed(PreconditionFailedException ex, HttpServletRequest request) {
        return DomainErrorCode.COMPOSITION_VERSION_MISMATCH.toProblemDetail(
                ex.getMessage(), URI.create(request.getRequestURI()));
    }

    @ExceptionHandler(StateConflictException.class)
    public ProblemDetail handleConflict(StateConflictException ex, HttpServletRequest request) {
        return DomainErrorCode.EHR_NOT_MODIFIABLE.toProblemDetail(ex.getMessage(), URI.create(request.getRequestURI()));
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex, HttpServletRequest request) {
        return DomainErrorCode.VALIDATION_ERROR.toProblemDetail(ex.getMessage(), URI.create(request.getRequestURI()));
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public ProblemDetail handleUnprocessable(UnprocessableEntityException ex, HttpServletRequest request) {
        return DomainErrorCode.TEMPLATE_VALIDATION_FAILED.toProblemDetail(
                ex.getMessage(), URI.create(request.getRequestURI()));
    }

    @ExceptionHandler(InvalidApiParameterException.class)
    public ProblemDetail handleInvalidParam(InvalidApiParameterException ex, HttpServletRequest request) {
        return DomainErrorCode.INVALID_PARAMETER.toProblemDetail(ex.getMessage(), URI.create(request.getRequestURI()));
    }

    @ExceptionHandler(NotAcceptableException.class)
    public ProblemDetail handleNotAcceptable(NotAcceptableException ex, HttpServletRequest request) {
        return DomainErrorCode.INVALID_FORMAT.toProblemDetail(ex.getMessage(), URI.create(request.getRequestURI()));
    }

    @ExceptionHandler(SecurityException.class)
    public ProblemDetail handleSecurity(SecurityException ex, HttpServletRequest request) {
        return DomainErrorCode.FORBIDDEN.toProblemDetail(ex.getMessage(), URI.create(request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return DomainErrorCode.INVALID_PARAMETER.toProblemDetail(ex.getMessage(), URI.create(request.getRequestURI()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ProblemDetail handleUnsupported(UnsupportedOperationException ex, HttpServletRequest request) {
        return DomainErrorCode.FEATURE_DISABLED.toProblemDetail(ex.getMessage(), URI.create(request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setType(DomainErrorCode.INTERNAL_ERROR.typeUri());
        pd.setTitle("Internal Server Error");
        pd.setProperty("code", "INTERNAL_ERROR");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    private DomainErrorCode resolveNotFoundCode(String message) {
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("ehr")) return DomainErrorCode.EHR_NOT_FOUND;
            if (lower.contains("composition")) return DomainErrorCode.COMPOSITION_NOT_FOUND;
            if (lower.contains("template")) return DomainErrorCode.TEMPLATE_NOT_FOUND;
            if (lower.contains("contribution")) return DomainErrorCode.CONTRIBUTION_NOT_FOUND;
        }
        return DomainErrorCode.COMPOSITION_NOT_FOUND;
    }
}
