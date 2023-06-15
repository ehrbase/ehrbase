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
import java.util.Map;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;

/**
 * deals with representation issues required to support AQL at DB level but causing wrong structuration when
 * returning canonical json. For example
 * <code>
 * /events: {
 * /events[at0002]: [
 * ...
 * ]
 * }
 * </code>
 * <p>
 * Should be return as:
 *
 * <code>
 * {"events":[...,"archetype_node_id":"at0002"}]}
 * </code>
 * <p>
 * The same logic applies to ACTIVITIES
 */
class IterativeItemStructure {

    private LinkedTreeMap<String, Object> valueMap;

    private String[] iterativeTags = {CompositionSerializer.TAG_ACTIVITIES, CompositionSerializer.TAG_EVENTS};

    IterativeItemStructure(LinkedTreeMap<String, Object> valueMap) {
        this.valueMap = valueMap;
    }

    LinkedTreeMap<String, Object> promoteIterations() {
        for (String iterativeTag : iterativeTags) {
            if (valueMap.containsKey(iterativeTag)) {
                LinkedTreeMap<String, Object> activities = (LinkedTreeMap<String, Object>) valueMap.get(iterativeTag);
                for (Map.Entry<String, Object> activityItem : activities.entrySet()) {
                    if (activityItem.getKey().startsWith(iterativeTag)) {
                        valueMap.put(activityItem.getKey(), activityItem.getValue());
                    }
                }
                valueMap.remove(iterativeTag);
            }
        }
        return valueMap;
    }
}
