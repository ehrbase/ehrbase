/*
 * Copyright 2022 vitasystems GmbH and Hannover Medical School.
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
import org.ehrbase.serialisation.exception.UnmarshalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * EhrScape API exception handler.
 *
 * @author Renaud Subiger
 * @since 1.0.0
 */
@RestControllerAdvice(basePackages = "org.ehrbase.rest.ehrscape.controller")
public class EhrScapeExceptionHandler {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @ExceptionHandler({
      GeneralRequestProcessingException.class,
      InvalidApiParameterException.class,
      MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class,
      ValidationException.class,
      UnmarshalException.class,
      UnprocessableEntityException.class
  })
  public ResponseEntity<Object> handleBadRequestExceptions(Exception ex) {
    return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ObjectNotFoundException.class)
  public ResponseEntity<Object> handleObjectNotFoundException(ObjectNotFoundException ex) {
    return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(NotAcceptableException.class)
  public ResponseEntity<Object> handleNotAcceptableException(NotAcceptableException ex) {
    return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(),
        HttpStatus.NOT_ACCEPTABLE);
  }

  @ExceptionHandler(StateConflictException.class)
  public ResponseEntity<Object> handleStateConflictException(StateConflictException ex) {
    return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(PreconditionFailedException.class)
  public ResponseEntity<Object> handlePreconditionFailedException(PreconditionFailedException ex) {
    var headers = new HttpHeaders();

    if (ex.getUrl() != null && ex.getCurrentVersionUid() != null) {
      headers.setETag("\"" + ex.getCurrentVersionUid() + "\"");
      headers.setLocation(URI.create(ex.getUrl()));
    }

    return handleExceptionInternal(ex, ex.getMessage(), headers, HttpStatus.PRECONDITION_FAILED);
  }

  @ExceptionHandler(UnsupportedMediaTypeException.class)
  public ResponseEntity<Object> handleUnsupportedMediaTypeException(
      UnsupportedMediaTypeException ex) {
    return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUncaughtException(Exception ex) {
    var message = "An internal error has occurred. Please contact your administrator.";
    return handleExceptionInternal(ex, message, new HttpHeaders(),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<Object> handleExceptionInternal(Exception ex, String message,
      HttpHeaders headers, HttpStatus status) {

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
