/*
 * Copyright (c) 2019-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.rest.ehrscape.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This base controller implements the basic functionality for all specific controllers. This
 * includes error handling and utils.
 *
 * @author Stefan Spiska
 * @author Jake Smolka
 */
public abstract class BaseController {
    public static final String EHR = "ehr";
    public static final String TEMPLATE = "template";
    public static final String API_ECIS_CONTEXT_PATH_WITH_VERSION = "/rest/ecis/v1";
    public static final String COMPOSITION = "composition";

    public Map<String, Map<String, String>> add2MetaMap(
            Map<String, Map<String, String>> metaMap, String key, String value) {
        Map<String, String> contentMap;

        if (metaMap == null) {
            metaMap = new HashMap<>();
            contentMap = new HashMap<>();
            metaMap.put("meta", contentMap);
        } else {
            contentMap = metaMap.get("meta");
        }

        contentMap.put(key, value);
        return metaMap;
    }

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
}
