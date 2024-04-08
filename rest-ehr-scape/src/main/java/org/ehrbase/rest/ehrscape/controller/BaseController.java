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
package org.ehrbase.rest.ehrscape.controller;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * This base controller implements the basic functionality for all specific controllers. This
 * includes error handling and utils.
 */
public abstract class BaseController {
    public static final String TEMPLATE = "template";
    public static final String API_ECIS_CONTEXT_PATH_WITH_VERSION = "/rest/ecis/v1";
    public static final String COMPOSITION = "composition";

    protected String getContextPath() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

    /**
     * Returns a URI for list of segments.
     * The segments are appended to the base path and encoded to ensure safe usage in a URI.
     *
     * @param pathSegments List of segments to append to the base URL
     * @return URI for the given base URL and segments
     */
    protected String createLocationUri(String... pathSegments) {
        return UriComponentsBuilder.fromHttpUrl(getContextPath())
                .path(API_ECIS_CONTEXT_PATH_WITH_VERSION)
                .pathSegment(pathSegments)
                .build()
                .toUriString();
    }

    private static final String SWAGGER_EHR_SCAPE_API = "swagger-ui/index.html?urls.primaryName=2.%20EhrScape%20API#";
    private static final String SWAGGER_OPENEHR_API = "swagger-ui/index.html?urls.primaryName=1.%20openEHR%20API#";

    @SuppressWarnings("UastIncorrectHttpHeaderInspection")
    protected HttpHeaders deprecationHeaders(String deprecatedPath, String successorVersion) {
        var headers = new HttpHeaders();
        headers.add("Deprecated", "Mon, 22 Jan 2024 00:00:00 GMT");

        List<String> links = List.of(
                "<%s/%s/%s>; rel=\"deprecation\"; type=\"text/html\""
                        .formatted(
                                getContextPath(),
                                SWAGGER_EHR_SCAPE_API,
                                UriUtils.encode(deprecatedPath, StandardCharsets.US_ASCII)),
                "<%s/%s/%s>; rel=\"successor-version\""
                        .formatted(
                                getContextPath(),
                                SWAGGER_OPENEHR_API,
                                UriUtils.encode(successorVersion, StandardCharsets.US_ASCII)));
        headers.add("Link", String.join(", ", links));
        // headers.add("Sunset", "Tue, 31 Dec 2024 00:00:00 GMT"); <- could be used until we know it ;)
        return headers;
    }
}
