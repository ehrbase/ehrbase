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
package org.ehrbase.configuration.exception;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.ehrbase.api.exception.AqlFeatureNotImplementedException;
import org.ehrbase.api.exception.BadGatewayException;
import org.ehrbase.api.exception.GeneralRequestProcessingException;
import org.ehrbase.api.exception.IllegalAqlException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.UnsupportedMediaTypeException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.openehr.sdk.serialisation.exception.UnmarshalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Default exception handler.
 */
@RestControllerAdvice
public class DefaultExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // 400
    @ExceptionHandler({
        // Spring MVC
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestPartException.class,
        BindException.class,
        ServletRequestBindingException.class,
        // Java/third party Library
        IllegalArgumentException.class,
        // ehrbase/SDK
        GeneralRequestProcessingException.class,
        InvalidApiParameterException.class,
        ValidationException.class,
        UnmarshalException.class,
        AqlFeatureNotImplementedException.class,
        IllegalAqlException.class,
    })
    public ResponseEntity<Object> handleBadRequestExceptions(Exception ex) {
        return handleExceptionInternal(ex, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 404
    @ExceptionHandler({ObjectNotFoundException.class})
    public ResponseEntity<Object> handleObjectNotFoundException(ObjectNotFoundException ex) {
        return handleExceptionInternal(ex, ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // 404 - Servlet resource mapping failure
    @ExceptionHandler({NoResourceFoundException.class})
    public ResponseEntity<Object> handleResourceNotFoundException(NoResourceFoundException ex) {
        // Raised by the dispatch servlet in case the path could not be mapped.
        return handleExceptionInternal(
                ex, "No resource found at path: %s".formatted(ex.getResourcePath()), HttpStatus.NOT_FOUND);
    }

    // 405
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<Object> handleMethodNotAllowedException(HttpRequestMethodNotSupportedException ex) {
        return handleExceptionInternal(ex, ex.getMessage(), HttpStatus.METHOD_NOT_ALLOWED);
    }

    // 406
    @ExceptionHandler({HttpMediaTypeNotAcceptableException.class, NotAcceptableException.class})
    public ResponseEntity<Object> handleNotAcceptableException(Exception ex) {
        return handleExceptionInternal(ex, ex.getMessage(), HttpStatus.NOT_ACCEPTABLE);
    }

    // 409
    @ExceptionHandler(StateConflictException.class)
    public ResponseEntity<Object> handleStateConflictException(StateConflictException ex) {
        return handleExceptionInternal(ex, ex.getMessage(), HttpStatus.CONFLICT);
    }

    // 412
    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<Object> handlePreconditionFailedException(PreconditionFailedException ex) {

        var headers = new HttpHeaders();

        if (ex.getUrl() != null && ex.getCurrentVersionUid() != null) {
            headers.setETag("\"" + ex.getCurrentVersionUid() + "\"");
            headers.setLocation(URI.create(ex.getUrl()));
        }

        return handleExceptionInternal(ex, ex.getMessage(), HttpStatus.PRECONDITION_FAILED, headers);
    }

    // 415
    @ExceptionHandler({HttpMediaTypeNotSupportedException.class, UnsupportedMediaTypeException.class})
    public ResponseEntity<Object> handleUnsupportedMediaTypeException(UnsupportedMediaTypeException ex) {
        return handleExceptionInternal(ex, ex.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // 422
    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<Object> handleUnprocessableEntityException(
            UnprocessableEntityException ex, WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // custom status
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleSpringResponseStatusException(ResponseStatusException ex) {
        // rethrow will not work properly, so we handle it
        return handleExceptionInternal(ex, ex.getReason(), ex.getStatusCode(), ex.getHeaders());
    }

    // 502 - bad gateway
    @ExceptionHandler(BadGatewayException.class)
    public ResponseEntity<Object> handleFooBadGatewayException(BadGatewayException ex) {
        // var message = "An internal error has occurred. Please contact your administrator.";
        return handleExceptionInternal(ex, ex.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    // 500 - general
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUncaughtException(Exception ex) {
        var message = "An internal error has occurred. Please contact your administrator.";
        return handleExceptionInternal(ex, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> handleExceptionInternal(Exception ex, String message, HttpStatusCode status) {
        return handleExceptionInternal(ex, message, status, HttpHeaders.EMPTY);
    }

    private ResponseEntity<Object> handleExceptionInternal(
            Exception ex, String message, HttpStatusCode status, HttpHeaders headers) {

        if (status.is5xxServerError()) {
            logger.error("", ex);
        } else {
            logger.warn(ex.getMessage());
            if (logger.isDebugEnabled()) {
                logger.debug("Exception stack trace", ex);
            }
        }

        Map<String, Object> body = new HashMap<>();
        if (status instanceof HttpStatus httpStatus) {
            body.put("error", httpStatus.getReasonPhrase());
        }
        body.put("message", message);
        return new ResponseEntity<>(body, headers, status);
    }
}
