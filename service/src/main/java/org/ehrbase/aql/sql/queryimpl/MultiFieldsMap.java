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

import java.util.*;
import java.util.stream.Collectors;
import org.ehrbase.aql.definition.LateralJoinDefinition;

public class MultiFieldsMap {

    protected Map<String, MultiFields> multiFieldsListAsMap;

    public MultiFieldsMap(List<MultiFields> multiFieldsList) {
        this.multiFieldsListAsMap = toMap(multiFieldsList);
    }

    public MultiFieldsMap() {}

    private Map<String, MultiFields> toMap(List<MultiFields> multiFieldsList) {

        Map<String, MultiFields> multiMap = new LinkedHashMap<>(); // preserve order of insertion

        for (MultiFields multiFields : multiFieldsList) {
            if (!multiFields.isEmpty())
                multiMap.put(
                        variableIdentifierPath(
                                multiFields.getVariableDefinition().getIdentifier(),
                                multiFields.getVariableDefinition().getPath()),
                        multiFields);
        }
        return multiMap;
    }

    public MultiFields get(String identifierPath) {
        return multiFieldsListAsMap.get(identifierPath);
    }

    public MultiFields get(String variableIdentifier, String variablePath) {
        // seek for multifields where the variable matches identifier and path
        List<MultiFields> result = multiFieldsListAsMap.values().stream()
                .filter(v -> v.getVariableDefinition().getIdentifier().equals(variableIdentifier)
                        && v.getVariableDefinition().getPath() != null
                        && v.getVariableDefinition().getPath().equals(variablePath))
                .collect(Collectors.toList());
        if (result.isEmpty()) return null;
        else return result.get(0); // by construction, the result is unique
    }

    private String variableIdentifierPath(String variableIdentifier, String variablePath) {
        return variableIdentifier + "::" + variablePath;
    }

    public Iterator<MultiFields> multiFieldsIterator() {
        return multiFieldsListAsMap.values().iterator();
    }

    /**
     * return the upper limit of all paths in the map
     */
    public int upperPathBoundary() {
        int upperbound = 0;

        for (MultiFields multiFields : multiFieldsListAsMap.values()) {
            if (multiFields.fieldsSize() > upperbound) upperbound = multiFields.fieldsSize();
        }
        return upperbound;
    }

    /**
     * traverse the list of existing definition and identify a lateral join matching this variable path, template and SQL expression
     * @param templateId
     * @param candidateLateralExpression
     * @return
     */
    public Optional<LateralJoinDefinition> matchingLateralJoin(String templateId, String candidateLateralExpression) {
        return matchingLateralJoin(multiFieldsListAsMap.values(), templateId, candidateLateralExpression);
    }

    /**
     * traverse the list of existing definition and identify a lateral join matching this variable path, template and SQL expression
     * @param multiFieldsCollection
     * @param templateId
     * @param candidateLateralExpression
     * @return
     */
    public static Optional<LateralJoinDefinition> matchingLateralJoin(
            Collection<MultiFields> multiFieldsCollection, String templateId, String candidateLateralExpression) {
        return multiFieldsCollection.stream()
                .map(MultiFields::getVariableDefinition)
                .map(vd -> vd.getLastLateralJoin(templateId))
                .filter(Objects::nonNull)
                .filter(lj -> lj.getSqlExpression().equals(candidateLateralExpression))
                .findFirst();
    }
}
