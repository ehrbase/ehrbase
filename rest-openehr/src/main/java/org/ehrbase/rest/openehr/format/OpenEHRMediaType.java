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
package org.ehrbase.rest.openehr.format;

import org.springframework.http.MediaType;

/**
 * OpenEHR specific Formats.
 */
public class OpenEHRMediaType {

    /**
     * Format <code>structSDT</code> JSON.
     *
     * @see <a href="https://specifications.openehr.org/releases/ITS-REST/latest/simplified_data_template.html">SDT</a>
     */
    public static final String APPLICATION_WT_JSON_VALUE = "application/openehr.wt+json";

    /**
     * @see #APPLICATION_WT_JSON_VALUE
     */
    public static final MediaType APPLICATION_WT_JSON = new MediaType("application", "openehr.wt+json");

    /**
     * Format <code>structSDT</code> JSON.
     *
     * @see <a href="https://specifications.openehr.org/releases/ITS-REST/latest/simplified_data_template.html">SDT</a>
     */
    public static final String APPLICATION_WT_STRUCTURED_SCHEMA_JSON_VALUE =
            "application/openehr.wt.structured.schema+json";

    /**
     * @see #APPLICATION_WT_STRUCTURED_SCHEMA_JSON_VALUE
     */
    public static final MediaType APPLICATION_WT_STRUCTURED_SCHEMA_JSON =
            new MediaType("application", "openehr.wt.structured.schema+json");

    /**
     * Formats <code>ncSDT</code> is an extract from an Operational Template (OPT) that uses AQL-style paths.
     *
     * @see <a href="https://specifications.openehr.org/releases/ITS-REST/latest/simplified_data_template.html">SDT</a>
     */
    public static final String APPLICATION_WT_FLAT_SCHEMA_JSON_VALUE = "application/openehr.wt.flat.schema+json";

    /**
     * #see #OPENEHR_WT_FLAT_SCHEMA_JSON_VALUE
     */
    public static final MediaType APPLICATION_WT_FLAT_SCHEMA_JSON =
            new MediaType("application", "openehr.wt.flat.schema+json");
}
