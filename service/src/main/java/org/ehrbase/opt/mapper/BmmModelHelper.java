/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
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

import org.openehr.bmm.core.BmmClass;
import org.openehr.bmm.core.BmmModel;
import org.openehr.bmm.core.BmmProperty;
import org.openehr.referencemodels.BuiltinReferenceModels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BmmModelHelper {

    private static final BmmModel MODEL = BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel();
    private static final String PARTY_PROXY = "PARTY_PROXY";

    private BmmModelHelper() {
    }

    static BmmClass getBmmClassForName(String typeName) {
        return MODEL.getClassDefinition(typeName);
    }

    static Map<String, Object> getRequiredAttributes(BmmClass clazz) {

        if (clazz.getName().equals(PARTY_PROXY) || clazz.getAncestors().values().stream().anyMatch(c -> c.getName().equals(PARTY_PROXY))) {
            clazz = getBmmClassForName("PARTY_REF");
        }

        Map<String, Object> retmap = new HashMap<String, Object>();
        List<BmmProperty> properties = new ArrayList<BmmProperty>();
        properties.addAll(clazz.getProperties().values());
        clazz.getAncestors().values().forEach(c -> properties.addAll(c.getProperties().values()));

        for (BmmProperty attr : properties) {
            if (attr.getMandatory()) {
                //check if field is a java primitive or openehr datatype

                if (attr.getType().getBaseClass().isPrimitiveType()) {
                    Map<String, Object> defMap = new HashMap<String, Object>();
                    retmap.put(attr.getName(), defMap);
                    defMap.put("type", attr.getType().getBaseClass().getName().toUpperCase());
                } else {
                    Map<String, Object> submap = getRequiredAttributes(attr.getType().getBaseClass());
                    submap.put("type", attr.getType().getBaseClass().getName());
                    retmap.put(attr.getName(), submap);
                }
            }
        }

        return retmap;
    }
}