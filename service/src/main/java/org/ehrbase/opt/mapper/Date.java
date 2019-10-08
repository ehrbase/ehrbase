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
public class Date {

    static final String type = "DV_DATE";

    CCOMPLEXOBJECT ccomplexobject;
    Map<String, TermDefinition> termDef;

    public Date(CCOMPLEXOBJECT ccomplexobject, Map<String, TermDefinition> termDef) {
        this.ccomplexobject = ccomplexobject;
        this.termDef = termDef;
    }

    public Map<String, Object> toMap(String name) {

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(Constants.TYPE, type);
        attributeMap.put(Constants.ATTRIBUTE_NAME, name);
        attributeMap.put(Constants.MANDATORY_ATTRIBUTES, new ValuePoint(type).attributes());

        List valueList = new ArrayList<>();

        Map<String, Object> constraintsMap = new HashMap<>();

        attributeMap.put(Constants.CONSTRAINT, constraintsMap);

        for (CATTRIBUTE attribute : ccomplexobject.getAttributesArray()) {
            if (attribute.getRmAttributeName().equals(Constants.VALUE)) {
                for (COBJECT cobject : attribute.getChildrenArray()) {
                    if (cobject.getRmTypeName().equals("DATE")) {
                        constraintsMap.put(Constants.NAME, Constants.VALUE);
                        //get the range and set it in constraints
                        CPRIMITIVEOBJECT cprimitiveobject = (CPRIMITIVEOBJECT) cobject;
                        CDATE cprimitive = (CDATE) cprimitiveobject.getItem();
                        if (cprimitive.isSetRange()) {
                            if (cprimitive.getRange().isSetLower())
                                constraintsMap.put(Constants.MIN, cprimitive.getRange().getLower());
                            if (cprimitive.getRange().isSetUpper())
                                constraintsMap.put(Constants.MAX, cprimitive.getRange().getUpper());
                        }
                        if (cprimitive.isSetAssumedValue())
                            constraintsMap.put(Constants.ASSUMED_VALUE, cprimitive.getAssumedValue());
                        if (cprimitive.getPattern() != null) {
                            constraintsMap.put(Constants.PATTERN, cprimitive.getPattern());
                        }
                    }
                }
            }
        }

        Map<String, Object> range = new HashMap<>();

        constraintsMap.put(Constants.OCCURRENCE, range);
        range.put(Constants.MIN_OP, ccomplexobject.getOccurrences().isSetLowerIncluded() ? ">=" : ">");
        range.put(Constants.MIN, ccomplexobject.getOccurrences().isSetLower() ? ccomplexobject.getOccurrences().getLower() : -1);
        range.put(Constants.MAX_OP, ccomplexobject.getOccurrences().isSetUpperIncluded() ? "<=" : "<");
        range.put(Constants.MAX, ccomplexobject.getOccurrences().isSetUpper() ? ccomplexobject.getOccurrences().getUpper() : -1);
        valueList.add(attributeMap);

        return attributeMap;
    }
}
