/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.queryimpl.attribute;

import static org.ehrbase.jooq.dbencoding.CompositionSerializer.TAG_OTHER_DETAILS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.ehrbase.aql.sql.queryimpl.JqueryPath;
import org.ehrbase.aql.sql.queryimpl.JsonbEntryQuery;
import org.ehrbase.aql.sql.queryimpl.NormalizedRmAttributePath;
import org.ehrbase.aql.sql.queryimpl.QueryImplConstants;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;

public class GenericJsonPath {

    public static final String CONTEXT = "context";
    public static final String FEEDER_AUDIT = "feeder_audit";
    public static final String ORIGINATING_SYSTEM_ITEM_IDS = "originating_system_item_ids";
    public static final String FEEDER_SYSTEM_ITEM_IDS = "feeder_system_item_ids";
    public static final String ORIGINAL_CONTENT = "original_content";
    public static final String ORIGINATING_SYSTEM_AUDIT = "originating_system_audit";
    public static final String FEEDER_SYSTEM_AUDIT = "feeder_system_audit";
    public static final String SETTING = "setting";
    public static final String HEALTH_CARE_FACILITY = "health_care_facility";
    public static final String ITEMS = "items";
    public static final String CONTENT = "content";
    public static final String VALUE = "value";
    public static final String NAME = "name";
    public static final String OTHER_DETAILS = "other_details";
    public static final String OTHER_CONTEXT = "other_context";
    public static final String TERMINOLOGY_ID = "terminology_id";
    public static final String PURPOSE = "purpose";
    public static final String TARGET = "target";
    public static final String ARCHETYPE_NODE_ID = "archetype_node_id";
    private final String path;
    private boolean isIterative = false;

    public GenericJsonPath(String path) {
        this.path = path;
    }

    public String jqueryPath() {
        if (path == null || path.isEmpty()) return path;

        JqueryPath jqueryPath = new JqueryPath(JsonbEntryQuery.PATH_PART.VARIABLE_PATH_PART, path, "0");

        List<String> jqueryPaths = new NormalizedRmAttributePath(jqueryPath.evaluate()).transformStartingAt(0);

        if (!jqueryPaths.isEmpty() && jqueryPaths.get(0).startsWith(TAG_OTHER_DETAILS)) {
            jqueryPaths.set(0, jqueryPaths.get(0).replace(TAG_OTHER_DETAILS, OTHER_DETAILS));
        } else if (!jqueryPaths.isEmpty() && jqueryPaths.get(0).startsWith("/other_context"))
            jqueryPaths.set(0, jqueryPaths.get(0).replace("/other_context", OTHER_CONTEXT));
        else if (!jqueryPaths.isEmpty() && jqueryPaths.get(0).startsWith("/feeder_system_item_ids"))
            jqueryPaths.set(0, jqueryPaths.get(0).replace("/feeder_system_item_ids", FEEDER_SYSTEM_ITEM_IDS));
        else if (jqueryPaths.size() == 1
                && !jqueryPaths.get(0).startsWith(OTHER_DETAILS)
                && !jqueryPaths.get(0).startsWith(OTHER_CONTEXT)) {
            jqueryPaths.set(0, jqueryPaths.get(0).replace("/", ""));
        }

        // substitute all fixed indexes by an iterative marker forcing an array elements fct in SQL expression
        if (jqueryPaths.contains("0")) {
            jqueryPaths = jqueryPaths.stream()
                    .map(s -> s.equals("0") ? QueryImplConstants.AQL_NODE_ITERATIVE_MARKER : s)
                    .collect(Collectors.toList());
        }

        return new JsonbSelect(jqueryPaths).field();
    }

    /**
     * @deprecated 12.6.21, use a common path resolution instead.
     * @return
     */
    @Deprecated(forRemoval = true)
    public String jqueryPathAttributeLevel() {
        if (path == null || path.isEmpty()) return path;

        List<String> jqueryPaths = Arrays.asList(path.split("/|,"));
        List<String> actualPaths = new ArrayList<>();

        for (int i = 0; i < jqueryPaths.size(); i++) {
            String segment = jqueryPaths.get(i);
            if ((segment.matches(NAME)
                            && isTerminalValue(jqueryPaths, i)
                            && jqueryPaths.get(0).equals(OTHER_DETAILS))
                    || (segment.startsWith(ITEMS))) {
                actualPaths.add("/" + segment);
                // takes care of array expression (unless the occurrence is specified)
                actualPaths.add("0");
            } else if (segment.startsWith(CONTENT)) {
                actualPaths.add(CONTENT + ",/" + segment);
                actualPaths.add("0"); // as above
            } else if (segment.matches(VALUE + "|" + NAME)
                    && !isTerminalValue(jqueryPaths, i)
                    && !jqueryPaths.get(0).equals(CONTEXT)) {
                actualPaths.add("/" + segment);
                if (segment.matches(NAME)) actualPaths.add("0");
            } else if (segment.matches(ARCHETYPE_NODE_ID) && jqueryPaths.get(0).equals(OTHER_DETAILS)) {
                // keep '/name' attribute db encoding format since other_details is not related to a template and kept
                // as is...
                actualPaths.add("/" + segment);
            } else actualPaths.add(segment);
        }

        return new JsonbSelect(actualPaths).field();
    }

    public static boolean isTerminalValue(List<String> paths, int index) {
        return paths.size() == 1
                || (paths.size() > 1
                        && index == paths.size() - 1
                        && paths.get(index)
                                .matches(VALUE + "|" + NAME + "|" + TERMINOLOGY_ID + "|" + PURPOSE + "|" + TARGET)
                        // check if this 'terminal attribute' is actually a node attribute
                        // match node predicate regexp starts with '/' which is not the case when splitting the path
                        && !paths.get(index - 1).matches(I_DvTypeAdapter.matchNodePredicate.substring(1))
                        && !paths.get(index - 1).startsWith(OTHER_DETAILS));
    }

    public boolean isIterative() {
        return isIterative;
    }
}
