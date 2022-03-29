package org.ehrbase.dao.access.jooq.party;

import java.util.List;
import java.util.UUID;

import org.ehrbase.jooq.pg.enums.PartyRefIdType;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.junit.Assert;
import org.junit.Test;

import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;



public class PersistedPartyIdentifiedTest {

    @SuppressWarnings({ "unchecked" })
    @Test
    public void retrieveMultiple() {
      List<UUID> uuids = DataGenerator.anyUUIDs(15);
      List<PartyIdentifiedRecord> records = DataGenerator.anyPartyIdentifiedRecordWith(
          uuids,
          rec -> rec.setPartyRefType("partyRefType"),
          rec -> rec.setObjectIdType(PartyRefIdType.hier_object_id));
      
      List<IdentifierRecord> identifierRecs = DataGenerator.anyIdentifierRecordWithParty(uuids);
      PersistedPartyIdentified partyIdentified = new PersistedPartyIdentified(MockSupport.prepareDomainAccessMock(identifierRecs));
      
      List<PartyProxy> partyProxies = partyIdentified.renderMultiple(records);
      
      partyProxies.forEach(p -> {
        Assert.assertTrue(p instanceof PartyIdentified);
        Assert.assertTrue(((PartyIdentified) p).getExternalRef() != null);
        Assert.assertTrue(((PartyIdentified) p).getIdentifiers().size() > 0);
      });
    }
}