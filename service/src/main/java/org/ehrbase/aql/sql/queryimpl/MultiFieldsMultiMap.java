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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MultiFieldsMultiMap extends MultiFieldsMap {

    public MultiFieldsMultiMap(List<MultiFields> multiFieldsList) {
        this.multiFieldsListAsMap = toMultiMap(multiFieldsList);
    }

    private Map<String, MultiFields> toMultiMap(List<MultiFields> multiFieldsList) {

        Map<String, MultiFields> multiMap = new LinkedHashMap<>(); // preserve order of insertion

        for (MultiFields multiFields : multiFieldsList) {
            if (!multiFields.isEmpty()) multiMap.put(variableIdentifierPath(multiFields), multiFields);
        }
        return multiMap;
    }

    private String variableIdentifierPath(MultiFields multiFields) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(multiFields.getVariableDefinition().getIdentifier());
        stringBuilder.append("::");
        stringBuilder.append(multiFields.getVariableDefinition().getPath());
        if (multiFields.getVariableDefinition().getAlias() != null) {
            stringBuilder.append("::");
            stringBuilder.append(multiFields.getVariableDefinition().getAlias());
        } else if (multiFields.getVariableDefinition().getPredicateDefinition() != null
                && multiFields.getVariableDefinition().getPredicateDefinition().getOperand2() != null) {
            stringBuilder.append("::");
            stringBuilder.append(
                    multiFields.getVariableDefinition().getPredicateDefinition().getOperand2());
        }
        return stringBuilder.toString();
    }
}
