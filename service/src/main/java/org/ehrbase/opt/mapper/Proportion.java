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
import com.nedap.archie.rm.datavalues.quantity.ProportionKind;
import org.openehr.schemas.v1.*;

import java.util.*;

/**
 * Created by christian on 1/31/2018.
 */
public class Proportion {

    static final String type = "DV_PROPORTION";

    CCOMPLEXOBJECT ccomplexobject;
    Map<String, TermDefinition> termDef;

    public Proportion(CCOMPLEXOBJECT ccomplexobject, Map<String, TermDefinition> termDef) {
        this.ccomplexobject = ccomplexobject;
        this.termDef = termDef;
    }

    public Map<String, Object> toMap(String name) {

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(Constants.TYPE, type);
        attributeMap.put(Constants.ATTRIBUTE_NAME, name);
//        attributeMap.put(Constants.MANDATORY_ATTRIBUTES, new ValuePoint(type).attributes(Constants.DENOMINATOR, Constants.REAL, Constants.NUMERATOR, Constants.REAL, Constants.TYPE, Constants.INTEGER));
        attributeMap.put(Constants.MANDATORY_ATTRIBUTES, new ValuePoint(type).attributes());

        List valueList = new ArrayList<>();

        Map<String, Object> constraintsMap = new HashMap<>();

        attributeMap.put(Constants.CONSTRAINT, constraintsMap);

        List<Map<String, Object>> specMap = new ArrayList<>();

        constraintsMap.put(Constants.TYPE, specMap);

        for (CATTRIBUTE attribute : ccomplexobject.getAttributesArray()) {
            Map<String, Object> map = new HashMap<>();

            String attributeName = attribute.getRmAttributeName();

            if (attribute.getRmAttributeName().equals(Constants.NUMERATOR)) {

                for (COBJECT cobject : attribute.getChildrenArray()) {
                    if (cobject.getRmTypeName().equals("REAL")) {
                        //get the range and set it in constraints

                        map.put(Constants.TYPE, "REAL");
                        CPRIMITIVEOBJECT cprimitiveobject = (CPRIMITIVEOBJECT) cobject;
                        CREAL cprimitive = (CREAL) cprimitiveobject.getItem();
                        if (cprimitive.isSetRange()) {
                            if (cprimitive.getRange().isSetLower())
                                map.put(Constants.MIN, cprimitive.getRange().getLower());
                            if (cprimitive.getRange().isSetUpper())
                                map.put(Constants.MAX, cprimitive.getRange().getUpper());
                        }
                    }
                }
            } else if (attribute.getRmAttributeName().equals(Constants.DENOMINATOR)) {

                for (COBJECT cobject : attribute.getChildrenArray()) {
                    if (cobject.getRmTypeName().equals("REAL")) {
                        //get the range and set it in constraints
                        map.put(Constants.NAME, Constants.DENOMINATOR);
                        map.put(Constants.TYPE, "REAL");

                        CPRIMITIVEOBJECT cprimitiveobject = (CPRIMITIVEOBJECT) cobject;
                        CREAL cprimitive = (CREAL) cprimitiveobject.getItem();
                        if (cprimitive.isSetRange()) {
                            if (cprimitive.getRange().isSetLower())
                                map.put(Constants.MIN, cprimitive.getRange().getLower());
                            if (cprimitive.getRange().isSetUpper())
                                map.put(Constants.MAX, cprimitive.getRange().getUpper());
                        }
                    }
                }
            } else if (attribute.getRmAttributeName().equals(Constants.TYPE)) {

                for (COBJECT cobject : attribute.getChildrenArray()) {
                    if (cobject.getRmTypeName().equals("INTEGER")) {
                        //get the range and set it in constraints
//                        map.put(Constants.MANDATORY_ATTRIBUTES, Constants.TYPE);
                        map.put(Constants.DESCRIPTION, "Indicates semantic type of proportion");
                        map.put(Constants.TYPE, "INTEGER");

                        CPRIMITIVEOBJECT cprimitiveobject = (CPRIMITIVEOBJECT) cobject;
                        CINTEGER cprimitive = (CINTEGER) cprimitiveobject.getItem();
                        if (cprimitive.getListArray().length > 0) {
                            List<Map<String, Object>> kindList = new ArrayList<>();
                            for (int kind : cprimitive.getListArray()) {
                                Map<String, Object> proportionKind = new HashMap<>();
                                proportionKind.put(Constants.VALUE, kind);
                                proportionKind.put(Constants.LABEL,
                                        Arrays.stream(ProportionKind.values())
                                                .filter(k -> k.getPk() == kind)
                                                .findAny()
                                                .map(p -> p.name())
                                                .orElseThrow(() -> new RuntimeException("Unknown ProportionKind " + kind)));
                                kindList.add(proportionKind);
                            }
                            map.put(Constants.VALUES, kindList);

                        }

                        if (cprimitive.isSetRange()) {
                            if (cprimitive.getRange().isSetLower())
                                map.put(Constants.MIN, cprimitive.getRange().getLower());
                            if (cprimitive.getRange().isSetUpper())
                                map.put(Constants.MAX, cprimitive.getRange().getUpper());
                        }
                    }
                }
            }

            if (!map.isEmpty())
                constraintsMap.put(attributeName, map);

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
