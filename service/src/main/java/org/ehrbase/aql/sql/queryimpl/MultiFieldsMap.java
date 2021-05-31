/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package org.ehrbase.aql.sql.queryimpl;

import java.util.*;

public class MultiFieldsMap {

   private final Map<String, MultiFields> multiFieldsListAsMap;

    public MultiFieldsMap(List<MultiFields> multiFieldsList) {
        this.multiFieldsListAsMap = toMap(multiFieldsList);
    }

    private Map<String, MultiFields> toMap(List<MultiFields> multiFieldsList){

        Map<String, MultiFields> multiMap = new LinkedHashMap<>(); //preserve order of insertion

        for (MultiFields multiFields: multiFieldsList){
            multiMap.put(variableIdentifierPath(multiFields.getVariableDefinition().getIdentifier(),multiFields.getVariableDefinition().getPath()), multiFields);
        }
        return multiMap;
    }

    public MultiFields get(String identifierPath){
        return  multiFieldsListAsMap.get(identifierPath);
    }

    public MultiFields get(String variableIdentifier, String variablePath){
        return  multiFieldsListAsMap.get(variableIdentifierPath(variableIdentifier,variablePath));
    }

    private String variableIdentifierPath(String variableIdentifier, String variablePath){
        return variableIdentifier+"::"+variablePath;
    }

    public Iterator<MultiFields> multiFieldsIterator(){
        return multiFieldsListAsMap.values().iterator();
    }

    /**
     * return the upper limit of all paths in the map
     */
    public int upperPathBoundary() {
        int upperbound = 0;

        for (MultiFields multiFields : multiFieldsListAsMap.values()) {
            if (multiFields.fieldsSize() > upperbound)
                upperbound = multiFields.fieldsSize();
        }
        return upperbound;
    }
}
