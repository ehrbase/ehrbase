package org.ehrbase.validation.terminology.validator;

public class Composition extends TerminologyCheck{

    public static void check(Object container, String context, com.nedap.archie.rm.composition.Composition composition) throws Exception {
        System.out.println(context+ "::"+composition.toString());
        if (composition.getCategory() != null)
            validate(container, "category", composition.getCategory().getDefiningCode().getTerminologyId(), composition.getCategory().getDefiningCode().getCodeString());
        if (composition.getLanguage() != null)
            validate(container, "language", composition.getLanguage().getTerminologyId(), composition.getLanguage().getCodeString());
        if (composition.getTerritory() != null)
            validate(container, "territory", composition.getTerritory().getTerminologyId(), composition.getTerritory().getCodeString());
    }
}
