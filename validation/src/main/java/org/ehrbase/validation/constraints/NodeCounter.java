/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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

package org.ehrbase.validation.constraints;

import com.nedap.archie.rm.composition.*;
import com.nedap.archie.rm.datastructures.*;

import java.util.List;

/**
 * Created by christian on 8/22/2016.
 */
public class NodeCounter {

    private Integer count;

    public NodeCounter() {
        count = 0;
    }

    public void count(Object locatable){

        if (locatable == null) //end of recursion
            return;

        if (locatable instanceof List)
            for (Object item: (List)locatable) {
                count(item);
            }
        else if (locatable instanceof Cluster){
            count(((Cluster) locatable).getItems());
        }
        else if (locatable instanceof Event) {
            count(((Event) locatable).getData());
            count(((Event) locatable).getState());
        }
        else if (locatable instanceof Element){
            //TODO: check for nil value && nullFlavor?
            if (((Element) locatable).getNullFlavour() != null || ((Element) locatable).getValue() != null)
                count++;
        }
        else if (locatable instanceof ItemTree)
            count(((ItemTree) locatable).getItems());
        else if (locatable instanceof ItemList)
            count(((ItemList) locatable).getItems());
        else if (locatable instanceof ItemSingle)
            count += ((ItemSingle) locatable).getItem() != null ? 1 : 0;
        else if (locatable instanceof Section)
            count(((Section) locatable).getItems());
        else if (locatable instanceof Evaluation) {
            count(((Evaluation) locatable).getData());
            count(((Evaluation) locatable).getProtocol());
        }
        else if (locatable instanceof Observation) {
            count(((Observation) locatable).getData());
            count(((Observation) locatable).getProtocol());
        }
        else if (locatable instanceof Instruction) {
            count(((Instruction) locatable).getActivities());
            count(((Instruction) locatable).getProtocol());
        }
        else if (locatable instanceof Action) {
            count(((Action) locatable).getDescription());
            count(((Action) locatable).getProtocol());
        }
        else if (locatable instanceof Activity) {
            count(((Activity) locatable).getDescription());
        }
        else if (locatable instanceof History) {
            count(((History) locatable).getEvents());
        }
        else if (locatable instanceof Composition) {
//            count(((Composition)locatable).getContent());
        }
        else if (locatable instanceof AdminEntry) {
            count(((AdminEntry) locatable).getData());
        }
        else
            throw new IllegalArgumentException("Unhandled data type:"+locatable+ ", class:"+locatable.getClass().getCanonicalName());
    }

    public Integer getCount() {
        return count;
    }
}
