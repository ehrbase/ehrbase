package org.ehrbase.validation.terminology.validator;

public class DvMultimedia extends TerminologyCheck{

    public static void check(Object container, String context, com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia dvMultimedia) throws Exception {
        System.out.println(context+ "::"+dvMultimedia.toString());

        if (dvMultimedia.getIntegrityCheckAlgorithm() != null)
            validate(container, "integrity_check_algorithm", dvMultimedia.getIntegrityCheckAlgorithm().getTerminologyId(), dvMultimedia.getIntegrityCheckAlgorithm().getCodeString());

        if (dvMultimedia.getCompressionAlgorithm() != null)
            validate(container, "compression_algorithm", dvMultimedia.getCompressionAlgorithm().getTerminologyId(), dvMultimedia.getCompressionAlgorithm().getCodeString());

        if (dvMultimedia.getMediaType() != null)
            validate(container, "media_type", dvMultimedia.getMediaType().getTerminologyId(), dvMultimedia.getMediaType().getCodeString());
    }
}
