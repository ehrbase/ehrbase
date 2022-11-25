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

import static org.junit.Assert.*;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.TermMapping;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.util.ArrayList;
import java.util.List;
import org.ehrbase.jooq.pg.tables.EventContext;
import org.ehrbase.jooq.pg.tables.records.EventContextRecord;
import org.ehrbase.jooq.pg.udt.records.CodePhraseRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.jooq.Record;
import org.junit.Test;

/**
 * test DvCodedText stored as a ehr.dv_coded_text UDT
 */
public class RecordedDvCodedTextTest {

    @Test
    public void testToDB() {

        Record record = new EventContextRecord();

        DvCodedText dvCodedText = new DvCodedText(
                "testvalue",
                new CodePhrase("en"),
                new CodePhrase("UTF-8"),
                new CodePhrase(new TerminologyId("terminology"), "1224"));
        TermMapping termMapping = new TermMapping(
                new CodePhrase(new TerminologyId("A"), "target"),
                Character.valueOf('>'),
                new DvCodedText("purpose", new CodePhrase(new TerminologyId("B"), "BBB")));
        List<TermMapping> termMappings = new ArrayList<>();
        termMappings.add(termMapping);
        dvCodedText.setMappings(termMappings);

        new RecordedDvCodedText().toDB(record, EventContext.EVENT_CONTEXT.SETTING, dvCodedText);

        DvCodedTextRecord dvCodedTextRecord = record.get(EventContext.EVENT_CONTEXT.SETTING);

        assertEquals(">", dvCodedTextRecord.getTermMapping()[0].split("|")[0]);
    }

    @Test
    public void testFromDB() {
        Record record = new EventContextRecord();

        DvCodedTextRecord dvCodedTextRecord = new DvCodedTextRecord();
        dvCodedTextRecord.setValue("1234");
        dvCodedTextRecord.setDefiningCode(new CodePhraseRecord("term1", "aaa"));
        dvCodedTextRecord.setTermMapping(new String[] {">|purpose|B|BBB|A|target"});

        record.set(EventContext.EVENT_CONTEXT.SETTING, dvCodedTextRecord);

        DvCodedText dvCodedText =
                (DvCodedText) new RecordedDvCodedText().fromDB(record, EventContext.EVENT_CONTEXT.SETTING);

        assertEquals('>', dvCodedText.getMappings().get(0).getMatch());
    }
}
