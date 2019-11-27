package org.ehrbase.validation.terminology.validator;

public class DvOrdered extends TerminologyCheck{

    public static void check(Object container, String context, com.nedap.archie.rm.datavalues.quantity.DvOrdered dvOrdered) throws Exception {
        System.out.println(context+ "::"+dvOrdered.toString());

        if (dvOrdered.getNormalStatus() != null)
            validate(container, "normal_status", dvOrdered.getNormalStatus().getTerminologyId(), dvOrdered.getNormalStatus().getCodeString());
    }
}
