/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.jooq.Field;
import org.jooq.Record;

/**
 * get/set a DvCodedText from/to DB
 */
public class RecordedDvCodedText {

    public void toDB(Record record, Field<DvCodedTextRecord> targetField, DvCodedText dvCodedText) {
        DvCodedTextRecord dvCodedTextRecord = new DvCodedTextRecord(
                dvCodedText.getValue(),
                new PersistentCodePhrase(dvCodedText.getDefiningCode()).encode(),
                dvCodedText.getFormatting(),
                new PersistentCodePhrase(dvCodedText.getLanguage()).encode(),
                new PersistentCodePhrase(dvCodedText.getEncoding()).encode(),
                new PersistentTermMapping().termMappingRepresentation(dvCodedText.getMappings()));

        record.set(targetField, dvCodedTextRecord);
    }

    public Object fromDB(Record record, Field<DvCodedTextRecord> fromField) {
        Object retObject;

        DvCodedTextRecord dvCodedTextRecord = record.get(fromField);

        CodePhraseRecord codePhraseDefiningCode = dvCodedTextRecord.getDefiningCode();
        CodePhraseRecord codePhraseLanguage = dvCodedTextRecord.getLanguage();
        CodePhraseRecord codePhraseEncoding = dvCodedTextRecord.getEncoding();

        if (codePhraseDefiningCode != null)
            retObject = new DvCodedText(
                    dvCodedTextRecord.getValue(),
                    codePhraseLanguage == null
                            ? null
                            : new CodePhrase(
                                    new TerminologyId(codePhraseLanguage.getTerminologyIdValue()),
                                    codePhraseLanguage.getCodeString()),
                    codePhraseEncoding == null
                            ? null
                            : new CodePhrase(
                                    new TerminologyId(codePhraseEncoding.getTerminologyIdValue()),
                                    codePhraseEncoding.getCodeString()),
                    new CodePhrase(
                            new TerminologyId(codePhraseDefiningCode.getTerminologyIdValue()),
                            codePhraseDefiningCode.getCodeString()));
        else // assume DvText
        retObject = new DvText(
                dvCodedTextRecord.getValue(),
                codePhraseLanguage == null
                        ? null
                        : new CodePhrase(
                                new TerminologyId(codePhraseLanguage.getTerminologyIdValue()),
                                codePhraseLanguage.getCodeString()),
                codePhraseEncoding == null
                        ? null
                        : new CodePhrase(
                                new TerminologyId(codePhraseEncoding.getTerminologyIdValue()),
                                codePhraseEncoding.getCodeString()));

        if (dvCodedTextRecord.getTermMapping() != null && dvCodedTextRecord.getTermMapping().length > 0) {
            for (String dvCodedTextTermMappingRecord : dvCodedTextRecord.getTermMapping()) {
                ((DvText) retObject).addMapping(new PersistentTermMapping().decode(dvCodedTextTermMappingRecord));
            }
        }

        return retObject;
    }
}
