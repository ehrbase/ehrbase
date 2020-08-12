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

import com.jayway.jsonpath.JsonPath;
import org.ehrbase.opt.query.QueryOptMetaData;
import org.ehrbase.service.KnowledgeCacheService;

import java.util.ArrayList;
import java.util.List;

/**
 * Process jsonpath queries on WebTemplates
 */
public class Templates {

    private final KnowledgeCacheService knowledgeCache;

    public Templates(KnowledgeCacheService knowledgeCache) {
        this.knowledgeCache = knowledgeCache;
    }

    /**
     * build the results for a jsonpath query applied to all defined templates in the KnowledgeCacheService
     * @param jsonQueryExpression
     * @return
     */
    public List<JsonPathQueryResult> resolve(String jsonQueryExpression) {
        if (jsonQueryExpression == null)
            return null;

        List<JsonPathQueryResult> jsonPathQueryResults = new ArrayList<>();
        //traverse the templates and identify the ones satisfying the query
        for (String templateId : knowledgeCache.getAllTemplateIds()) {
            JsonPathQueryResult result = resolveForTemplate(templateId, jsonQueryExpression);
            if (result != null) {
                jsonPathQueryResults.add(result);
            }
        }
        return jsonPathQueryResults;
    }

    /**
     * build the results for a jsonpath query applied to a defined templates in the KnowledgeCacheService
     * @param templateId
     * @param jsonQueryExpression
     * @return
     */
    public JsonPathQueryResult resolveForTemplate(String templateId, String jsonQueryExpression) {

       /*
        Map<String, Object> results = new OptJsonPath(knowledgeCache).evaluate(templateId,jsonQueryExpression);
        JsonPathQueryResult jsonPathQueryResult = knowledgeCache.resolveForTemplate(templateId, jsonQueryExpression);

        if (results != null && !results.isEmpty()){
            return  new JsonPathQueryResult(templateId, results);
        }
        return null;

        */
        return knowledgeCache.resolveForTemplate(templateId, jsonQueryExpression);
    }

    /**
     * retrieve composition Node Id from template
     * @param templateId
     * @return
     */
    public String rootArchetypeNodeId(String templateId) {
        try {
            QueryOptMetaData queryOptMetaData = QueryOptMetaData.initialize(knowledgeCache.retrieveOperationalTemplate(templateId).get());
            return JsonPath.read(queryOptMetaData.getJsonPathVisitor(), "$['tree']['node_id']").toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not retrieve template meta data:" + e);
        }
    }
}
