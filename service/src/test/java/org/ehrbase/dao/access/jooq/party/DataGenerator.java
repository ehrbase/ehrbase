package org.ehrbase.dao.access.jooq.party;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;

public class DataGenerator {

  public IdentifierRecord anyIdentifierRecord(Consumer<IdentifierRecord>... constraints) {
    IdentifierRecord rec = new IdentifierRecord();
    Stream.of(constraints).forEach(c -> c.accept(rec));
    return rec;
  }
  
  public static List<IdentifierRecord> anyIdentifierRecordWith(List<UUID> partyUUIDs) {
    return partyUUIDs.stream()
        .map(uuid -> {
            IdentifierRecord rec = new IdentifierRecord();
            rec.setParty(uuid);
            return rec;
        })
        .collect(Collectors.toList());
  }
  

  public PartyIdentifiedRecord anyPartyIdentifiedRecord(Consumer<PartyIdentifiedRecord>... constraints) {
    PartyIdentifiedRecord rec = new PartyIdentifiedRecord();
    Stream.of(constraints).forEach(c -> c.accept(rec));
    return rec;
  }
  
  public static List<PartyIdentifiedRecord> anyPartyIdentifiedRecordWith(List<UUID> uuids) {
    return uuids.stream()
      .map(uuid -> {
        PartyIdentifiedRecord rec = new PartyIdentifiedRecord();
        rec.setId(uuid);
        return rec;
      })
      .collect(Collectors.toList());
  }
  
  public static List<UUID> anyUUIDs(int num) {
    return IntStream.range(0, num).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
  }
}
