/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
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
package org.ehrbase.validation.terminology.validator;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import org.ehrbase.terminology.openehr.TerminologyInterface;
import org.ehrbase.terminology.openehr.implementation.AttributeCodesetMapping;
import org.ehrbase.terminology.openehr.implementation.ContainerType;

public class TerminologyCheck implements I_TerminologyCheck {

    protected Class RM_CLASS;

    public static void validate(TerminologyInterface terminologyInterface, AttributeCodesetMapping codesetMapping, String context, CodePhrase codePhrase, String language) {
        //if terminology id == 'local' (e.g. defined at Template level) skip the validation
        if (codePhrase.getTerminologyId().getValue().equals("local"))
            return;

        //get the actual attribute
        if (!codesetMapping.isLocalizedAttribute(codePhrase.getTerminologyId().getValue(), context, language))
            language = "en"; //default to English for the rest of the validation

        String attribute = codesetMapping.actualAttributeId(codePhrase.getTerminologyId().getValue(), context, language);
        ContainerType containerType = codesetMapping.containerType(codePhrase.getTerminologyId().getValue(), context);

        switch (containerType){
            case GROUP: //a code string defined within a group of a codeset
                boolean valid = terminologyInterface.terminology(codePhrase.getTerminologyId().getValue()).hasCodeForGroupId(attribute, codePhrase);
                if (!valid){
                    throw new IllegalArgumentException("supplied code string ["+codePhrase.getCodeString()+"] is not found in group:"+attribute);
                }
                break;

            case CODESET: //a codestring defined in a codeset
                valid = terminologyInterface.codeSet(codePhrase.getTerminologyId().getValue()).hasCode(codePhrase);
                if (!valid){
                    throw new IllegalArgumentException("supplied code string ["+codePhrase.getCodeString()+"] is not found in codeset:"+attribute);
                }
                break;

            case UNDEFINED:
                break;

            default:
                throw new IllegalArgumentException("undefined container type");
        }
    }

    public static void validate(TerminologyInterface terminologyInterface, AttributeCodesetMapping codesetMapping, String context, CodePhrase codePhrase) throws IllegalArgumentException {
        validate(terminologyInterface, codesetMapping, context, codePhrase, "en");
    }

    public static void validate(TerminologyInterface terminologyInterface, AttributeCodesetMapping codesetMapping, String context, DvCodedText dvCodedText, String language) throws IllegalArgumentException {
        validate(terminologyInterface, codesetMapping, context, dvCodedText.getDefiningCode(), language);

        if (terminologyInterface.terminology(dvCodedText.getDefiningCode().getTerminologyId().getValue()) == null) //terminology is NOT defined
            return;

        if (!codesetMapping.isLocalizedAttribute(dvCodedText.getDefiningCode().getTerminologyId().getValue(), context, language))
            language = "en"; //default to English for the rest of the validation

        String rubric = terminologyInterface.terminology(dvCodedText.getDefiningCode().getTerminologyId().getValue()).rubricForCode(dvCodedText.getDefiningCode().getCodeString(), language);
        boolean valid = rubric.equals(dvCodedText.getValue());
        if (!valid){
            throw new IllegalArgumentException("supplied value ["
                    +dvCodedText.getValue()
                    +"] doesn't match code string:"
                    +dvCodedText.getDefiningCode().getCodeString()
                    +" (language:"+language+", terminology:"+dvCodedText.getDefiningCode().getTerminologyId().getValue()+"), expected:"+rubric);
        }
    }
    public static void validate(TerminologyInterface terminologyInterface, AttributeCodesetMapping codesetMapping, String context, DvCodedText dvCodedText) throws IllegalArgumentException {
        validate(terminologyInterface, codesetMapping, context, dvCodedText, "en");
    }

    public Class rmClass(){
        return RM_CLASS;
    }

}
