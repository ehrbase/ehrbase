/*
 * Copyright (C) 2020 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

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
package org.ehrbase.aql.containment;

/**
 * Definition of a 'simple' chained CONTAINS
 */
public class SimpleChainedCheck extends ContainsCheck {

    private ContainmentSet containmentSet;

    public SimpleChainedCheck(String symbol, ContainmentSet containmentSet) {
        super(symbol);
        this.containmentSet = containmentSet;
    }

    public String jsonPathNodeFilterExpression(){
        if (containmentSet == null || containmentSet.getContainmentList().isEmpty())
            return null;


        return new JsonPathQueryBuilder(containmentSet.getContainmentList().asList()).assemble();
    }

    public String toString(){
        if (containmentSet == null)
            return "";
        this.checkExpression = containmentSet.getContainmentList().toString();
        return checkExpression;
    }

    public String getSymbol(){
        return label;
    }

    public ContainmentSet getContainmentSet() {
        return containmentSet;
    }
}
