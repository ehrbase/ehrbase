/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.jooq.dbencoding.wrappers.json.writer.translator_db2raw;

import com.google.gson.internal.LinkedTreeMap;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;

/**
 * Deals with specialization of DataValues (f.e. DV_INTERVAL<DV_QUANTITY>)
 */
public class GenericRmType {

    private String type;

    public GenericRmType(String type) {
        this.type = type;
    }

    /**
     * check if type is specialized
     *
     * @return true if the type is specialized
     */
    public boolean isSpecialized() {
        return type.contains("<") && type.contains(">");
    }

    /**
     * identify main RmType (f.e. DV_INTERVAL)
     *
     * @return the main type
     */
    public String mainType() {
        if (!isSpecialized()) return type;
        return type.substring(0, type.indexOf("<"));
    }

    /**
     * identify the RmType used to specialized the type
     * f.e. DV_QUANTITY in DV_INTERVAL<DV_QUANTITY>
     *
     * @return the specialized type or null
     */
    public String specializedWith() {
        if (!isSpecialized()) return null;

        return type.substring(type.indexOf("<") + 1, type.indexOf(">"));
    }

    /**
     * add the type in each datavalues for this instance.
     * F.e. add the _type for lower and upper in an interval
     *
     * @param valueMap the built valueMap for canonical json encoding
     * @return the updated valueMap
     */
    @SuppressWarnings("java:S2864")
    LinkedTreeMap<String, Object> inferSpecialization(LinkedTreeMap<String, Object> valueMap) {

        if (!isSpecialized()) // do nothing
        return valueMap;

        for (String key : valueMap.keySet()) {
            Object entry = valueMap.get(key);
            // entry is either a Map or an Array
            if (entry instanceof Map) {
                // add the type
                ((Map<String, Object>) entry).put(I_DvTypeAdapter.TAG_CLASS_RAW_JSON, specializedWith());
            }
        }

        // remove the specialization for canonical rendering
        String valueType = (String) valueMap.get(I_DvTypeAdapter.TAG_CLASS_RAW_JSON);
        valueMap.put(I_DvTypeAdapter.TAG_CLASS_RAW_JSON, valueType.split("<")[0]);

        return valueMap;
    }
}
