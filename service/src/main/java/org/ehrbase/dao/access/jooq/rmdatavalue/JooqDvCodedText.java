/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.dao.access.jooq.rmdatavalue;

import com.nedap.archie.rm.datavalues.DvCodedText;
import org.ehrbase.jooq.pg.udt.records.CodePhraseRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.ehrbase.service.PersistentTermMapping;

public class JooqDvCodedText {

    DvCodedTextRecord dvCodedTextRecord;

    public JooqDvCodedText(DvCodedTextRecord dvCodedTextRecord) {
        this.dvCodedTextRecord = dvCodedTextRecord;
    }

    public DvCodedText toRmInstance() {
        return new DvCodedText(
                dvCodedTextRecord.getValue(), new JooqCodePhrase(dvCodedTextRecord.getDefiningCode()).toRmInstance());
    }

    public DvCodedTextRecord toRecord(DvCodedText dvCodedText) {

        if (dvCodedText == null) return null;

        return new DvCodedTextRecord(
                dvCodedText.getValue(),
                new CodePhraseRecord(
                        dvCodedText.getDefiningCode().getTerminologyId().getValue(),
                        dvCodedText.getDefiningCode().getCodeString()),
                dvCodedText.getFormatting(),
                dvCodedText.getLanguage() != null
                        ? new CodePhraseRecord(
                                dvCodedText.getLanguage().getTerminologyId().getValue(),
                                dvCodedText.getLanguage().getCodeString())
                        : null,
                dvCodedText.getEncoding() != null
                        ? new CodePhraseRecord(
                                dvCodedText.getEncoding().getTerminologyId().getValue(),
                                dvCodedText.getEncoding().getCodeString())
                        : null,
                dvCodedText.getMappings() != null
                        ? new PersistentTermMapping().termMappingRepresentation(dvCodedText.getMappings())
                        : null);
    }
}
