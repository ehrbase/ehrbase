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

import com.nedap.archie.rm.datavalues.quantity.DvQuantity;
import org.apache.commons.lang3.StringUtils;
import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;
import org.openehr.schemas.v1.CDVQUANTITY;
import org.openehr.schemas.v1.CQUANTITYITEM;
import org.openehr.schemas.v1.IntervalOfReal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validate a DvQuantity
 *
 * @see DvQuantity
 * <p>
 * Created by christian on 7/24/2016.
 */
public class CDvQuantity extends CConstraint implements I_CArchetypeConstraintValidate {

    CDvQuantity(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    public void validate(String path, Object aValue, ARCHETYPECONSTRAINT archetypeconstraint) throws IllegalArgumentException {

        DvQuantity quantity = (DvQuantity) aValue;

        if (quantity.getMagnitude() == null)
            ValidationException.raise(path, "DvQuantity requires a non null magnitude", "DV_QUANTITY_01");

        CDVQUANTITY constraint = (CDVQUANTITY) archetypeconstraint;
        //check constraint attributes
        if (quantity.getUnits() == null)
            ValidationException.raise(path, "No units specified for item:" + quantity + " at path:" + path, "DV_QUANTITY_02");

        List<String> stringBuffer = new ArrayList<>();
        match_value:
        {
            if (constraint.sizeOfListArray() == 0) //no units specified in constraint
                break match_value;
            for (CQUANTITYITEM cquantityitem : constraint.getListArray()) {
                //check if this item matches the defined unit
                if (!cquantityitem.getUnits().equals(quantity.getUnits())) {
                    stringBuffer.add(cquantityitem.getUnits());
                    continue;
                }
                if (cquantityitem.isSetMagnitude()) {
                    IntervalOfReal magnitudes = cquantityitem.getMagnitude();
                    IntervalComparator.isWithinBoundaries((quantity.getMagnitude()).floatValue(), magnitudes);
                }
                if (cquantityitem.isSetMagnitude() && quantity.getMagnitude() != null) {
                    Long precision = quantity.getPrecision();
                    if (precision != null) {
                        IntervalComparator.isWithinPrecision(precision.intValue(), cquantityitem.getPrecision());
                    }
                }
                break match_value; //comparison done with a matching unit
            }

            ValidationException.raise(path, "No matching units for:" + (StringUtils.isNotEmpty(quantity.getUnits()) ? quantity.getUnits() : "*undef*") + ", expected units:" + String.join(",", stringBuffer), "DV_QUANTITY_03");

        }
    }
}
