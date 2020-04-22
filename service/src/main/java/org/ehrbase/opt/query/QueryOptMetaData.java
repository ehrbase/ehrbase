/*
 * Modifications copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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

package org.ehrbase.opt.query;

import org.ehrbase.opt.OptVisitor;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.util.List;
import java.util.Map;

/**
 * Created by christian on 5/7/2018.
 */
public class QueryOptMetaData implements I_QueryOptMetaData {

    Object document;

    private QueryOptMetaData(Object document) {
        this.document = document;
    }

    /**
     * prepare a document for querying
     *
     * @return
     */
    public static QueryOptMetaData initialize(OPERATIONALTEMPLATE operationaltemplate) throws Exception {
        Map map = new OptVisitor().traverse(operationaltemplate);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(new MapJson(map).toJson());
        return new QueryOptMetaData(document);
    }

    public static I_QueryOptMetaData getInstance(OPERATIONALTEMPLATE operationaltemplate) throws Exception {
        return initialize(operationaltemplate);
    }

    public static I_QueryOptMetaData getInstance(String visitor) throws Exception {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(visitor);
        return new QueryOptMetaData(document);
    }

    public static I_QueryOptMetaData getInstance(Object visitor) throws Exception {
        return new QueryOptMetaData(visitor);
    }


    /**
     * returns all path for which upper limit is unbounded.
     *
     * @return
     */
    @Override
    public List upperNotBounded() {
        return JsonPath.read(document, "$..children[?(@.max == -1)]");
    }

    /**
     * get the type of the node identified with path
     *
     * @param path
     * @return
     */
    @Override
    public String type(String path) {
        return attributeChildValue(path, "type");
    }

    @Override
    public String category(String path) {
        return attributeChildValue(path, "category");
    }

    private String attributeChildValue(String path, String attribute){
        Object child = JsonPath.read(document, "$..children[?(@.aql_path == '" + path + "')]");

        if (child != null && child instanceof JSONArray && ((JSONArray) child).size() > 0) {
            Object childDef = ((JSONArray) child).get(0);
            if (childDef != null && childDef instanceof Map) {
                return (String) ((Map) childDef).get(attribute);
            }
        }

        return null;
    }

    /**
     * return the list of node with name == 'name'
     *
     * @param value
     * @return
     */
    @Override
    public List nodeByFieldValue(String field, String value) {
        return JsonPath.read(document, "$..children[?(@." + field + " == '" + value + "')]");
    }


    @Override
    public List nodeFieldRegexp(String field, String regexp) {
        return JsonPath.read(document, "$..children[?(@." + field + " =~ " + regexp + ")]");
    }

    @Override
    public Object getJsonPathVisitor() {
        return document;
    }

    @Override
    public String getTemplateConcept() {
        return (String) ((Map) document).get("concept");
    }

    @Override
    public String getTemplateId() {
        return (String) ((Map) document).get("template_id");
    }

}
