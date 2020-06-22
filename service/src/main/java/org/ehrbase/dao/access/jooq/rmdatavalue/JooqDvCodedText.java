package org.ehrbase.dao.access.jooq.rmdatavalue;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import org.ehrbase.jooq.pg.udt.records.CodePhraseRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;

public class JooqDvCodedText {

    DvCodedTextRecord dvCodedTextRecord;

    public JooqDvCodedText(DvCodedTextRecord dvCodedTextRecord) {
        this.dvCodedTextRecord = dvCodedTextRecord;
    }

    public DvCodedText toRmInstance(){
        return new DvCodedText(
                dvCodedTextRecord.getValue(),
                new JooqCodePhrase(dvCodedTextRecord.getDefiningCode()).toRmInstance()
        );
    }

    public DvCodedTextRecord toRecord(DvCodedText dvCodedText){

        if (dvCodedText == null)
            return null;

        return new DvCodedTextRecord(
                dvCodedText.getValue(),
                new CodePhraseRecord(dvCodedText.getDefiningCode().getTerminologyId().getValue(), dvCodedText.getDefiningCode().getCodeString()),
                dvCodedText.getFormatting(),
                dvCodedText.getLanguage() != null ? new CodePhraseRecord(dvCodedText.getLanguage().getTerminologyId().getValue(), dvCodedText.getLanguage().getCodeString()) : null,
                dvCodedText.getEncoding() != null ? new CodePhraseRecord(dvCodedText.getEncoding().getTerminologyId().getValue(), dvCodedText.getEncoding().getCodeString()) : null
        );
    }
}
