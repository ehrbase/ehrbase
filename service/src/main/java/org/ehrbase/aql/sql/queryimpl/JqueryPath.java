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
import org.ehrbase.aql.sql.queryimpl.value_field.NodePredicate;
import org.ehrbase.ehr.util.LocatableHelper;

public class JqueryPath {

    private final JsonbEntryQuery.PATH_PART pathPart;
    private final String path;
    private final String defaultIndex;

    private static final String[] listIdentifier = {TAG_CONTENT, TAG_ITEMS, TAG_ACTIVITIES, TAG_EVENTS};

    public JqueryPath(JsonbEntryQuery.PATH_PART pathPart, String path, String defaultIndex) {
        this.pathPart = pathPart;
        this.path = path;
        this.defaultIndex = defaultIndex;
    }

    public List<String> evaluate() {

        // CHC 160607: this offset (1 or 0) was required due to a bug in generating the containment table
        // from a PL/pgSQL script. this is no more required

        if (path == null) { // partial path
            return new ArrayList<>();
        }

        int offset = 0;
        List<String> segments = LocatableHelper.dividePathIntoSegments(path);
        List<String> jqueryPath = new ArrayList<>();
        String nodeId = null;
        for (int i = offset; i < segments.size(); i++) {
            nodeId = segments.get(i);
            nodeId = nodeId.equals(OTHER_CONTEXT.substring(1)) ? nodeId : "/" + nodeId;

            encodeTreeMapNodeId(jqueryPath, nodeId);

            // CHC, 180502. See CR#95 for more on this.
            // IDENTIFIER_PATH_PART is provided by CONTAINMENT.
            NodePredicate nodePredicate = new NodePredicate(nodeId);
            if (pathPart.equals(JsonbEntryQuery.PATH_PART.IDENTIFIER_PATH_PART)) {
                nodeId = nodePredicate.removeNameValuePredicate();
                jqueryPath.add(nodeId);
                if (i <= segments.size() - 1 && isList(nodeId)) jqueryPath.add(defaultIndex);
            }
            // VARIABLE_PATH_PART is provided by the user. It may contain name/value node predicate
            // see http://www.openehr.org/releases/QUERY/latest/docs/AQL/AQL.html#_node_predicate
            else if (pathPart.equals(JsonbEntryQuery.PATH_PART.VARIABLE_PATH_PART)) {

                if (nodePredicate.hasPredicate()) {
                    // do the formatting to allow name/value node predicate processing
                    jqueryPath = new NodeNameValuePredicate(nodePredicate).path(jqueryPath, nodeId);
                } else {
                    nodeId = nodePredicate.removeNameValuePredicate();
                    jqueryPath.add(nodeId);
                }

                if (isList(nodeId)) jqueryPath.add(defaultIndex);
            }
        }

        if (pathPart.equals(JsonbEntryQuery.PATH_PART.VARIABLE_PATH_PART)) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = jqueryPath.size() - 1; i >= 0; i--) {
                if (jqueryPath.get(i).matches("[0-9]*|#")
                        || jqueryPath.get(i).contains("[")
                        || jqueryPath.get(i).startsWith("'")) break;
                String item = jqueryPath.remove(i);
                stringBuilder.insert(0, item);
            }
            nodeId = EntryAttributeMapper.map(stringBuilder.toString());
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
