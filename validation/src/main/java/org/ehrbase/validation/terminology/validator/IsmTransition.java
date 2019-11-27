package org.ehrbase.validation.terminology.validator;

public class IsmTransition extends TerminologyCheck{

    public static void check(Object container, String context, com.nedap.archie.rm.composition.IsmTransition ismTransition) throws Exception {
        System.out.println(context+ "::"+ismTransition.toString());

        if (ismTransition.getCurrentState() != null)
            validate(container, "current_state", ismTransition.getCurrentState().getDefiningCode().getTerminologyId(), ismTransition.getCurrentState().getDefiningCode().getCodeString());

        if (ismTransition.getTransition() != null)
            validate(container, "transition", ismTransition.getTransition().getDefiningCode().getTerminologyId(), ismTransition.getTransition().getDefiningCode().getCodeString());
    }
}
