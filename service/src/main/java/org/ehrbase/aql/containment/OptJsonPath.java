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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.ehrbase.opt.query.I_QueryOptMetaData;
import org.ehrbase.service.KnowledgeCacheService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * prepare and perform jsonpath queries on WebTemplates
 */
public class OptJsonPath {

    private KnowledgeCacheService knowledgeCache;

    public OptJsonPath(KnowledgeCacheService knowledgeCache) {
        this.knowledgeCache = knowledgeCache;
    }


    public String representAsString(String templateId) throws IllegalStateException {
        I_QueryOptMetaData queryOptMetaData = knowledgeCache.getQueryOptMetaData(templateId);

        if (queryOptMetaData != null)
            return toJson((Map) queryOptMetaData.getJsonPathVisitor());
        else
            return null;
    }

    private String toJson(Map<String, Object> map) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.setPrettyPrinting().create();
        return gson.toJson(map);
    }

    public Map<String, Object> jsonPathEval(String json, String jsonPathExpression) {
        DocumentContext jsonPathContext = JsonPath.parse(json);
        Object pathResult = jsonPathContext.read(JsonPath.compile(jsonPathExpression));

        if (pathResult instanceof List && !((List) pathResult).isEmpty())
            return (Map<String, Object>) ((List) pathResult).get(0);
        else if (pathResult instanceof Map && !((Map) pathResult).isEmpty())
            return (Map<String, Object>) pathResult;
        else
            return null;
    }

    public Map<String, Object> evaluate(String templateId, String jsonPathExpression) {


        //extract all nodeIds from the query
        List<String> nodeIds = Arrays.stream(jsonPathExpression.split("\\.\\."))
                .filter(s -> s.contains("@.node_id"))
                .map(s -> s.replace("[?(@.node_id == '", "")
                        .replace("[?(@.node_id == '", "")
                        .replace("')]", ""))
                .map(String::trim)
                .collect(Collectors.toList());

        //If the template dos not contain all nodeIds fo the query it can not be part of the query
        if (!knowledgeCache.containsNodeIds(templateId, nodeIds)) {
            return Collections.emptyMap();
        } else {
            String json = representAsString(templateId);
            return jsonPathEval(json, jsonPathExpression);
        }
    }


}
