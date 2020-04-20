/*
 * Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.ehrbase.jooq.pg.udt.records.CodePhraseRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextEmbeddedRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.jooq.Field;
import org.jooq.Record;

/**
 * get/set a DvCodedText WITHOUT TermMapping from/to DB
 */
public class RecordedDvCodedTextEmbedded {

    public DvCodedTextEmbeddedRecord toRecord(DvCodedText dvCodedText){
        return new DvCodedTextEmbeddedRecord(dvCodedText.getValue(),
                        new PersistentCodePhrase(dvCodedText.getDefiningCode()).encode(),
                        dvCodedText.getFormatting(),
                        new PersistentCodePhrase(dvCodedText.getLanguage()).encode(),
                        new PersistentCodePhrase(dvCodedText.getEncoding()).encode());
    }

    public void toDB(Record record, Field<DvCodedTextEmbeddedRecord> targetField, DvCodedText dvCodedText){
        DvCodedTextEmbeddedRecord dvCodedTextEmbeddedRecord = toRecord(dvCodedText);

        record.set(targetField, dvCodedTextEmbeddedRecord);
    }

    public DvCodedText fromRecord(DvCodedTextEmbeddedRecord dvCodedTextEmbeddedRecord){
        CodePhraseRecord codePhraseDefiningCode = dvCodedTextEmbeddedRecord.getDefiningCode();
        CodePhraseRecord codePhraseLanguage = dvCodedTextEmbeddedRecord.getLanguage();
        CodePhraseRecord codePhraseEncoding = dvCodedTextEmbeddedRecord.getEncoding();

        if (codePhraseDefiningCode != null)
            return new DvCodedText(dvCodedTextEmbeddedRecord.getValue(),
                    codePhraseLanguage == null ? null : new CodePhrase(new TerminologyId(codePhraseLanguage.getTerminologyIdValue()), codePhraseLanguage.getCodeString()),
                    codePhraseEncoding == null ? null : new CodePhrase(new TerminologyId(codePhraseEncoding.getTerminologyIdValue()), codePhraseEncoding.getCodeString()),
                    new CodePhrase(new TerminologyId(codePhraseDefiningCode.getTerminologyIdValue()), codePhraseDefiningCode.getCodeString())
            );
        else
            return new DvCodedText(dvCodedTextEmbeddedRecord.getValue(),
                    codePhraseLanguage == null ? null : new CodePhrase(new TerminologyId(codePhraseLanguage.getTerminologyIdValue()), codePhraseLanguage.getCodeString()),
                    codePhraseEncoding == null ? null : new CodePhrase(new TerminologyId(codePhraseEncoding.getTerminologyIdValue()), codePhraseEncoding.getCodeString()),
                    null);

    }

    public Object fromDB(Record record, Field<DvCodedTextEmbeddedRecord> fromField){

        DvCodedTextEmbeddedRecord dvCodedTextEmbeddedRecord = record.get(fromField);

        CodePhraseRecord codePhraseDefiningCode = dvCodedTextEmbeddedRecord.getDefiningCode();
        CodePhraseRecord codePhraseLanguage = dvCodedTextEmbeddedRecord.getLanguage();
        CodePhraseRecord codePhraseEncoding = dvCodedTextEmbeddedRecord.getEncoding();

        if (codePhraseDefiningCode != null)
            return new DvCodedText(dvCodedTextEmbeddedRecord.getValue(),
                    codePhraseLanguage == null ? null : new CodePhrase(new TerminologyId(codePhraseLanguage.getTerminologyIdValue()), codePhraseLanguage.getCodeString()),
                    codePhraseEncoding == null ? null : new CodePhrase(new TerminologyId(codePhraseEncoding.getTerminologyIdValue()), codePhraseEncoding.getCodeString()),
                    new CodePhrase(new TerminologyId(codePhraseDefiningCode.getTerminologyIdValue()), codePhraseDefiningCode.getCodeString())
                    );
        else //assume DvText
            return new DvText(dvCodedTextEmbeddedRecord.getValue(),
                    codePhraseLanguage == null ? null : new CodePhrase(new TerminologyId(codePhraseLanguage.getTerminologyIdValue()), codePhraseLanguage.getCodeString()),
                    codePhraseEncoding == null ? null : new CodePhrase(new TerminologyId(codePhraseEncoding.getTerminologyIdValue()), codePhraseEncoding.getCodeString())
            );
    }
}
