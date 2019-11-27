package org.ehrbase.validation.terminology.validator;

public class EventContext extends TerminologyCheck{

    public static void check(Object container, String context, com.nedap.archie.rm.composition.EventContext eventContext) throws Exception {
        System.out.println(context+ "::"+eventContext.toString());

        if (eventContext.getSetting() != null)
            validate(container, "setting", eventContext.getSetting().getDefiningCode().getTerminologyId(), eventContext.getSetting().getDefiningCode().getCodeString());
    }
}
