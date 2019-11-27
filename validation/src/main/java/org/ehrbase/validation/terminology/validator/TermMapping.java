package org.ehrbase.validation.terminology.validator;

public class TermMapping extends TerminologyCheck{

    public static void check(Object container, String context, com.nedap.archie.rm.datavalues.TermMapping termMapping) throws Exception {
        System.out.println(context+ "::"+termMapping.toString());

        if (termMapping.getPurpose() != null)
            validate(container, "purpose", termMapping.getPurpose().getDefiningCode().getTerminologyId(), termMapping.getPurpose().getDefiningCode().getCodeString());
    }
}
