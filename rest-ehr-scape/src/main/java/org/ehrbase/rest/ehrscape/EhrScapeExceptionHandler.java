/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.rest.ehrscape;

import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;
import org.ehrbase.api.exception.GeneralRequestProcessingException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

/**
 * EhrScape API exception handler.
 *
 * @author Renaud Subiger
 * @since 1.0.0
 */
@RestControllerAdvice(basePackages = "org.ehrbase.rest.ehrscape.controller")
public class EhrScapeExceptionHandler {

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
        UnprocessableEntityException.class,
        ValidationException.class,
        UnmarshalException.class,
    })
    public ResponseEntity<Object> handleBadRequestExceptions(Exception ex) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleObjectNotFoundException(AccessDeniedException ex) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    // 404
    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<Object> handleObjectNotFoundException(ObjectNotFoundException ex) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.NOT_FOUND);
    }

    // 406
    @ExceptionHandler({HttpMediaTypeNotAcceptableException.class, NotAcceptableException.class})
    public ResponseEntity<Object> handleNotAcceptableException(NotAcceptableException ex) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.NOT_ACCEPTABLE);
    }

    // 409
    @ExceptionHandler(StateConflictException.class)
    public ResponseEntity<Object> handleStateConflictException(StateConflictException ex) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.CONFLICT);
    }

    // 412
    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<Object> handlePreconditionFailedException(PreconditionFailedException ex) {

        var headers = new HttpHeaders();

        if (ex.getUrl() != null && ex.getCurrentVersionUid() != null) {
            headers.setETag("\"" + ex.getCurrentVersionUid() + "\"");
            headers.setLocation(URI.create(ex.getUrl()));
        }

        return handleExceptionInternal(ex, ex.getMessage(), headers, HttpStatus.PRECONDITION_FAILED);
    }

    // 415
    @ExceptionHandler({HttpMediaTypeNotSupportedException.class, UnsupportedMediaTypeException.class})
    public ResponseEntity<Object> handleUnsupportedMediaTypeException(UnsupportedMediaTypeException ex) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // custom status
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleSpringResponseStatusException(ResponseStatusException ex) {
        // rethrow will not work properly, so we handle it
        return handleExceptionInternal(ex, ex.getReason(), ex.getResponseHeaders(), ex.getStatus());
    }

    // 500 - general
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUncaughtException(Exception ex) {
        var message = "An internal error has occurred. Please contact your administrator.";
        return handleExceptionInternal(ex, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> handleExceptionInternal(
            Exception ex, String message, HttpHeaders headers, HttpStatus status) {

        if (status.is5xxServerError()) {
            logger.error("", ex);
        } else {
            logger.warn(ex.getMessage());
            if (logger.isDebugEnabled()) {
                logger.debug("Exception stack trace", ex);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, headers, status);
    }
}
