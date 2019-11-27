package org.ehrbase.validation.terminology.validator;

public class DvCodedText extends TerminologyCheck{

    public static void check(Object container, String context, com.nedap.archie.rm.datavalues.DvCodedText dvCodedText) throws Exception {
        System.out.println(context+ "::"+dvCodedText.toString());
        validate(container, context, dvCodedText.getDefiningCode().getTerminologyId(), dvCodedText.getDefiningCode().getCodeString());
    }
}
