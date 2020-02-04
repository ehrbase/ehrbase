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

import com.nedap.archie.rm.datavalues.DvText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;
import org.openehr.schemas.v1.CCODEPHRASE;
import org.openehr.schemas.v1.CONSTRAINTREF;
import org.openehr.schemas.v1.CSINGLEATTRIBUTE;

import java.util.Map;

/**
 * Validate a DvText
 *
 * @see DvText
 *
 * Created by christian on 8/10/2016.
 */
public class CDvText extends CConstraint implements I_CArchetypeConstraintValidate {

    private Logger logger = LogManager.getLogger(CDvText.class);

    CDvText(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, ARCHETYPECONSTRAINT archetypeconstraint) throws IllegalArgumentException {

        DvText checkValue = (DvText) aValue;

        if (!(archetypeconstraint instanceof CSINGLEATTRIBUTE))
            ValidationException.raise(path, "Constraint for DvCodedText is not applicable:" + archetypeconstraint, "SYS01");
        CSINGLEATTRIBUTE csingleattribute = (CSINGLEATTRIBUTE) archetypeconstraint;

        Object object = csingleattribute.getChildrenArray(0);

        if (!(object instanceof CCODEPHRASE)) {
            if (object instanceof CONSTRAINTREF) //safely ignore it!
            {
                logger.warn("Constraint reference is not supported, path:" + path);
                return;
            }
            ValidationException.raise(path, "Constraint child is not a code phrase constraint:" + object, "SYS01");
        }
        CCODEPHRASE ccodephrase = (CCODEPHRASE) object;

        if (ccodephrase.getCodeListArray().length == 0)
            return;


        for (String termKey : ccodephrase.getCodeListArray()) {
            String matcher = localTerminologyLookup.get(lookupPath(path)).get(termKey);
            if (matcher.equals(checkValue.getValue()))
                return;
        }
        ValidationException.raise(path, "Value does not match any defined codes,found:" + aValue, "TEXT01");
    }

    private String lookupPath(String path) {
        int last = path.lastIndexOf("[openEHR-");
        last = path.indexOf("]", last);

        return path.substring(0, last + 1);
    }
}
