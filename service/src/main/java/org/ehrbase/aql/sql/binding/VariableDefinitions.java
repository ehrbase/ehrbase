/*
 * Copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.VariableDefinition;

import java.util.Iterator;
import java.util.List;

public class VariableDefinitions implements Iterator<I_VariableDefinition>{

    private final List<I_VariableDefinition> variableDefinitionList;
    private Iterator<I_VariableDefinition> iterator;

    public VariableDefinitions(List<I_VariableDefinition> variableDefinitionList) {
        this.variableDefinitionList = variableDefinitionList;
        iterator = variableDefinitionList.iterator();
    }

    public Iterator<I_VariableDefinition> iterator(){
        return variableDefinitionList.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public I_VariableDefinition next() {
        return iterator.next();
    }

    public boolean exists(I_VariableDefinition variableDefinition){
        Iterator<I_VariableDefinition> iterator = variableDefinitionList.iterator();

        while (iterator.hasNext()){
            if (iterator.next().equals(variableDefinition)){
                return true;
            }
        }
        return false;
    }

    public boolean isDistinct(String variableAlias){
        Iterator<I_VariableDefinition> iterator = variableDefinitionList.iterator();

        while (iterator.hasNext()){
            I_VariableDefinition variableDefinition = iterator.next();
            if (variableDefinition instanceof VariableDefinition && variableDefinition.getAlias().equals(variableAlias))
                return variableDefinition.isDistinct();
        }
        return false;
    }
}
