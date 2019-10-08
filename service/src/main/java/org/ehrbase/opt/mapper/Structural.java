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

import org.ehrbase.opt.Generic;
import org.ehrbase.opt.NodeChildren;
import org.ehrbase.opt.NodeId;
import org.ehrbase.opt.TermDefinition;
import org.openehr.schemas.v1.CATTRIBUTE;
import org.openehr.schemas.v1.CCOMPLEXOBJECT;
import org.openehr.schemas.v1.CSINGLEATTRIBUTE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 1/31/2018.
 */
public class Structural {

    CCOMPLEXOBJECT ccomplexobject;
    Map<String, TermDefinition> termDef;
    Map<String, List<Map<String, Object>>> childrenNodeMap;
    String path, nodeId;
    String rmTypeName;
    String archetypeNodeId;

    public Structural(String rmTypeName, String archetypeNodeId, String path, String nodeId, CCOMPLEXOBJECT ccomplexobject, Map<String, TermDefinition> termDef, Map<String, List<Map<String, Object>>> childrenNodeMap) {
        this.ccomplexobject = ccomplexobject;
        this.termDef = termDef;
        this.childrenNodeMap = childrenNodeMap;
        this.path = path;
        this.nodeId = nodeId;
        this.rmTypeName = new Generic(rmTypeName).specialize();
        this.archetypeNodeId = archetypeNodeId;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> embedded = new NodeChildren(childrenNodeMap).include(path);

        if (embedded == null || embedded.isEmpty()) {
            return null;
        }

        //add mandatory attributes for this node
        Map<String, Object> mandatoryAttributes = new MandatoryAttributes(rmTypeName).toMap();

        if (!mandatoryAttributes.isEmpty())
            embedded.addAll(new AttributeList(mandatoryAttributes).toList(path));

        Map validationMap = new HashMap<>();

        if (!path.isEmpty())
            map.put(Constants.AQL_PATH, path);
        else {
            if (!"COMPOSITION".equals(rmTypeName))
                return null; //do not add mandatory attribute without a corresponding path
        }

        if (!validationMap.isEmpty())
            map.put(Constants.VALIDATION, validationMap);

        if (!nodeId.isEmpty()) {
            map.put(Constants.NODE_ID, nodeId);
            map.put(Constants.NAME, termDef.get(nodeId).getValue());
            map.put(Constants.ID, new NodeId(termDef.get(nodeId).getValue()).ehrscape());
            map.put(Constants.DESCRIPTION, termDef.get(nodeId).getDescription());
        }

        if (ccomplexobject.getOccurrences() != null) {
            map.put(Constants.MIN, ccomplexobject.getOccurrences().getLower() == 0 ?
                    (ccomplexobject.getOccurrences().getLowerUnbounded() == true ? -1 : 0) :
                    ccomplexobject.getOccurrences().getLower());
            map.put(Constants.MAX, ccomplexobject.getOccurrences().getUpper() == 0 ?
                    (ccomplexobject.getOccurrences().getUpperUnbounded() == true ? -1 : 0) :
                    ccomplexobject.getOccurrences().getUpper());

        }

        if (archetypeNodeId != null) {
            map.put(Constants.NODE_ID, archetypeNodeId);
        }

        Map rangeMap = new HashMap<>();

        for (CATTRIBUTE cattribute : ccomplexobject.getAttributesArray()) {
            if (cattribute.getRmAttributeName().equals(Constants.VALUE)) {
                validationMap.put(Constants.OCCURRENCE, rangeMap);
                rangeMap.put(Constants.MIN_OP, cattribute.getExistence().isSetLowerIncluded() ? ">=" : ">");
                rangeMap.put(Constants.MIN, cattribute.getExistence().isSetLower() ? cattribute.getExistence().getLower() : -1);
                rangeMap.put(Constants.MAX_OP, cattribute.getExistence().isSetUpperIncluded() ? "<=" : "<");
                rangeMap.put(Constants.MAX, cattribute.getExistence().isSetUpper() ? cattribute.getExistence().getUpper() : -1);
            } else if (cattribute instanceof CSINGLEATTRIBUTE && cattribute.getRmAttributeName().equals(Constants.NAME)) {
                //check if node name is overriden in the template
                String overridenName = new NodeNameAttribute(cattribute).staticName();
                if (overridenName != null) {
                    map.put(Constants.NAME, overridenName);
                    map.put(Constants.ID, new NodeId(overridenName).ehrscape());
                }
            }
        }

        map.put(Constants.CHILDREN, embedded);
        map.put(Constants.TYPE, rmTypeName);
        map.put(Constants.CATEGORY, Constants.LITTERAL_STRUCTURE);

        return map;
    }

    public Map<String, List<Map<String, Object>>> trim() {
        return new NodeChildren(childrenNodeMap).exclude(path);
    }
}
