/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

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

package org.ehrbase.validation.constraints.hardwired;

import org.ehrbase.validation.constraints.ConstraintMapper;
import org.ehrbase.validation.constraints.OptConstraintMapper;
import org.ehrbase.validation.constraints.wrappers.IntervalComparator;
import org.ehrbase.validation.constraints.wrappers.ValidationException;
import org.openehr.schemas.v1.CARDINALITY;
import org.openehr.schemas.v1.CATTRIBUTE;
import org.openehr.schemas.v1.CCOMPLEXOBJECT;
import org.openehr.schemas.v1.CMULTIPLEATTRIBUTE;

/**
 * Validate an Event node
 *
 * @see com.nedap.archie.rm.datastructures.PointEvent
 * @link https://specifications.openehr.org/releases/RM/latest/data_structures.html#_event_t_class
 *
 * Created by christian on 8/11/2016.
 */
public abstract class StructureConstraint {
    private final ConstraintMapper constraintMapper;

    StructureConstraint(ConstraintMapper constraintMapper) {
        this.constraintMapper = constraintMapper;
    }

    /**
     * validate the occurrences of events
     * @param path the node path
     * @param eventsOccurrences the identified event occurrences
     * @throws IllegalArgumentException
     */
    public void validate(String path, Integer eventsOccurrences) throws IllegalArgumentException {

        //check mandatory fields
        CCOMPLEXOBJECT ccomplexobject = ((OptConstraintMapper.OptConstraintItem) constraintMapper.getConstraintItem(path)).getConstraint();

        for (CATTRIBUTE cattribute : ccomplexobject.getAttributesArray()) {
            if (cattribute instanceof CMULTIPLEATTRIBUTE) {
                CARDINALITY cardinality = ((CMULTIPLEATTRIBUTE) cattribute).getCardinality();
                //check cardinality..

                if ((eventsOccurrences > 1) && cardinality.getIsUnique())
                    ValidationException.raise(path, "Only one event is allowed in history", "HIS02");

                //check within boundaries
                IntervalComparator.isWithinBoundaries(eventsOccurrences, cardinality.getInterval());
            }
        }

    }
}
