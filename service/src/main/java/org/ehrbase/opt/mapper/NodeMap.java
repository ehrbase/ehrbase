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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by christian on 1/31/2018.
 */
public class NodeMap {

    Map<String, TermDefinition> termDef;
    String nodeId;

    public NodeMap(Map<String, TermDefinition> termDef, String nodeId) {
        this.termDef = termDef;
        this.nodeId = nodeId;
    }

    public Map toMap(int min, int max, String path, String archetypeNodeId, String rmTypeName) {

        Map nodeMap = new HashMap<>();

        TermDefinition definition = termDef.get(nodeId);

        nodeMap.put(Constants.MIN, min);
        nodeMap.put(Constants.MAX, max);
        nodeMap.put(Constants.NAME, definition == null ? null : definition.getValue());
        nodeMap.put(Constants.AQL_PATH, path);
        nodeMap.put(Constants.NODE_ID, archetypeNodeId == null ? nodeId : archetypeNodeId);
        nodeMap.put(Constants.RM_TYPE, rmTypeName);
        nodeMap.put(Constants.DESCRIPTION, definition == null ? null : definition.getDescription());

        return nodeMap;
    }
}
