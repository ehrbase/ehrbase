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

import java.util.List;
import java.util.Map;

/**
 * Created by christian on 2/15/2018.
 */
public class ValueItem {

    List<Map<String, Object>> embedded;

    public ValueItem(List<Map<String, Object>> embedded) {
        this.embedded = embedded;
    }

    public String path() {
        for (Map<String, Object> valueMap : embedded) {
            //get the value_point definition (we need 'name')
            //get value_point
            if (valueMap.containsKey(Constants.MANDATORY_ATTRIBUTES)) {
                List<Map<String, String>> defMap = (List<Map<String, String>>) valueMap.get(Constants.MANDATORY_ATTRIBUTES);

                for (Map<String, String> definition : defMap) {
                    if (definition.containsKey(Constants.NAME)) {
                        return definition.get(Constants.NAME);
                    }
                }

            }
        }
        return "";
    }
}
