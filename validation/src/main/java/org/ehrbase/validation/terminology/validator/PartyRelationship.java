package org.ehrbase.validation.terminology.validator;

public class PartyRelationship extends TerminologyCheck{

    public static void check(Object container, String context, com.nedap.archie.rm.demographic.PartyRelationship participation) throws Exception {
        System.out.println(context+ "::"+participation.toString());

//        if (participation.getMode() != null)
//            validate(container, context, participation.getMode().getDefiningCode().getTerminologyId(), participation.getMode().getDefiningCode().getCodeString());
    }
}
