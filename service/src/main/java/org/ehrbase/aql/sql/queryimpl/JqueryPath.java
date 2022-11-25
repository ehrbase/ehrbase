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
package org.ehrbase.aql.sql.queryimpl;

import static org.ehrbase.aql.sql.queryimpl.JsonbEntryQuery.*;
import static org.ehrbase.aql.sql.queryimpl.attribute.eventcontext.EventContextResolver.OTHER_CONTEXT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.ehrbase.aql.sql.queryimpl.value_field.NodePredicate;
import org.ehrbase.ehr.util.LocatableHelper;

public final class JqueryPath {

    private final JsonbEntryQuery.PATH_PART pathPart;
    private final String path;
    private final String defaultIndex;

    private static final String[] listIdentifier = {TAG_CONTENT, TAG_ITEMS, TAG_ACTIVITIES, TAG_EVENTS};

    private static final Pattern INDEX_PATTERN = Pattern.compile("[0-9]*|#");

    public JqueryPath(JsonbEntryQuery.PATH_PART pathPart, String path, String defaultIndex) {
        this.pathPart = pathPart;
        this.path = path;
        this.defaultIndex = defaultIndex;
    }

    public List<String> evaluate() {
        return evaluate(pathPart, path, defaultIndex);
    }

    public static List<String> evaluate(JsonbEntryQuery.PATH_PART pathPart, String path, String defaultIndex) {

        // CHC 160607: this offset (1 or 0) was required due to a bug in generating the containment table
        // from a PL/pgSQL script. this is no more required

        if (path == null) { // partial path
            return new ArrayList<>();
        }

        int offset = 0;
        List<String> segments = LocatableHelper.dividePathIntoSegments(path);
        List<String> jqueryPath = new ArrayList<>();
        for (int i = offset; i < segments.size(); i++) {
            String nodeId = segments.get(i);
            if (!nodeId.equals(OTHER_CONTEXT.substring(1))) {
                nodeId = "/" + nodeId;
            }

            encodeTreeMapNodeId(jqueryPath, nodeId);

            NodePredicate nodePredicate = new NodePredicate(nodeId);
            //
            if (nodePredicate.hasPredicate()) {
                // do the formatting to allow name/value node predicate processing
                jqueryPath = new NodeNameValuePredicate(nodePredicate).path(jqueryPath, nodeId);
            } else {
                nodeId = nodePredicate.removeNameValuePredicate();
                jqueryPath.add(nodeId);
            }

            if (isList(nodeId)) {
                jqueryPath.add(defaultIndex);
            }
        }

        if (pathPart.equals(JsonbEntryQuery.PATH_PART.VARIABLE_PATH_PART)) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = jqueryPath.size() - 1; i >= 0; i--) {
                if (INDEX_PATTERN.matcher(jqueryPath.get(i)).matches()
                        || jqueryPath.get(i).contains("[")
                        || jqueryPath.get(i).startsWith("'")) break;
                String item = jqueryPath.remove(i);
                stringBuilder.insert(0, item);
            }
            String nodeId = EntryAttributeMapper.map(stringBuilder.toString());
            if (nodeId != null) {
                if (defaultIndex.equals("#")) { // jsquery
                    if (nodeId.contains(",")) {
                        String[] parts = nodeId.split(",");
                        jqueryPath.addAll(Arrays.asList(parts));
                    } else {
                        jqueryPath.add(nodeId);
                    }
                } else {
                    jqueryPath.add(nodeId);
                }
            }
        }

        return jqueryPath;
    }

    // deals with special tree based entities
    // this is required to encode structure like events of events
    // the same is applicable to activities. These are in fact pseudo arrays.
    private static void encodeTreeMapNodeId(List<String> jqueryPath, String nodeId) {
        if (nodeId.startsWith(TAG_EVENTS)) {
            // this is an exception since events are represented in an event tree
            jqueryPath.add(TAG_EVENTS);
        } else if (nodeId.startsWith(TAG_ACTIVITIES)) {
            jqueryPath.add(TAG_ACTIVITIES);
        }
    }

    private static boolean isList(String predicate) {
        if (predicate.equals(TAG_ACTIVITIES)) return false;
        for (String identifier : listIdentifier) if (predicate.startsWith(identifier)) return true;
        return false;
    }
}
