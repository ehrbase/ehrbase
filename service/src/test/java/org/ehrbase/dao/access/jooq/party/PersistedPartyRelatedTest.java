package org.ehrbase.dao.access.jooq.party;

import java.util.List;

import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.ehrbase.jooq.pg.udt.records.CodePhraseRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartyRelated;



public class PersistedPartyRelatedTest {

    @SuppressWarnings({ "unchecked" })
    @Test
    public void retrieveMultiple() {
      PartyIdentifiedRecord record0 = DataGenerator.anyPartyIdentifiedRecord(
          rec -> rec.setName("record0"),
          rec -> {
            DvCodedTextRecord dv = new DvCodedTextRecord();
            dv.setDefiningCode(new CodePhraseRecord());
            rec.setRelationship(dv);
          }
      );
      
      PartyIdentifiedRecord record1 = DataGenerator.anyPartyIdentifiedRecord(
          rec -> rec.setName("record1"),
          rec -> {
            DvCodedTextRecord dv = new DvCodedTextRecord();
            dv.setDefiningCode(new CodePhraseRecord());
            rec.setRelationship(dv);
          }
      );
      
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
