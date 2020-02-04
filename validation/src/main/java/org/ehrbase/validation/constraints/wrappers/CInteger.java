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
import org.openehr.schemas.v1.CINTEGER;
import org.openehr.schemas.v1.CPRIMITIVE;
import org.openehr.schemas.v1.IntervalOfInteger;

import java.util.Map;

/**
 * Validate an Integer
 *
 * @see com.nedap.archie.aom.primitives.CInteger
 * @link https://specifications.openehr.org/releases/AM/latest/AOM1.4.html#_c_integer_class
 *
 * Created by christian on 7/23/2016.
 */
public class CInteger extends CConstraint implements I_CTypeValidate {

    CInteger(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, CPRIMITIVE cprimitive) throws IllegalArgumentException {

        CINTEGER cinteger = (CINTEGER) cprimitive;
        Integer integer = null;

        if (aValue instanceof Integer)
            integer = (Integer)aValue;
        else if (aValue instanceof Long)
            integer = ((Long)aValue).intValue();
        else
            ValidationException.raise(path, "Value is not a supported type for Integer:" + aValue.getClass().getSimpleName(), "INT02");


        IntervalOfInteger intervalOfInteger = cinteger.getRange();
        if (intervalOfInteger != null)
            IntervalComparator.isWithinBoundaries(integer, intervalOfInteger);

        //check within value list if specified
        if (cinteger.sizeOfListArray() > 0 && !ArrayUtils.contains(cinteger.getListArray(), integer))
            ValidationException.raise(path, "Integer value does not match any values in constraint:" + integer, "INT01");
    }
}
