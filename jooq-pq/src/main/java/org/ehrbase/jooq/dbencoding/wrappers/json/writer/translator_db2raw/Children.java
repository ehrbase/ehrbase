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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;

/** Created by christian on 3/13/2018. */
public class Children {

    LinkedTreeMap<String, Object> linkedTreeMap;

    public Children(LinkedTreeMap<String, Object> linkedTreeMap) {
        this.linkedTreeMap = linkedTreeMap;
    }

    public boolean isItemsOnly() {

        if (linkedTreeMap.keySet().stream()
                        .filter(s -> s.startsWith(CompositionSerializer.TAG_ITEMS))
                        .collect(Collectors.toSet())
                        .size()
                == 0) return false;

        for (String key : linkedTreeMap.keySet()) {
            if (!key.startsWith(CompositionSerializer.TAG_ITEMS)
                    && !key.equals(I_DvTypeAdapter.ARCHETYPE_NODE_ID)
                    && !key.equals(I_DvTypeAdapter.AT_CLASS)
                    && !isTrivialLocatableAttribute(key)
                    && !key.equals(CompositionSerializer.TAG_CLASS)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTrivialLocatableAttribute(String key) {
        return (key.equals(CompositionSerializer.TAG_ARCHETYPE_NODE_ID)
                || key.equals(CompositionSerializer.TAG_ARCHETYPE_DETAILS)
                || key.equals(CompositionSerializer.TAG_NAME));
    }

    public int itemsCount() {
        int count = 0;

        for (String key : linkedTreeMap.keySet()) {
            if (key.startsWith(CompositionSerializer.TAG_ITEMS)) count++;
        }
        return count;
    }

    public int eventsCount() {
        int count = 0;

        for (String key : linkedTreeMap.keySet()) {
            if (key.startsWith(CompositionSerializer.TAG_EVENTS)) count++;
        }
        return count;
    }

    // check for multiple items in content
    public boolean isMultiContent() {

        if (!containsKeyStartingWith(CompositionSerializer.TAG_CONTENT)) return false;

        int contents = 0;

        for (String key : linkedTreeMap.keySet()) {
            if (key.startsWith(CompositionSerializer.TAG_CONTENT)) {
                contents++;
            }
        }
        return contents > 1;
    }

    public int contentCount() {
        int contents = 0;

        for (String key : linkedTreeMap.keySet()) {
            if (key.startsWith(CompositionSerializer.TAG_CONTENT)) {
                contents++;
            }
        }
        return contents;
    }

    public boolean isMultiData() {
        int data = 0;

        for (String key : linkedTreeMap.keySet()) {
            if (key.startsWith(CompositionSerializer.TAG_DATA)) {
                data++;
            }
        }
        return data > 1;
    }

    public boolean isEvents() {
        if (linkedTreeMap.keySet().stream()
                        .filter(s -> s.startsWith(CompositionSerializer.TAG_EVENTS))
                        .collect(Collectors.toSet())
                        .size()
                == 0) return false;

        int isEvents = 0;

        for (String key : linkedTreeMap.keySet()) {
            if (key.startsWith(CompositionSerializer.TAG_EVENTS)) {
                isEvents++;
            }
        }
        return isEvents > 1;
    }

    public String type() {

        if (linkedTreeMap.containsKey(CompositionSerializer.TAG_CLASS)) {
            return (String) linkedTreeMap.get(CompositionSerializer.TAG_CLASS);
        }

        return "*UNDEF*";
    }

    public ArrayList items() {

        ArrayList items = new ArrayList();

        for (String key : linkedTreeMap.keySet()) {
            if (key.startsWith(CompositionSerializer.TAG_ITEMS)) {
                String archetypeNodeId = new NodeId(key).predicate();
                Object e = linkedTreeMap.get(key);
                if (List.class.isAssignableFrom(e.getClass())) {
                    ((List) e)
                            .stream()
                                    .filter(o -> Map.class.isAssignableFrom(o.getClass()))
                                    .forEach(m -> ((Map) m).put(I_DvTypeAdapter.ARCHETYPE_NODE_ID, archetypeNodeId));
                } else if (Map.class.isAssignableFrom(e.getClass())) {
                    ((Map) e).put(I_DvTypeAdapter.ARCHETYPE_NODE_ID, archetypeNodeId);
                }
                items.add(e);
            }
        }

        return items;
    }

    public ArrayList events() {

        ArrayList events = new ArrayList();

        for (String key : linkedTreeMap.keySet()) {
            if (key.startsWith(CompositionSerializer.TAG_EVENTS)) events.add(linkedTreeMap.get(key));
        }

        return events;
    }

    public ArrayList contents() {
        ArrayList contents = new ArrayList();

        for (String key : linkedTreeMap.keySet()) {
            if (key.startsWith(CompositionSerializer.TAG_CONTENT)) contents.add(linkedTreeMap.get(key));
        }

        return contents;
    }

    public LinkedTreeMap removeContents() {
        LinkedTreeMap retMap = new LinkedTreeMap();
        for (String key : linkedTreeMap.keySet()) {
            if (!key.startsWith(CompositionSerializer.TAG_CONTENT)) retMap.put(key, linkedTreeMap.get(key));
        }
        return retMap;
    }

    public LinkedTreeMap removeDuplicateArchetypeNodeId() {
        LinkedTreeMap retMap = new LinkedTreeMap();
        retMap.putAll(linkedTreeMap);
        if (linkedTreeMap.containsKey(I_DvTypeAdapter.ARCHETYPE_NODE_ID)
                && linkedTreeMap.containsKey(CompositionSerializer.TAG_ARCHETYPE_NODE_ID))
            retMap.remove(CompositionSerializer.TAG_ARCHETYPE_NODE_ID);
        return retMap;
    }

    private boolean containsKeyStartingWith(String key) {
        return linkedTreeMap.keySet().stream()
                        .filter(s -> s.startsWith(key))
                        .collect(Collectors.toSet())
                        .size()
                > 0;
    }
}
