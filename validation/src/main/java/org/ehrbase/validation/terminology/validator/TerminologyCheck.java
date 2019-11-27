package org.ehrbase.validation.terminology.validator;

import com.nedap.archie.rm.support.identification.TerminologyId;

public abstract class TerminologyCheck {

    protected static void validate(Object container, String context, TerminologyId terminologyId, String code){
        //check the code depending on the context and terminology
    }
}
