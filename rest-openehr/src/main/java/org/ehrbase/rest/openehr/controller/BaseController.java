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

package org.ehrbase.rest.openehr.controller;

import org.ehrbase.api.definitions.CompositionFormat;
import org.apache.commons.lang.StringUtils;
import org.ehrbase.api.exception.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This base controller implements the basic functionality for all specific controllers. This includes error handling
 * and utils.
 */
public abstract class BaseController {

    static final String RETURN_MINIMAL = "return=minimal";
    static final String RETURN_REPRESENTATION = "return=representation";

    // Fixed header identifiers
    static final String CONTENT_TYPE = HttpHeaders.CONTENT_TYPE;
    static final String LOCATION = HttpHeaders.LOCATION;
    static final String ETAG = HttpHeaders.ETAG;
    static final String LAST_MODIFIED = HttpHeaders.LAST_MODIFIED;
    static final String ACCEPT = HttpHeaders.ACCEPT;
    static final String PREFER = "PREFER";
    static final String IF_MATCH = HttpHeaders.IF_MATCH;
    static final String IF_NONE_MATCH = HttpHeaders.IF_NONE_MATCH;

    // Configuration of swagger-ui description fields
    // request headers
    static final String REQ_OPENEHR_VERSION = "Optional custom request header for versioning";
    static final String REQ_OPENEHR_AUDIT = "Optional custom request header for auditing";
    static final String REQ_CONTENT_TYPE = "Client may request content format";
    static final String REQ_CONTENT_TYPE_BODY = "Format of transferred body";
    static final String REQ_ACCEPT = "Client should specify expected format";
    static final String REQ_PREFER = "May be used by clients for resource representation negotiation";
    // response headers
    static final String RESP_CONTENT_TYPE_DESC = "Format of response";
    static final String RESP_LOCATION_DESC = "Location of resource";
    static final String RESP_ETAG_DESC = "Entity tag for resource";
    static final String RESP_LAST_MODIFIED_DESC = "Time of last modification of resource";
    // common response description fields
    static final String RESP_NOT_ACCEPTABLE_DESC = "Not Acceptable - Service can not fulfill requested format via accept header.";
    static final String RESP_UNSUPPORTED_MEDIA_DESC = "Unsupported Media Type - request's content-type not supported.";

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

    /**
     * Helper to allow string UUID input from controllers, which throws an ObjectNotFound exception when no UUID representation
     * can be created. This case is equal to no matching object.
     * @param ehrIdString Input String representation of the ehrId
     * @throws ObjectNotFoundException when no UUID can't be created from input
     * @return UUID representation of the ehrId
     */
    protected UUID getEhrUuid(String ehrIdString) {
        return extractUUIDFromStringWithError(ehrIdString, "ehr", "EHR not found, in fact, only UUID-type IDs are supported");
    }

    /**
     * Helper to allow string UUID input from controllers, which throws an ObjectNotFound exception when no UUID representation
     * can be created. This case is equal to no matching object.
     * @param compositionVersionedObjectUidString Input String representation
     * @throws ObjectNotFoundException when no UUID can't be created from input
     * @return UUID representation
     */
    protected UUID getCompositionVersionedObjectUidString(String compositionVersionedObjectUidString) {
        return extractUUIDFromStringWithError(compositionVersionedObjectUidString, "composition", "Composition not found, in fact, only UUID-type versionedObjectUids are supported");
    }

    /**
     * Helper to allow string UUID input from controllers, which throws an ObjectNotFound exception when no UUID representation
     * can be created. This case is equal to no matching object.
     * @param compositionVersionedObjectUidString Input String representation
     * @throws ObjectNotFoundException when no UUID can't be created from input
     * @return UUID representation
     */
    protected UUID getContributionVersionedObjectUidString(String compositionVersionedObjectUidString) {
        return extractUUIDFromStringWithError(compositionVersionedObjectUidString, "contribution", "Contribution not found, in fact, only UUID-type versionedObjectUids are supported");
    }

    // Internal abstraction layer helper, so calling methods above can invoke with meaningful error messages depending on context.
    private UUID extractUUIDFromStringWithError(String uuidString, String type, String error) {
        UUID uuid = null;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new ObjectNotFoundException(type, error);
        }
        return uuid;
    }

    /**
     * Extracts the {@link CompositionFormat} from the REST request's input {@link MediaType} style content type header string.
     * @param contentType String representation of REST request's input {@link MediaType} style content type header
     * @return {@link CompositionFormat} expressing the content type
     * @throws NotAcceptableException when content type is not supported or input is invalid
     */
    protected CompositionFormat extractCompositionFormat(String contentType) {
        final CompositionFormat compositionFormat;

        if (MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_XML)) {
            compositionFormat = CompositionFormat.XML;
        } else if (MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON)) {
            compositionFormat = CompositionFormat.JSON;
        } else {
            throw new NotAcceptableException("Only compositions in XML or JSON are supported at the moment");
        }
        return compositionFormat;
    }

    /**
     * Convenience helper to encode path strings to URI-safe strings
     * @param path input
     * @return URI-safe escaped string
     * @throws InternalServerException when encoding failed
     */
    public String encodePath(String path) {
        try {
            path = UriUtils.encodePath(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new InternalServerException("Error encoding parameter {}", e);
        }
        return path;
    }

    protected ResponseEntity<Map<String, String>> createErrorResponse(String message, HttpStatus status) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("status", status.getReasonPhrase());
        return new ResponseEntity<>(error, status);
    }

    /**
     * Extracts the UUID base from a versioned UID. Or, if
     * @param versionUid
     * @return
     */
    protected UUID extractVersionedObjectUidFromVersionUid(String versionUid) {
        if (!versionUid.contains("::"))
            return UUID.fromString(versionUid);
        return UUID.fromString(versionUid.substring(0, versionUid.indexOf("::")));
    }

    protected int extractVersionFromVersionUid(String versionUid) {
        if (!versionUid.contains("::"))
            return 0; //current version
        // extract the version from string of format "$UUID::$SYSTEM::$VERSION"
        // via making a substring starting at last occurrence of "::" + 2
        return Integer.valueOf(versionUid.substring(versionUid.lastIndexOf("::") + 2));
    }

    /*
    EXCEPTION HANDLING GENERAL BEHAVIOR DEFINITION
     */

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

    /**
     * This handler catches the exception automatically generated and thrown by the framework, when the request's
     * message can't be read. For example, due to missing body, while required.
     * @return ResponseEntity<Map<String, String>> as BAD_REQUEST - 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(HttpMessageNotReadableException e) {
        return createErrorResponse("Bad Request: HTTP message not readable, for instance, due to missing parameter. Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 401 Unauthorized is created automatically by framework

    /**
     * Handler for authentication related errors.
     * @return ResponseEntity<Map<String, String>> as FORBIDDEN - 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(AccessDeniedException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
    }

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
     * @return ResponseEntity<Map<String, String>> as NOT_ACCEPTABLE - 406
     */
    @ExceptionHandler(NotAcceptableException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(NotAcceptableException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
    }

    /**
     * Handler for project-custom exception.
     * @return ResponseEntity<Map<String, String>> as CONFLICT - 409
     */
    @ExceptionHandler(StateConflictException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(StateConflictException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Handler for project-custom exception.
     * @return ResponseEntity<Map<String, String>> as PRECONDITION FAILED - 412
     */
    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(PreconditionFailedException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.PRECONDITION_FAILED);
    }

    /**
     * Handler for all exceptions that are caused by a payload that cannot be
     * processed by a service or subordinated handlers.
     *
     * @param e - UnsupportedMediaTypeException thrown at handler mechanism
     * @return ResponseEntity<Map<String, String>> as UNSUPPORTED MEDIA Type - 415
     */
    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ResponseEntity<Map<String, String>> restErrorHandler(UnsupportedMediaTypeException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
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
}
