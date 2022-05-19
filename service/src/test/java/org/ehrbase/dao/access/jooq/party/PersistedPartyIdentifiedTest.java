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
import java.util.List;
import java.util.UUID;
import org.ehrbase.jooq.pg.enums.PartyRefIdType;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.junit.Assert;
import org.junit.Test;

public class PersistedPartyIdentifiedTest {

    @SuppressWarnings({"unchecked"})
    @Test
    public void retrieveMultiple() {
        List<UUID> uuids = DataGenerator.anyUUIDs(15);
        List<PartyIdentifiedRecord> records = DataGenerator.anyPartyIdentifiedRecordWith(
                uuids,
                rec -> rec.setPartyRefType("partyRefType"),
                rec -> rec.setObjectIdType(PartyRefIdType.hier_object_id));

        List<IdentifierRecord> identifierRecs = DataGenerator.anyIdentifierRecordWithParty(uuids);
        PersistedPartyIdentified partyIdentified =
                new PersistedPartyIdentified(MockSupport.prepareDomainAccessMock(identifierRecs));

        List<PartyProxy> partyProxies = partyIdentified.renderMultiple(records);

        partyProxies.forEach(p -> {
            Assert.assertTrue(p instanceof PartyIdentified);
            Assert.assertTrue(((PartyIdentified) p).getExternalRef() != null);
            Assert.assertTrue(((PartyIdentified) p).getIdentifiers().size() > 0);
        });
    }
}
