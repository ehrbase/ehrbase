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

import java.util.List;
import java.util.Map;

/**
 * Created by christian on 2/19/2018.
 */
public class Attribute {

    Object attributeList;

    public Attribute(Object attributeList) {
        this.attributeList = attributeList;
    }

    public void associate(String id, String key, Object value) {
        if (attributeList == null)
            return;
        List<Map<String, Object>> attributes = (List<Map<String, Object>>) attributeList;

        for (Map<String, Object> attributeMap : attributes) {
            if (attributeMap.containsKey(Constants.VALUE_MAP)) {
                for (Map<String, Object> map : (List<Map<String, Object>>) attributeMap.get(Constants.VALUE_MAP)) {
                    if (map.containsKey(id))
                        map.put(key, value);
                }
            }
        }
        return;
    }
}
