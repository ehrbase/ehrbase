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

import org.ehrbase.validation.constraints.wrappers.IntervalComparator;
import com.nedap.archie.base.MultiplicityInterval;
import org.openehr.schemas.v1.CCOMPLEXOBJECT;
import org.openehr.schemas.v1.CMULTIPLEATTRIBUTE;
import org.openehr.schemas.v1.CSINGLEATTRIBUTE;
import org.openehr.schemas.v1.IntervalOfInteger;

import java.util.Map;

/**
 * Created by christian on 8/9/2016.
 */
public class OptConstraintMapper extends ConstraintMapper  {

    public void setTerminology(Map<String, Map<String, String>> termTable) {
        localTerminologyLookup = termTable;
    }

    public class OptConstraintItem extends ConstraintItem {
        private CCOMPLEXOBJECT ccomplexobject;

        OptConstraintItem(String path, CCOMPLEXOBJECT ccomplexobject) {
            super(path);
            this.ccomplexobject = ccomplexobject;
        }

        public CCOMPLEXOBJECT getConstraint() {
            return ccomplexobject;
        }

        public boolean isMandatory() {
            return !IntervalComparator.isOptional(ccomplexobject.getOccurrences());
        }

        String occurrencesToString() {
            return IntervalComparator.toString(ccomplexobject.getOccurrences());
        }
    }

    public void bind(String path, CCOMPLEXOBJECT ccobj) throws IllegalArgumentException {
        elementConstraintMap.put(path, new OptConstraintItem(path, ccobj));
    }

    void addToValidPath(String path) {
        if (path.isEmpty())
            return;
        String setPath = path.substring(0, path.lastIndexOf("]") + 1);

        if (setPath.isEmpty())
            return;

        validNodeList.add(setPath);
    }

    void addToWatchList(String path, CSINGLEATTRIBUTE csingleattribute) {
        //check the cardinality of this node
        if (path.isEmpty())
            return;
        String setPath = path.substring(0, path.lastIndexOf("]") + 1);

        if (setPath.isEmpty())
            return;

        ConstraintOccurrences constraintOccurrences = new ConstraintOccurrences(csingleattribute.getExistence());
        //evaluate if this should go into the watch list
        MultiplicityInterval existence = constraintOccurrences.getExistence();

        if (existence.isMandatory() || existence.isProhibited())
            watchList.put(setPath, new OccurrenceItem(constraintOccurrences, existence));

        //check for cardinality
    }

    void addToCardinalityList(String path, CMULTIPLEATTRIBUTE cmultipleattribute) {
        IntervalOfInteger cardinalInterval = cmultipleattribute.getCardinality().getInterval();

        try {

            //monitor only "interesting" cardinality: lower >= 1, upper <= n
            if (cardinalInterval.getLower() >= 1 || !cardinalInterval.getUpperUnbounded()) {
                CardinalityItem cardinalityItem = new CardinalityItem(new ConstraintOccurrences(cmultipleattribute.getExistence()), new ConstraintOccurrences(cmultipleattribute.getCardinality().getInterval()));
                cardinalityList.put(path, cardinalityItem);
            }

        } catch (Exception e) {
             //do nothing, fails if path is root
        }
    }

    void addToExistence(String path, IntervalOfInteger occurrences) {
        occurrencesMap.put(path, new ConstraintOccurrences(occurrences));
    }

}
