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
package org.ehrbase.aql.sql.queryimpl;

import java.util.List;
import org.ehrbase.aql.sql.queryimpl.value_field.NodePredicate;

/**
 * Created by christian on 5/9/2018.
 */
public class NodeNameValuePredicate {

    NodePredicate nodePredicate;

    public NodeNameValuePredicate(NodePredicate nodePredicate) {
        this.nodePredicate = nodePredicate;
    }

    public List<String> path(List<String> jqueryPath, String nodeId) {
        // do the formatting to allow name/value node predicate processing
        String predicate = nodePredicate.predicate();
        jqueryPath.add(new NodePredicate(nodeId).removeNameValuePredicate());
        // encode it to prepare for plpgsql function call: marker followed by the name/value predicate
        jqueryPath.add(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER);
        jqueryPath.add(predicate);

        return jqueryPath;
    }
}
