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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;
import org.openehr.schemas.v1.CATTRIBUTE;
import org.openehr.schemas.v1.CCOMPLEXOBJECT;

import java.util.Map;

/**
 * Validate a complex object
 *
 * @see com.nedap.archie.aom.CComplexObject
 * @link https://specifications.openehr.org/releases/AM/latest/AOM1.4.html#_c_complex_object_class
 *
 * Created by christian on 7/22/2016.
 */
public class CComplexObject extends CConstraint implements I_CArchetypeConstraintValidate {

    static Logger logger = LogManager.getLogger(CComplexObject.class);

    CComplexObject(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    public void validate(String path, Object value, ARCHETYPECONSTRAINT constraint) throws IllegalArgumentException {

        CCOMPLEXOBJECT ccomplexobject = (CCOMPLEXOBJECT) constraint;

        int attributeCount = ccomplexobject.sizeOfAttributesArray();
        int failCount = 0;
        IllegalArgumentException lastException = null;

        for (CATTRIBUTE cattribute : ccomplexobject.getAttributesArray()) {
//            if (cattribute.getRmAttributeName().equals("DV_CODED_TEXT") && (value instanceof DvText)){
//                //validate this DvText as a DvCodedText... (just check the value == matching local terminology entry
//                new CDvText(localTerminologyLookup).validate(path, value, cattribute);
//            }
//            else
            try {
                new CAttribute(localTerminologyLookup).validate(path, value, cattribute);
            } catch (ValidationException e) {
                lastException = e;
                ++failCount;
            }
//            if (attribute instanceof CSINGLEATTRIBUTE)
//                new CSingleAttribute().validate(path, value, (CSINGLEATTRIBUTE)attribute);
//            else if (attribute instanceof CMULTIPLEATTRIBUTE)
//                new CMultipleAttribute().validate(path, value, (CMULTIPLEATTRIBUTE)attribute);
//            else
//                throw new IllegalArgumentException("INTERNAL: could not validate attribute:"+cattribute);
        }

        if (attributeCount > 0 && failCount > 0) {
            if (lastException != null)
                throw lastException;
            else
                ValidationException.raise(path, "Value could not be validated (multiple rules)", "MULT01");
        }

    }
}
