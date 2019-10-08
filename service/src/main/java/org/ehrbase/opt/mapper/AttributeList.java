/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) Ripple Foundation CIC Ltd, UK, 2017
 * Author: Christian Chevalley
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

package org.ehrbase.opt.mapper;

import org.ehrbase.opt.AttributeDef;
import org.ehrbase.opt.ValuePoint;
import org.ehrbase.ehr.encode.wrappers.SnakeCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 2/13/2018.
 */
public class AttributeList {

    Map<String, Object> attributes;

    public AttributeList(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public List<Map<String, Object>> toList(String path) {
        List<Map<String, Object>> attributeList = new ArrayList<>();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            Map<String, Object> definition = new HashMap<>();
            definition.put(Constants.CATEGORY, Constants.LITTERAL_ATTRIBUTE);
//            definition.put(Constants.ATTRIBUTE, new SnakeCase(entry.getKey()).camelToSnake());
//            definition.put(Constants.NAME, StringUtils.capitalize(entry.getKey()));

            definition.putAll(new AttributeDef(entry.getKey()).naming());

            String type = (String) ((Map) entry.getValue()).get(Constants.TYPE);
            definition.put(Constants.TYPE, type);

            //add value points and other data for this type
            List<Map<String, String>> valuePointList = new ValuePoint(type).attributes();
            if (!valuePointList.isEmpty())
                definition.put(Constants.MANDATORY_ATTRIBUTES, valuePointList);

            definition.put(Constants.AQL_PATH, path + "/" + new SnakeCase(entry.getKey()).camelToSnake());

            Map<String, Object> range = new HashMap<>();
            definition.put(Constants.OCCURRENCE, range);
            range.put(Constants.MIN_OP, ">=");
            range.put(Constants.MIN, 1);
            range.put(Constants.MAX_OP, "<=");
            range.put(Constants.MAX, 1);

            attributeList.add(definition);
        }
        return attributeList;
    }
}
