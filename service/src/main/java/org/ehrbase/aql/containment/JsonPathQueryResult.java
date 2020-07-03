/*
 * Copyright (C) 2020 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase
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
package org.ehrbase.aql.containment;

import java.util.Map;

/**
 * wrap the result of a jsonpath query
 * This is required since the result is generally a Map of multiple objects
 */
public class JsonPathQueryResult {

    private String template_id;
    private String aql_path;

    public JsonPathQueryResult(String template_id, Map<String, Object> objectMap) {
        this.template_id = template_id;

        if (!objectMap.isEmpty()){
            aql_path = (String) objectMap.get("aql_path");
        }
    }

    public String getTemplateId() {
        return template_id;
    }

    public String getAqlPath() {
        return aql_path;
    }
}
