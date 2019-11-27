package org.ehrbase.validation.terminology.validator;

public class Participation extends TerminologyCheck{

    public static void check(Object container, String context, com.nedap.archie.rm.generic.Participation participation) throws Exception {
        System.out.println(context+ "::"+participation.toString());

        if (participation.getMode() != null)
            validate(container, "mode", participation.getMode().getDefiningCode().getTerminologyId(), participation.getMode().getDefiningCode().getCodeString());
    }
}
