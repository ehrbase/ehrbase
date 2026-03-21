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
package org.ehrbase.rest.api.controller;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Base controller with shared utility methods for all REST API v1 controllers.
 * All controllers extend this to get consistent UUID parsing, URI building, and header handling.
 */
public abstract class BaseApiController {

    protected static final String API_V1 = "/api/v1";
    protected static final String PREFER_RETURN_REPRESENTATION = "return=representation";
    protected static final String PREFER_RETURN_MINIMAL = "return=minimal";

    protected UUID parseEhrId(String ehrIdString) {
        return parseUuid(ehrIdString, "ehr");
    }

    protected UUID parseUuid(String uuidString, String resourceName) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new InvalidApiParameterException("Invalid %s ID: %s".formatted(resourceName, uuidString));
        }
    }

    protected UUID extractVersionedObjectUid(String versionUid) {
        int sep = versionUid.indexOf("::");
        String uuidPart = sep > 0 ? versionUid.substring(0, sep) : versionUid;
        return parseUuid(uuidPart, "versioned_object");
    }

    protected int extractVersion(String versionUid) {
        int lastSep = versionUid.lastIndexOf("::");
        if (lastSep < 0) {
            throw new InvalidApiParameterException("Invalid version UID format: %s".formatted(versionUid));
        }
        return Integer.parseInt(versionUid.substring(lastSep + 2));
    }

    protected OffsetDateTime parseVersionAtTime(String versionAtTime) {
        if (versionAtTime == null || versionAtTime.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(versionAtTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new InvalidApiParameterException("Invalid version_at_time format: %s".formatted(versionAtTime));
        }
    }

    protected URI locationUri(String... pathSegments) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .pathSegment(pathSegments)
                .build()
                .toUri();
    }

    protected ResponseEntity.BodyBuilder created(URI location, String etag) {
        return ResponseEntity.created(location).eTag("\"" + etag + "\"");
    }

    protected ResponseEntity.BodyBuilder ok(String etag) {
        return ResponseEntity.ok().eTag("\"" + etag + "\"");
    }

    protected boolean preferRepresentation(String preferHeader) {
        return preferHeader != null && preferHeader.contains(PREFER_RETURN_REPRESENTATION);
    }
}
