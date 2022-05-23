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
package org.ehrbase.dao.access.jooq.party;

import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartyRelated;
import java.util.List;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.ehrbase.jooq.pg.udt.records.CodePhraseRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class PersistedPartyRelatedTest {

    @SuppressWarnings({"unchecked"})
    @Test
    public void retrieveMultiple() {
        PartyIdentifiedRecord record0 = DataGenerator.anyPartyIdentifiedRecord(rec -> rec.setName("record0"), rec -> {
            DvCodedTextRecord dv = new DvCodedTextRecord();
            dv.setDefiningCode(new CodePhraseRecord());
            rec.setRelationship(dv);
        });

        PartyIdentifiedRecord record1 = DataGenerator.anyPartyIdentifiedRecord(rec -> rec.setName("record1"), rec -> {
            DvCodedTextRecord dv = new DvCodedTextRecord();
            dv.setDefiningCode(new CodePhraseRecord());
            rec.setRelationship(dv);
        });

        PartyIdentified pp0 = DataGenerator.anyPartyProxy(p -> p.setName("record0"));
        PartyIdentified pp1 = DataGenerator.anyPartyProxy(p -> p.setName("record1"));

        PersistedPartyIdentified mock = Mockito.mock(PersistedPartyIdentified.class);
        Mockito.when(mock.renderMultiple(Mockito.isA(List.class))).thenReturn(List.of(pp0, pp1));

        PersistedPartyRelated partyRelated = new PersistedPartyRelated(null);
        partyRelated.persistedPartyIdentifiedCreator = () -> mock;

        List<PartyProxy> partyProxies = partyRelated.renderMultiple(List.of(record0, record1));

        partyProxies.forEach(p -> {
            Assert.assertTrue(p instanceof PartyRelated);
            Assert.assertTrue(List.of("record0", "record1").contains(((PartyRelated) p).getName()));
        });
    }
}
