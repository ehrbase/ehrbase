package org.ehrbase.validation.terminology.validator;

public class IntervalEvent extends TerminologyCheck{

    public static void check(Object container, String context, com.nedap.archie.rm.datastructures.IntervalEvent intervalEvent) throws Exception {
        System.out.println(context+ "::"+intervalEvent.toString());

        if (intervalEvent.getMathFunction() != null)
            validate(container, "math_function", intervalEvent.getMathFunction().getDefiningCode().getTerminologyId(), intervalEvent.getMathFunction().getDefiningCode().getCodeString());
    }
}
