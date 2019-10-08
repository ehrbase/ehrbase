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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by christian on 2/14/2018.
 */
public class EventContextAttributes {

    static final String EVENT_CONTEXT = "EVENT_CONTEXT";
    public static final String CONTEXT_PATH = "/context";

    public Map<String, Object> toMap() {
        Map<String, Object> eventContextMap = new HashMap<>();

//        eventContextMap.put(Constants.ATTRIBUTE, Constants.CONTEXT);
        eventContextMap.putAll(new AttributeDef(Constants.CONTEXT).naming());
        eventContextMap.put(Constants.RM_TYPE, EVENT_CONTEXT);
        eventContextMap.put(Constants.NODE_ID, "");
        eventContextMap.put(Constants.MAX, 1);
        eventContextMap.put(Constants.MIN, 1);
        eventContextMap.put(Constants.AQL_PATH, CONTEXT_PATH);
        eventContextMap.put(Constants.CHILDREN, new AttributeList(new MandatoryAttributes(EVENT_CONTEXT).toMap()).toList(CONTEXT_PATH));

        return eventContextMap;
    }
}
