/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
import org.ehrbase.jooq.dbencoding.CompositionSerializer;

/**
 * restructure special items containing a "value" key for canonical representation
 */
class EmbeddedValue {
    private LinkedTreeMap<String, Object> valueMap;

    private String[] embeddedTags = {
        CompositionSerializer.TAG_NARRATIVE,
        CompositionSerializer.TAG_MATH_FUNCTION,
        CompositionSerializer.TAG_WIDTH,
        CompositionSerializer.TAG_UID
    };

    EmbeddedValue(LinkedTreeMap<String, Object> valueMap) {
        this.valueMap = valueMap;
    }

    private LinkedTreeMap<String, Object> formatForTag(String tag) {

        if (valueMap.containsKey(tag)) {
            LinkedTreeMap<String, Object> treeMap = (LinkedTreeMap<String, Object>) valueMap.get(tag);
            // get the value
            if (!(treeMap.get(CompositionSerializer.TAG_VALUE) instanceof String)) {
                LinkedTreeMap treeMapValue = (LinkedTreeMap) treeMap.get(CompositionSerializer.TAG_VALUE);
                if (treeMapValue != null) treeMap.replace(CompositionSerializer.TAG_VALUE, treeMapValue.get("value"));
            }
        }

        return valueMap;
    }

    LinkedTreeMap<String, Object> formatForEmbeddedTag() {
        for (String tag : embeddedTags) {
            if (valueMap.containsKey(tag)) valueMap = formatForTag(tag);
        }
        return valueMap;
    }
}
