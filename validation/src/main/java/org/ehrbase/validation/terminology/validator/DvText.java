package org.ehrbase.validation.terminology.validator;

import com.nedap.archie.rm.datavalues.TermMapping;

public class DvText extends TerminologyCheck {

    public static void check(Object container, String context, com.nedap.archie.rm.datavalues.DvText dvText) throws Exception {

        if (dvText.getMappings() != null && !dvText.getMappings().isEmpty()){

            for (TermMapping termMapping: dvText.getMappings()){
                org.ehrbase.validation.terminology.validator.TermMapping.check(container, context, termMapping);
            }
        }

    }
}
