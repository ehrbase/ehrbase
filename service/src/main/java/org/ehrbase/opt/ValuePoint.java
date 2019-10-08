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

package org.ehrbase.opt;

import org.ehrbase.opt.mapper.Constants;
import org.ehrbase.opt.mapper.MandatoryAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 2/15/2018.
 */
public class ValuePoint {

    String rmTypeName;

    public ValuePoint(String rmTypeName) {
        this.rmTypeName = new Generic(rmTypeName).specialize();
    }

    //encode in map of value attribute name (ex 'magnitude') and type (ex 'INTEGER')
    public List<Map<String, String>> attributes(String... attributes) {

        if ((attributes.length % 2) != 0) //odd arguments
            throw new IllegalArgumentException("INTERNAL: Wrong number of arguments in value point definition");

        List<Map<String, String>> definitionList = new ArrayList<>();

        Map<String, String> attributeSpec = new HashMap<>();

        for (int i = 0; i < attributes.length; i++) {
            if ((i % 2) == 0) {//value name
//                attributeSpec.put(Constants.ATTRIBUTE, attributes[i]);
                attributeSpec.putAll(new AttributeDef(attributes[i]).naming());
            } else {
                attributeSpec.put(Constants.TYPE, attributes[i]);
                definitionList.add(attributeSpec);
                attributeSpec = new HashMap<>();
            }
        }

        return definitionList;

    }

    //encode in map of value attribute name (ex 'magnitude') and type (ex 'INTEGER')
    public List<Map<String, String>> attributes() {

        Map<String, Object> attributes = new MandatoryAttributes(rmTypeName).toMap();


        if (attributes.isEmpty()) //odd arguments
            return new ArrayList<>();

        List<Map<String, String>> definitionList = new ArrayList<>();

        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {

            Map<String, String> attributeSpec = new HashMap<>();

//            attributeSpec.put(Constants.ATTRIBUTE, new SnakeCase(attribute.getKey()).camelToSnake());

            attributeSpec.putAll(new AttributeDef(attribute.getKey()).naming());

            attributeSpec.put(Constants.TYPE, String.valueOf(((Map) attribute.getValue()).get("type")));

            definitionList.add(attributeSpec);
        }
        return definitionList;

    }

}
