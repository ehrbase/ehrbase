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

package org.ehrbase.validation.constraints.wrappers;

import com.nedap.archie.rm.datavalues.DvState;
import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;
import org.openehr.schemas.v1.CDVSTATE;
import org.openehr.schemas.v1.STATE;
import org.openehr.schemas.v1.STATEMACHINE;

import java.util.Map;

/**
 * Validate a DvState
 *
 * @see DvState
 *
 * Created by christian on 7/24/2016.
 */
public class CDvState extends CConstraint implements I_CArchetypeConstraintValidate {

    CDvState(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, ARCHETYPECONSTRAINT archetypeconstraint) throws IllegalArgumentException {

        if (!(archetypeconstraint instanceof CDVSTATE))
            throw new IllegalStateException("INTERNAL: archetype constraint is not a CDvState");

        CDVSTATE cdvstate = (CDVSTATE) archetypeconstraint;
        DvState dvState = (DvState) aValue;

        if (cdvstate.getValue() != null) {
            STATEMACHINE statemachine = cdvstate.getValue();

            if (statemachine.sizeOfStatesArray() > 0) {
                for (STATE state : statemachine.getStatesArray()) {
                    if (dvState.getValue().getValue().equals(state.getName()))
                        break;
                }
            }
            throw new IllegalArgumentException("Could not find a state matching:" + dvState.getValue());
        }
    }
}
