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
import java.util.List;
import java.util.Set;
import org.ehrbase.openehr.sdk.webtemplate.parser.NodeId;
import org.ehrbase.service.KnowledgeCacheService;

/**
 * Convenience class to perform specific Containment operations related to containment path resolution
 */
public class Containments {

    private Set<Object> containmentSet;
    private KnowledgeCacheService knowledgeCacheService;

    public Containments(KnowledgeCacheService knowledgeCacheService, ContainmentSet containmentSet) {
        this.knowledgeCacheService = knowledgeCacheService;
        this.containmentSet = containmentSet.getContainmentList();
    }

    public boolean hasUnresolvedContainment(String templateId) {
        for (Object containment : containmentSet) {

            if (containment instanceof Containment && ((Containment) containment).getPath(templateId) == null)
                return true;
        }
        return false;
    }

    public void resolveContainers(String templateId) {

        // traverse the list from the last containment and resolve the ones with path
        List<Object> containmentList = new ArrayList<>();
        containmentList.addAll(containmentSet);

        for (int i = 0; i < containmentList.size(); i++) {
            if (containmentList.get(i) instanceof Containment) {
                Containment containment = (Containment) containmentList.get(i);

                if (containment.getClassName().equals("COMPOSITION") && containment.getArchetypeId() == null) {
                    continue;
                }

                if (containment.getPath(templateId) == null) {
                    List sublist = containmentList.subList(i, containmentList.size());
                    // build the jsonpath expression up to this containment
                    List<NodeId> jsonQuery = new JsonPathQueryBuilder(sublist).assemble();
                    // get the path for this template
                    JsonPathQueryResult jsonPathQueryResult =
                            new Templates(knowledgeCacheService).resolveForTemplate(templateId, jsonQuery);
                    if (jsonPathQueryResult != null) {
                        containment.setPath(templateId, jsonPathQueryResult.getAqlPath());
                    }
                }
            }
        }
    }
}
