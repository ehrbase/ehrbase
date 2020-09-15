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

import org.apache.commons.collections.iterators.ReverseListIterator;

import java.util.Iterator;
import java.util.List;

/**
 * Build jsonpath expression matching containments
 */
public class JsonPathQueryBuilder {

    private Iterator<Object> reverseListIterator;

    public JsonPathQueryBuilder(List<Object> containmentList) {
        this.reverseListIterator = new ReverseListIterator(containmentList);;
    }

    /**
     * return the jsonpath expression depending on the containment definition:
     * 1. if the archetype node id is defined use equals node_id (CONTAINS CLUSTER c[openEHR-EHR-CLUSTER.location.v1])
     * 2. if not, use a regexp based on the class name (CONTAINS CLUSTER c)
     * @return
     */
    public String assemble(){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("$");
        while (reverseListIterator.hasNext()){
            Object containment = reverseListIterator.next();
            if (containment instanceof Containment){
                if (!((Containment) containment).getArchetypeId().isBlank()) {
                    stringBuilder.append("..");
                    stringBuilder.append("[?(@.node_id == ");
                    stringBuilder.append("'");
                    stringBuilder.append(((Containment) containment).getArchetypeId());
                    stringBuilder.append("'");
                    //closing the bracket condition
                    stringBuilder.append(")]");
                }
                else {
                    //use the type checking if no archetype node id is specified
                    stringBuilder.append("..");
                    stringBuilder.append("[?(@.type == ");
                    stringBuilder.append("'");
                    stringBuilder.append(((Containment) containment).getClassName().toUpperCase());
                    stringBuilder.append("'");
                    //closing the bracket condition
                    stringBuilder.append(")]");
                }
            }
        }
        return stringBuilder.toString();
    }
}
