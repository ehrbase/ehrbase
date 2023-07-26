/*
 * Copyright (c) 2021-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.util;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.parser.OPTParser;
import org.openehr.schemas.v1.OBJECTID;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

/**
 * Utility class that implements basic operations for OPT template.
 *
 * @author Renaud Subiger
 * @since 1.0.0
 */
public class TemplateUtils {

    public static final List<String> UNSUPPORTED_RM_TYPES = List.of("ITEM_TABLE");

    private TemplateUtils() {}

    /**
     * Check whether the given OPT template is supported.
     *
     * @param template the candidate template
     * @return <code>true</code> if the template is supported
     */
    public static boolean isSupported(OPERATIONALTEMPLATE template) {
        var webTemplate = new OPTParser(template).parse();
        return isSupported(webTemplate);
    }

    /**
     * Check whether the given WebTemplate is supported.
     *
     * @param template the candidate template
     * @return <code>true</code> if the template is supported
     */
    public static boolean isSupported(WebTemplate template) {
        return template.getTree()
                .findMatching(node -> UNSUPPORTED_RM_TYPES.contains(node.getRmType()))
                .isEmpty();
    }

    /**
     * Retrieves the template ID from the given OPT template.
     *
     * @param template the template
     * @return template ID
     */
    public static String getTemplateId(OPERATIONALTEMPLATE template) {
        if (template == null) {
            throw new IllegalArgumentException("Template must not be null");
        }
        return Optional.ofNullable(template.getTemplateId())
                .map(OBJECTID::getValue)
                .orElseThrow(() -> new IllegalArgumentException("Template ID must not be null for the given template"));
    }

    /**
     * Retrieves the template unique ID from the given OPT template.
     *
     * @param template the template
     * @return template unique ID
     */
    public static UUID getUid(OPERATIONALTEMPLATE template) {
        if (template == null) {
            throw new IllegalArgumentException("Template must not be null");
        }
        return Optional.ofNullable(template.getUid())
                .map(OBJECTID::getValue)
                .map(UUID::fromString)
                .orElseThrow(() -> new IllegalArgumentException("Unique ID must not be null for the given template"));
    }
}
