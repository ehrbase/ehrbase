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
package org.ehrbase.aql.sql.queryimpl.value_field;

/**
 * Created by christian on 5/2/2018.
 */
public class NodePredicate {

    private static final String AND_NAME_VALUE = " and name/value=";
    String nodeId;

    public NodePredicate(String nodeId) {
        this.nodeId = nodeId;
    }

    public String removeNameValuePredicate() {

        String retNodeId = nodeId;

        if (retNodeId.contains(AND_NAME_VALUE)) {
            retNodeId = retNodeId.substring(0, retNodeId.indexOf(AND_NAME_VALUE)) + "]";
        } else if (retNodeId.contains(",")) {
            retNodeId = retNodeId.substring(0, retNodeId.indexOf(",")) + "]";
        }

        return retNodeId;
    }

    public String predicate() {
        String predicate = null;

        if (nodeId.contains(AND_NAME_VALUE)) {
            predicate = nodeId.substring(nodeId.indexOf(AND_NAME_VALUE) + AND_NAME_VALUE.length(), nodeId.indexOf("]"));
        } else if (nodeId.contains(",")) {
            predicate = nodeId.substring(nodeId.indexOf(",") + 1, nodeId.indexOf("]"));
        }

        return predicate;
    }

    public boolean hasPredicate() {

        boolean retval = false;

        if (nodeId.contains(AND_NAME_VALUE) || nodeId.contains(",")) {
            retval = true;
        }

        return retval;
    }
}
