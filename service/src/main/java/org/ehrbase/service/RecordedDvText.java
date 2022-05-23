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

import com.nedap.archie.rm.datavalues.DvText;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.jooq.Field;
import org.jooq.Record;

public class RecordedDvText {

    /**
     * set a DvText to DB
     */
    public void toDB(Record record, Field<DvCodedTextRecord> targetField, DvText dvText) {
        DvCodedTextRecord dvCodedTextRecord = new DvCodedTextRecord(
                dvText.getValue(),
                null,
                dvText.getFormatting(),
                new PersistentCodePhrase(dvText.getLanguage()).encode(),
                new PersistentCodePhrase(dvText.getEncoding()).encode(),
                new PersistentTermMapping().termMappingRepresentation(dvText.getMappings()));

        record.set(targetField, dvCodedTextRecord);
    }

    // fromDB is performed by the corresponding method in RecordedDvCodedText since DvCodedText inherits from DvText
}
