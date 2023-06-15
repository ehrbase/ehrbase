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
package org.ehrbase.aql.containment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections.iterators.ReverseListIterator;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.sdk.webtemplate.parser.NodeId;

/**
 * Build jsonpath expression matching containments
 */
public class JsonPathQueryBuilder {

    private Iterator<Object> reverseListIterator;

    public JsonPathQueryBuilder(List<Object> containmentList) {
        this.reverseListIterator = new ReverseListIterator(containmentList);
    }

    public List<NodeId> assemble() {
        List<NodeId> nodeIdList = new ArrayList<>();
        while (reverseListIterator.hasNext()) {
            Object containment = reverseListIterator.next();
            if (containment instanceof Containment) {
                String archetypeId = ((Containment) containment).getArchetypeId();
                NodeId nodeId = new NodeId(
                        ((Containment) containment).getClassName(),
                        StringUtils.isNotBlank(archetypeId) ? archetypeId : null);
                nodeIdList.add(nodeId);
            }
        }
        return nodeIdList;
    }
}
