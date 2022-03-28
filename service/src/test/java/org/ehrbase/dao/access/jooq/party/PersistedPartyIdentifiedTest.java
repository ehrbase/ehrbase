package org.ehrbase.dao.access.jooq.party;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;

import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.enums.PartyRefIdType;
import org.ehrbase.jooq.pg.tables.Identifier;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectWhereStep;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;



public class PersistedPartyIdentifiedTest {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void retrieveMultiple() {
      List<UUID> uuids = DataGenerator.anyUUIDs(15);
      List<PartyIdentifiedRecord> records = DataGenerator.anyPartyIdentifiedRecordWith(
          uuids,
          rec -> rec.setPartyRefType("partyRefType"),
          rec -> rec.setObjectIdType(PartyRefIdType.hier_object_id));
      
      List<IdentifierRecord> idRec = DataGenerator.anyIdentifierRecordWith(uuids);
      
      Result result = Mockito.mock(Result.class);
      Mockito.when(result.collect(Mockito.isA(Collector.class))).thenReturn(idRec);
      
      SelectConditionStep cond = Mockito.mock(SelectConditionStep.class);
      Mockito.when(cond.fetch()).thenReturn(result);
      
      SelectWhereStep where = Mockito.mock(SelectWhereStep.class);
      Mockito.when(where.where(Mockito.isA(Condition.class))).thenReturn(cond);
      
      DSLContext ctx = Mockito.mock(DSLContext.class);
      Mockito.when(ctx.selectFrom(Mockito.isA(Identifier.class))).thenReturn(where);
      
      
      
      I_DomainAccess domAccess = Mockito.mock(I_DomainAccess.class);
      Mockito.when(domAccess.getContext()).thenReturn(ctx);
      
      PersistedPartyIdentified pIds = new PersistedPartyIdentified(domAccess);
      
      List<PartyProxy> retrieveMultiple = pIds.renderMultiple(records);
      
      retrieveMultiple.forEach(p -> {
        Assert.assertTrue(p instanceof PartyIdentified);
        Assert.assertTrue(((PartyIdentified) p).getExternalRef() != null);
        Assert.assertTrue(((PartyIdentified) p).getIdentifiers().size() > 0);
      });
    }
}