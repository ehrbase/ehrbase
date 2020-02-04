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

package org.ehrbase.validation.constraints.wrappers;

import org.apache.commons.lang3.ArrayUtils;
import org.openehr.schemas.v1.CPRIMITIVE;
import org.openehr.schemas.v1.CREAL;
import org.openehr.schemas.v1.IntervalOfReal;

import java.util.Map;

/**
 * Validate a real
 *
 * @see com.nedap.archie.aom.primitives.CReal
 * @link https://specifications.openehr.org/releases/AM/latest/AOM1.4.html#_c_real_class
 *
 * Created by christian on 7/23/2016.
 */
public class CReal extends CConstraint implements I_CTypeValidate {
    CReal(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, CPRIMITIVE cprimitive) throws IllegalArgumentException {

        CREAL creal = (CREAL) cprimitive;
        Float aFloat = null;

        if (aValue instanceof Double)
            aFloat = ((Double) aValue).floatValue();
        else if (aValue instanceof Float)
            aFloat = (Float) aValue;
        else if (aValue instanceof Integer)
            aFloat = ((Integer) aValue).floatValue();
        else
            ValidationException.raise(path, "Value could not be handled (is it numerical?)" + aValue, "FLOAT01");

        IntervalOfReal intervalOfReal = creal.getRange();
        if (intervalOfReal != null)
            IntervalComparator.isWithinBoundaries(aFloat, intervalOfReal);

        //check within value list if specified
        if (creal.sizeOfListArray() > 0 && !ArrayUtils.contains(creal.getListArray(), aFloat))
            ValidationException.raise(path, "Real value does not match any values in constraint:" + aFloat, "FLOAT02");
    }
}
