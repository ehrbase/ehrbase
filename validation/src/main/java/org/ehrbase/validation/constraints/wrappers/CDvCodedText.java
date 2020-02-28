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

import com.nedap.archie.rm.datavalues.DvCodedText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;
import org.openehr.schemas.v1.*;

import java.util.Map;

/**
 * Validate a DvCodedText
 *
 * The validation check the passed code phrase bound to local or openehr terminology
 *
 * @see DvCodedText
 *
 * Created by christian on 8/10/2016.
 */
public class CDvCodedText extends CConstraint implements I_CArchetypeConstraintValidate {

    private Logger logger = LogManager.getLogger(CDvCodedText.class);

    CDvCodedText(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, ARCHETYPECONSTRAINT archetypeconstraint) throws IllegalArgumentException {

        DvCodedText checkValue = (DvCodedText) aValue;

        if (!(archetypeconstraint instanceof CSINGLEATTRIBUTE))
            ValidationException.raise(path, "Constraint for DvCodedText is not applicable:" + archetypeconstraint, "CODED_TEXT_03");
        CSINGLEATTRIBUTE csingleattribute = (CSINGLEATTRIBUTE) archetypeconstraint;

        Object object = csingleattribute.getChildrenArray(0);

        if (object != null){
            String rmTypeName = ((COBJECT)object).getRmTypeName();
            SchemaType type = rmTypeName.equals("CODE_PHRASE") ? CCODEPHRASE.type : XmlObject.type;
            object = ((COBJECT) object).changeType(type);
        }

        if (!(object instanceof CCODEPHRASE)) {
            if (object instanceof CONSTRAINTREF) //safely ignore it!
            {
                logger.warn("Constraint reference is not supported, path:" + path);
                return;
            }
            ValidationException.raise(path, "Constraint child is not a code phrase constraint:" + object, "CODED_TEXT_02");
        }
        CCODEPHRASE ccodephrase = (CCODEPHRASE) object;

        //use code phrase validation checker
        new CArchetypeConstraint(localTerminologyLookup).validate(path, checkValue.getDefiningCode(), ccodephrase);

        if (ccodephrase.isSetTerminologyId() && ccodephrase.getTerminologyId() != null  && ccodephrase.getTerminologyId().getValue() != null && (ccodephrase.getTerminologyId().getValue().equals("local") || ccodephrase.getTerminologyId().getValue().equals("openehr"))) //if null, the terminology is let free and we cannot check the code
            checkCodedValue(path, checkValue);

    }

    private String lookupPath(String path) {
        int last = path.lastIndexOf("[openEHR-");
        last = path.indexOf("]", last);

        return path.substring(0, last + 1);
    }

    /**
     * check if the dvCodedText value is as coded in the code phrase
     * @return
     */
    private void checkCodedValue(String path, DvCodedText codedText){
        if (localTerminologyLookup == null) //mostly a test scenario
            return;

        for (Map lookup: localTerminologyLookup.values()){
            if (lookup.containsKey(codedText.getDefiningCode().getCodeString()) && lookup.get(codedText.getDefiningCode().getCodeString()).equals(codedText.getValue())){
                return;
            }
        }
        ValidationException.raise(path, "CodedText value is not valid:" + codedText.toString(), "CODED_TEXT_01");
    }
}
