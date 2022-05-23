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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.nedap.archie.rm.datavalues.DvIdentifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.junit.Assert;
import org.junit.Test;

public class PartyIdentifiersTest {

    @Test
    @SuppressWarnings({"unchecked"})
    public void retrieveMultiple() {
        String issuer = UUID.randomUUID().toString();
        List<UUID> uuids = DataGenerator.anyUUIDs(4);
        List<PartyIdentifiedRecord> records = DataGenerator.anyPartyIdentifiedRecordWith(uuids);
        List<IdentifierRecord> idRec = DataGenerator.anyIdentifierRecordWithParty(uuids, rec -> rec.setIssuer(issuer));

        PartyIdentifiers partyIdents = new PartyIdentifiers(MockSupport.prepareDomainAccessMock(idRec));
        List<Pair<PartyIdentifiedRecord, List<DvIdentifier>>> pairs = partyIdents.retrieveMultiple(records);

        pairs.forEach(p -> {
            p.getRight().forEach(i -> {
                Assert.assertTrue(issuer.equals(i.getIssuer()));
            });
        });
    }

    @Test
    public void isIdentical() {

        // set 1
        DvIdentifier dvIdentifier11 = new DvIdentifier();
        dvIdentifier11.setId("A");
        dvIdentifier11.setAssigner("A");
        dvIdentifier11.setIssuer("A");
        dvIdentifier11.setType("A");
        DvIdentifier dvIdentifier12 = new DvIdentifier();
        dvIdentifier12.setId("B");
        dvIdentifier12.setAssigner("B");
        dvIdentifier12.setIssuer("B");
        dvIdentifier12.setType("B");

        List<DvIdentifier> set1 = new ArrayList<>();
        set1.add(dvIdentifier11);
        set1.add(dvIdentifier12);

        // set 2
        DvIdentifier dvIdentifier21 = new DvIdentifier();
        dvIdentifier21.setId("A");
        dvIdentifier21.setAssigner("A");
        dvIdentifier21.setIssuer("A");
        dvIdentifier21.setType("A");
        DvIdentifier dvIdentifier22 = new DvIdentifier();
        dvIdentifier22.setId("B");
        dvIdentifier22.setAssigner("B");
        dvIdentifier22.setIssuer("B");
        dvIdentifier22.setType("B");

        List<DvIdentifier> set2 = new ArrayList<>();
        set2.add(dvIdentifier21);
        set2.add(dvIdentifier22);

        assertTrue(new PartyIdentifiers(null).compare(set1, set2));

        // set 3
        DvIdentifier dvIdentifier31 = new DvIdentifier();
        dvIdentifier31.setId("A");
        dvIdentifier31.setAssigner("A");
        dvIdentifier31.setIssuer("A");
        dvIdentifier31.setType("A");
        DvIdentifier dvIdentifier32 = new DvIdentifier();
        dvIdentifier32.setId("C");
        dvIdentifier32.setAssigner("C");
        dvIdentifier32.setIssuer("C");
        dvIdentifier32.setType("C");

        List<DvIdentifier> set3 = new ArrayList<>();
        set3.add(dvIdentifier31);
        set3.add(dvIdentifier32);

        assertFalse(new PartyIdentifiers(null).compare(set1, set3));
    }

    @Test
    public void isIdenticalWithNull() {

        // set 1
        DvIdentifier dvIdentifier11 = new DvIdentifier();
        dvIdentifier11.setId("A");
        DvIdentifier dvIdentifier12 = new DvIdentifier();
        dvIdentifier12.setId("B");

        List<DvIdentifier> set1 = new ArrayList<>();
        set1.add(dvIdentifier11);
        set1.add(dvIdentifier12);

        // set 2
        DvIdentifier dvIdentifier21 = new DvIdentifier();
        dvIdentifier21.setId("A");
        DvIdentifier dvIdentifier22 = new DvIdentifier();
        dvIdentifier22.setId("B");

        List<DvIdentifier> set2 = new ArrayList<>();
        set2.add(dvIdentifier21);
        set2.add(dvIdentifier22);

        assertTrue(new PartyIdentifiers(null).compare(set1, set2));
    }
}
