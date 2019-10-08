/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 2/2/2018.
 */
public class NodeChildren {

    Map<String, List<Map<String, Object>>> childrenNodeMap;

    public NodeChildren(Map<String, List<Map<String, Object>>> childrenNodeMap) {
        this.childrenNodeMap = childrenNodeMap;
    }

    public List<Map<String, Object>> include(String path) {
        List<Map<String, Object>> retlist = new ArrayList<>();

        for (String childPath : childrenNodeMap.keySet()) {

            if (childPath.startsWith(path)) {
                retlist.addAll(childrenNodeMap.get(childPath));
            }
        }

        return retlist;
    }

    public Map<String, List<Map<String, Object>>> exclude(String path) {
        Map<String, List<Map<String, Object>>> retmap = new HashMap<>();

        for (String childPath : childrenNodeMap.keySet()) {
            if (!childPath.startsWith(path)) {
                retmap.put(childPath, childrenNodeMap.get(childPath));
            }
        }

        return retmap;
    }
}
