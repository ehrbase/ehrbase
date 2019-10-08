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

package org.ehrbase.ehr.encode.wrappers.json.writer.translator_db2raw;

import com.google.gson.internal.LinkedTreeMap;
import org.ehrbase.ehr.encode.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.serialisation.CompositionSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by christian on 8/8/2017.
 */
public class PathAttribute {

    public static final String AND_NAME_VALUE = "and name/value='";
    String path;

    public PathAttribute(String path) {
        this.path = path;
    }

    public PathAttribute() {
    }

    String nodeNameFromPath(String key) {
        String namePart = AND_NAME_VALUE;

        int startAt = -1;

        if (key.indexOf("]") >= 0) {
            startAt = path.indexOf(key.substring(0, key.indexOf("]")));
        }

        if (startAt >= 0) {
            return path.substring(path.indexOf(namePart, startAt) + namePart.length(), path.indexOf("]", startAt) - 1);
        }
        return null;
    }

    String parentArchetypeNodeId(String key) {
        if (path == null)
            return null;

        List<String> pathSegments = dividePathIntoSegments(path);
        String compareKey = key.trim();
        if (key.startsWith("/"))
            compareKey = compareKey.substring(1);
        if (compareKey.endsWith("]"))
            compareKey = compareKey.substring(0, compareKey.length() - 1);

        for (int i = 0; i < pathSegments.size(); i++) {
            String pathNode = pathSegments.get(i);
            if (pathNode.startsWith(compareKey)) {
                if (i > 0) {
                    String parentNode = pathSegments.get(i - 1);
                    String parentArchetypeNodeId;
                    if (parentNode.contains(AND_NAME_VALUE))
                        parentArchetypeNodeId = parentNode.substring(parentNode.indexOf("[") + 1, parentNode.indexOf(AND_NAME_VALUE)).trim();
                    else
                        parentArchetypeNodeId = parentNode.substring(parentNode.indexOf("[") + 1, parentNode.indexOf("]")).trim();
                    return parentArchetypeNodeId;
                } else
                    return null;
            }
        }
        return null;
    }

    String parentNodeName(String key) {
        if (path == null)
            return null;

        List<String> pathSegments = dividePathIntoSegments(path);
        String compareKey = key.trim();
        if (key.startsWith("/"))
            compareKey = compareKey.substring(1);
        if (compareKey.endsWith("]"))
            compareKey = compareKey.substring(0, compareKey.length() - 1);

        for (int i = 0; i < pathSegments.size(); i++) {
            String pathNode = pathSegments.get(i);
            if (pathNode.startsWith(compareKey)) {
                if (i > 0) {
                    String parentNode = pathSegments.get(i - 1);
                    String parentArchetypeNodeId = null;
                    if (parentNode.contains(AND_NAME_VALUE))
                        parentArchetypeNodeId = parentNode.substring(parentNode.indexOf(AND_NAME_VALUE) + AND_NAME_VALUE.length(), parentNode.indexOf("]") - 1).trim();
                    return parentArchetypeNodeId;
                } else
                    return null;
            }
        }
        return null;
    }

    String structuralNodeKey(LinkedTreeMap map) {
        for (Object entry : map.entrySet()) {
            String key = (String) ((Map.Entry) entry).getKey();
            if (key.matches(I_DvTypeAdapter.matchNodePredicate)) {
                return key;
            }
        }
        return null;
    }

    String findPath(Object value) {
        String foundPath = null;
        if (value instanceof List) {
            String path;
            for (Object listItem : (List) value) {
                path = findPath(listItem);
                if (path != null)
                    return path;
            }
        } else
            return usePath(foundPath, (LinkedTreeMap) value);
        return null;
    }

    String usePath(String path, LinkedTreeMap linkedTreeMap) {
        if (path != null)
            return path;

        if (linkedTreeMap.containsKey(CompositionSerializer.TAG_PATH))
            return linkedTreeMap.get(CompositionSerializer.TAG_PATH).toString();

        for (Object object : linkedTreeMap.entrySet()) {
            Map.Entry entry = (Map.Entry) object;
            String key = (String) ((Map.Entry) entry).getKey();
            if (!key.matches(I_DvTypeAdapter.matchNodePredicate)) {
                continue;
            }
            if (entry.getValue() instanceof List) {
                for (Object item : ((List) entry.getValue())) {
                    return usePath(path, (LinkedTreeMap) item);
                }
            }
            if (entry.getValue() instanceof LinkedTreeMap) {
                return usePath(path, ((LinkedTreeMap) entry.getValue()));
            } else {
                if ((entry.getKey()).equals(CompositionSerializer.TAG_PATH)) {
                    return entry.getValue().toString();
                }
            }
        }
        return null;
    }

    String archetypeNodeId() {

        String archetypeNodeId;

        if (path == null)
            return null;

        List<String> pathSegments = dividePathIntoSegments(path);
        String lastSegment = pathSegments.get(pathSegments.size() - 1);

        if (!lastSegment.contains("[")) //attribute
            return null;

        if (lastSegment.contains(AND_NAME_VALUE))
            archetypeNodeId = lastSegment.substring(lastSegment.indexOf("[") + 1, lastSegment.indexOf(AND_NAME_VALUE)).trim();
        else
            archetypeNodeId = lastSegment.substring(lastSegment.indexOf("[") + 1, lastSegment.indexOf("]")).trim();

        return archetypeNodeId;
    }

    public static List<String> dividePathIntoSegments(String path) {
        List<String> segments = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(path, "/");
        while (tokens.hasMoreTokens()) {
            String next = tokens.nextToken();
            if (next.matches(".+\\[.+[^\\]]$")) {
                do {
                    next = next + "/" + tokens.nextToken();
                } while (!next.matches(".*]$"));
            }
            segments.add(next);
        }
        return segments;
    }
}
