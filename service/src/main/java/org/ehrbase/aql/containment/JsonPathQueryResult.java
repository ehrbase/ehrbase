/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.containment;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.MapUtils;

/**
 * wrap the result of a jsonpath query
 * This is required since the result is generally a Map of multiple objects
 */
public class JsonPathQueryResult implements Serializable {

    private final String templateId;
    private final Set<String> aqlPath;

    public JsonPathQueryResult(String templateId, Map<String, Object> objectMap) {
        this.templateId = templateId;

        if (!MapUtils.isEmpty(objectMap)) {
            aqlPath = (Set<String>) objectMap.get("aql_path");
        } else {
            aqlPath = null;
        }
    }

    public JsonPathQueryResult(String templateId, Set<String> aqlPath) {
        this.templateId = templateId;
        this.aqlPath = aqlPath;
    }

    public String getTemplateId() {
        return templateId;
    }

    public Set<String> getAqlPath() {
        return aqlPath;
    }
}
