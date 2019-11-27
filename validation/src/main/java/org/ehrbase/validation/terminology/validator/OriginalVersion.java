package org.ehrbase.validation.terminology.validator;

public class OriginalVersion extends TerminologyCheck{

    public static void check(Object container, String context, com.nedap.archie.rm.changecontrol.OriginalVersion originalVersion) throws Exception {
        System.out.println(context+ "::"+originalVersion.toString());
        if (originalVersion.getLifecycleState() != null)
            validate(container, context, originalVersion.getLifecycleState().getDefiningCode().getTerminologyId(), originalVersion.getLifecycleState().getDefiningCode().getCodeString());
    }
}
