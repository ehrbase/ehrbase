package org.ehrbase.dao.access.jooq.party;

import static org.ehrbase.jooq.pg.Tables.IDENTIFIER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.ehrbase.jooq.pg.tables.Identifier;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectWhereStep;
import org.junit.Test;
import org.mockito.Mockito;
import org.jooq.Condition;

import com.nedap.archie.rm.datavalues.DvIdentifier;


public class PartyIdentifiersTest {

//    @Test
//    public void retrieveMultiple() {
//      List<UUID> uuids = DataGenerator.anyUUIDs(4);
//      List<PartyIdentifiedRecord> records = DataGenerator.anyPartyIdentifiedRecordWith(uuids);
//      
//      
//      DataGenerator
//      
//      
//      
//      Result result = Mockito.mock(Result.class);
//      Mockito.when(result.collect(Mockito.isA(Collector.class))).thenReturn(result);
//      
//      SelectConditionStep cond = Mockito.mock(SelectConditionStep.class);
//      Mockito.when(cond.fetch()).thenReturn(result);
//      
//      SelectWhereStep where = Mockito.mock(SelectWhereStep.class);
//      Mockito.when(where.where(Mockito.isA(Condition.class))).thenReturn(cond);
//      
//      DSLContext ctx = Mockito.mock(DSLContext.class);
//      Mockito.when(ctx.selectFrom(Mockito.isA(Identifier.class))).thenReturn(where);
//      
//      I_DomainAccess domAccess = Mockito.mock(I_DomainAccess.class);
//      Mockito.when(domAccess.getContext()).thenReturn(ctx);
//      
//      
////      Collection<IdentifierRecord> allIdRecs = ctx
////          .selectFrom(IDENTIFIER)
////          .where(IDENTIFIER.PARTY.in(allIds))
////          .fetch()
////          .collect(Collectors.toCollection(HashSet::new));
//    }
  
    
  
  
  
  
  
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

        List<DvIdentifier> set1 = new ArrayList<>(); set1.add(dvIdentifier11); set1.add(dvIdentifier12);

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

        List<DvIdentifier> set2 = new ArrayList<>(); set2.add(dvIdentifier21); set2.add(dvIdentifier22);

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

        List<DvIdentifier> set3 = new ArrayList<>(); set3.add(dvIdentifier31); set3.add(dvIdentifier32);

        assertFalse(new PartyIdentifiers(null).compare(set1, set3));
    }

    @Test
    public void isIdenticalWithNull() {

        // set 1
        DvIdentifier dvIdentifier11 = new DvIdentifier();
        dvIdentifier11.setId("A");
        DvIdentifier dvIdentifier12 = new DvIdentifier();
        dvIdentifier12.setId("B");

        List<DvIdentifier> set1 = new ArrayList<>(); set1.add(dvIdentifier11); set1.add(dvIdentifier12);

        // set 2
        DvIdentifier dvIdentifier21 = new DvIdentifier();
        dvIdentifier21.setId("A");
        DvIdentifier dvIdentifier22 = new DvIdentifier();
        dvIdentifier22.setId("B");

        List<DvIdentifier> set2 = new ArrayList<>(); set2.add(dvIdentifier21); set2.add(dvIdentifier22);

        assertTrue(new PartyIdentifiers(null).compare(set1, set2));
    }
}