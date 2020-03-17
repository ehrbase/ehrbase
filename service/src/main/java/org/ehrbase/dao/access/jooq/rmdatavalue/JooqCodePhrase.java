package org.ehrbase.dao.access.jooq.rmdatavalue;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.ehrbase.jooq.pg.udt.records.CodePhraseRecord;

public class JooqCodePhrase {

    CodePhraseRecord codePhraseRecord;

    public JooqCodePhrase(CodePhraseRecord codePhraseRecord) {
        this.codePhraseRecord = codePhraseRecord;
    }

    public CodePhrase toRmInstance(){
        return new CodePhrase(new TerminologyId(codePhraseRecord.getTerminologyIdValue()), codePhraseRecord.getCodeString());
    }
}
