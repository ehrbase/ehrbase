/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.rest.ehrscape.controller;

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * This base controller implements the basic functionality for all specific controllers. This includes error handling
 * and utils.
 */
public abstract class BaseController {

    public Map<String, Map<String, String>> add2MetaMap(Map<String, Map<String, String>> metaMap, String key, String value) {
        Map<String, String> contentMap;

        if (metaMap == null) {
            metaMap = new HashMap<>();
            contentMap = new HashMap<>();
            metaMap.put("meta", contentMap);
        } else
            contentMap = metaMap.get("meta");

        contentMap.put(key, value);
        return metaMap;
    }

    protected String getBaseEnvLinkURL() {
        String baseEnvLinkURL = null;
        HttpServletRequest currentRequest =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        // lazy about determining protocol but can be done too
        baseEnvLinkURL = "http://" + currentRequest.getLocalName();
        if (currentRequest.getLocalPort() != 80) {
            baseEnvLinkURL += ":" + currentRequest.getLocalPort();
        }
        if (!StringUtils.isEmpty(currentRequest.getContextPath())) {
            baseEnvLinkURL += currentRequest.getContextPath();
        }
        return baseEnvLinkURL;
    }

    // 204 No content is handled in controllers, as it refers to a non-error situation.

    /**
     * Handler for project-custom exception.
     * @return ResponseEntity<Map<String, String>> as ALREADY_REPORTED - 208
     */
    @ExceptionHandler(DuplicateObjectException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(DuplicateObjectException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.ALREADY_REPORTED);
    }

    /**
     * Handler for broad and general Java standard exception IllegalArgumentException. Shall be replaced with a more
     * specific exception like InvalidApiParameterException in backend code with time.
     * @deprecated Throw a more specific exception.
     * @return ResponseEntity<Map<String, String>> as BAD_REQUEST - 400
     */
    @Deprecated
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(IllegalArgumentException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handler for project-custom exception.
     * @return ResponseEntity<Map<String, String>> as BAD_REQUEST - 400
     */
    @ExceptionHandler(GeneralRequestProcessingException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(GeneralRequestProcessingException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handler for project-custom exception.
     * @return ResponseEntity<Map<String, String>> as BAD_REQUEST - 400
     */
    @ExceptionHandler(InvalidApiParameterException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(InvalidApiParameterException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * This handler catches the exception automatically generated and thrown by the framework, when specified
     * parameters are not present or matching.
     * @return ResponseEntity<Map<String, String>> as BAD_REQUEST - 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(MissingServletRequestParameterException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 401 Unauthorized is created automatically by framework


    /**
     * Handler for project-custom exception.
     * @return ResponseEntity<Map<String, String>> as NOT_FOUND - 404
     */
    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(ObjectNotFoundException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Handler for project-custom exception.
     * @return ResponseEntity<Map<String, String>> as CONFLICT - 409
     */
    @ExceptionHandler(StateConflictException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(StateConflictException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
    }

    // TODO: Maybe remove this redundant handler since fallback will cover the same functionality
    /**
     * Handler for less specific internal error
     * @return ResponseEntity<Map<String, String>> as INTERNAL_SERVER_ERROR - 500
     */
    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(InternalServerException e) {
        return createErrorResponse("Internal Server Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handler for project-custom exception.
     * @return ResponseEntity<Map<String, String>> as BAD_GATEWAY - 502
     */
    @ExceptionHandler(BadGatewayException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(BadGatewayException e) {
        return createErrorResponse("Bad Gateway: Proxied connection failed", HttpStatus.BAD_GATEWAY);
    }

    /**
     * Fallback error handler.
     * @return ResponseEntity<Map<String, String>> as INTERNAL_SERVER_ERROR - 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(Exception e) {
        return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    protected ResponseEntity<Map<String, String>> createErrorResponse(String message, HttpStatus status) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("status", status.getReasonPhrase());
        return new ResponseEntity<>(error, status);
    }
}
