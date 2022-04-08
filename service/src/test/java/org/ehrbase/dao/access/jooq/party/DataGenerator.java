package org.ehrbase.dao.access.jooq.party;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;

import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyIdentified;

public class DataGenerator {

  @SuppressWarnings("unchecked")
  public static DvIdentifier anyDvIdentifier(Consumer<DvIdentifier>...constraints) {
    DvIdentifier dv = new DvIdentifier();
    Stream.of(constraints).forEach(c -> c.accept(dv));
    return dv;
  }
  
  @SuppressWarnings({ "serial", "unchecked" })
  public static PartyIdentified anyPartyProxy(Consumer<PartyIdentified>... constraints) {
    PartyIdentified proxy = new PartyIdentified() { };
    Stream.of(constraints).forEach(c -> c.accept(proxy));
    return proxy;
  }
  
  @SuppressWarnings("unchecked")
  public IdentifierRecord anyIdentifierRecord(Consumer<IdentifierRecord>... constraints) {
    IdentifierRecord rec = new IdentifierRecord();
    Stream.of(constraints).forEach(c -> c.accept(rec));
    return rec;
  }
  
  @SuppressWarnings("unchecked")
  public static List<IdentifierRecord> anyIdentifierRecordWithParty(List<UUID> uuids, Consumer<IdentifierRecord>... constraints) {
    return uuids.stream()
        .map(uuid -> {
            IdentifierRecord rec = new IdentifierRecord();
            rec.setIdValue(UUID.randomUUID().toString());
            rec.setParty(uuid);
            Stream.of(constraints).forEach(c -> c.accept(rec));
            return rec;
        })
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  public static PartyIdentifiedRecord anyPartyIdentifiedRecord(Consumer<PartyIdentifiedRecord>... constraints) {
    PartyIdentifiedRecord rec = new PartyIdentifiedRecord();
    Stream.of(constraints).forEach(c -> c.accept(rec));
    return rec;
  }
  
  @SuppressWarnings("unchecked")
  public static List<PartyIdentifiedRecord> anyPartyIdentifiedRecordWith(List<UUID> uuids, Consumer<PartyIdentifiedRecord>... constraints) {
    return uuids.stream()
      .map(uuid -> {
        PartyIdentifiedRecord rec = new PartyIdentifiedRecord();
        rec.setId(uuid);
        Stream.of(constraints).forEach(c -> c.accept(rec));
        return rec;
      })
      .collect(Collectors.toList());
  }
  
  public static List<UUID> anyUUIDs(int num) {
    return IntStream.range(0, num).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
  }
}
