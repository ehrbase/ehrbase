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

import org.ehrbase.opt.TermDefinition;
import org.ehrbase.opt.ValuePoint;
import org.openehr.schemas.v1.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 1/31/2018.
 */
public class Interval {

    static final String type = "DV_INTERVAL";

    CCOMPLEXOBJECT ccomplexobject;
    Map<String, TermDefinition> termDef;

    public Interval(CCOMPLEXOBJECT ccomplexobject, Map<String, TermDefinition> termDef) {
        this.ccomplexobject = ccomplexobject;
        this.termDef = termDef;
    }

    public Map<String, Object> toMap(String name) {

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(Constants.TYPE, type);
        attributeMap.put(Constants.ATTRIBUTE_NAME, name);

        List valueList = new ArrayList<>();

        Map<String, Object> constraintsMap = new HashMap<>();

        attributeMap.put(Constants.CONSTRAINT, constraintsMap);

//        List<Map<String, Object>> specMap = new ArrayList<>();

//        constraintsMap.put(Constants.VALUE_MAP, specMap);

        List<String> valuePoints = new ArrayList<>();

        for (CATTRIBUTE attribute : ccomplexobject.getAttributesArray()) {
            Map<String, Object> map = new HashMap<>();

            if (attribute.getRmAttributeName().equals(Constants.UPPER)) {
                for (COBJECT cobject : attribute.getChildrenArray()) {
                    putInnerType(constraintsMap, cobject, Constants.UPPER);
                    //get the type
                    valuePoints.add(Constants.UPPER);
                    valuePoints.add((String) ((Map) constraintsMap.get(Constants.UPPER)).get(Constants.TYPE));
//                    if (childObject == null)
//                        childObject = cobject;
                }
            } else if (attribute.getRmAttributeName().equals(Constants.LOWER)) {
                for (COBJECT cobject : attribute.getChildrenArray()) {
                    putInnerType(constraintsMap, cobject, Constants.LOWER);
                    valuePoints.add(Constants.LOWER);
                    valuePoints.add((String) ((Map) constraintsMap.get(Constants.LOWER)).get(Constants.TYPE));
                }
            }

//            if (childObject != null)
//                map.put(Constants.MANDATORY_ATTRIBUTES, new ValuePoint().attributes(valuePointType(childObject)));

//            specMap.add(map);
        }

        attributeMap.put(Constants.MANDATORY_ATTRIBUTES, new ValuePoint(type).attributes(valuePoints.toArray(new String[]{})));

        Map<String, Object> range = new HashMap<>();

        constraintsMap.put(Constants.OCCURRENCE, range);
        range.put(Constants.MIN_OP, ccomplexobject.getOccurrences().isSetLowerIncluded() ? ">=" : ">");
        range.put(Constants.MIN, ccomplexobject.getOccurrences().isSetLower() ? ccomplexobject.getOccurrences().getLower() : -1);
        range.put(Constants.MAX_OP, ccomplexobject.getOccurrences().isSetUpperIncluded() ? "<=" : "<");
        range.put(Constants.MAX, ccomplexobject.getOccurrences().isSetUpper() ? ccomplexobject.getOccurrences().getUpper() : -1);
        valueList.add(attributeMap);

        return attributeMap;
    }

    private void putInnerType(Map<String, Object> map, COBJECT cobject, String attributeName) {
        if (cobject.getRmTypeName().equals("DV_QUANTITY")) {
            //get the range and set it in constraints
            map.put(attributeName, new Quantity((CDVQUANTITY) cobject, termDef).toMap(attributeName));
        } else if (cobject.getRmTypeName().equals("DV_COUNT")) {
            //get the range and set it in constraints
            map.put(attributeName, new Count((CCOMPLEXOBJECT) cobject, termDef).toMap(attributeName));
        } else if (cobject.getRmTypeName().equals("DV_DATE") || cobject.getRmTypeName().equals("DV_DATE_TIME") || cobject.getRmTypeName().equals("DV_TIME")) {
            //get the range and set it in constraints
            map.put(attributeName, new ValueType((CCOMPLEXOBJECT) cobject, termDef).toMap(cobject.getRmTypeName(), attributeName));
        } else if (cobject.getRmTypeName().equals("DV_DURATION")) {
            //get the range and set it in constraints
            map.put(attributeName, new Duration((CCOMPLEXOBJECT) cobject, termDef).toMap(attributeName));
        } else if (cobject.getRmTypeName().equals("DV_ORDINAL")) {
            //get the range and set it in constraints
            map.put(attributeName, new Ordinal((CDVORDINAL) cobject, termDef).toMap(attributeName));
        } else if (cobject.getRmTypeName().equals("DV_PROPORTION")) {
            //get the range and set it in constraints
            map.put(attributeName, new Proportion((CCOMPLEXOBJECT) cobject, termDef).toMap(attributeName));
        }
    }

    private String[] valuePointType(COBJECT cobject) {
        List<String> arraySpec = new ArrayList<>();

        if (cobject.getRmTypeName().equals("DV_QUANTITY") || cobject.getRmTypeName().equals("DV_COUNT")) {
            //get the range and set it in constraints
            arraySpec.add(Constants.MAGNITUDE);
            arraySpec.add(Constants.REAL);
        } else if (cobject.getRmTypeName().equals("DV_DATE") || cobject.getRmTypeName().equals("DV_DATE_TIME") || cobject.getRmTypeName().equals("DV_TIME") || cobject.getRmTypeName().equals("DV_DURATION")) {
            arraySpec.add(Constants.VALUE);
            arraySpec.add(Constants.STRING);
        } else if (cobject.getRmTypeName().equals("DV_ORDINAL")) {
            arraySpec.add(Constants.VALUE);
            arraySpec.add(Constants.INTEGER);
        } else if (cobject.getRmTypeName().equals("DV_PROPORTION")) {
            arraySpec.add(Constants.LOWER);
            arraySpec.add(Constants.REAL);
            arraySpec.add(Constants.UPPER);
            arraySpec.add(Constants.REAL);
        }

        return arraySpec.toArray(new String[]{});
    }
}
