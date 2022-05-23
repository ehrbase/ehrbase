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

import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyIdentified;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;

public class DataGenerator {

    @SuppressWarnings("unchecked")
    public static DvIdentifier anyDvIdentifier(Consumer<DvIdentifier>... constraints) {
        DvIdentifier dv = new DvIdentifier();
        Stream.of(constraints).forEach(c -> c.accept(dv));
        return dv;
    }

    @SuppressWarnings({"serial", "unchecked"})
    public static PartyIdentified anyPartyProxy(Consumer<PartyIdentified>... constraints) {
        PartyIdentified proxy = new PartyIdentified() {};
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
    public static List<IdentifierRecord> anyIdentifierRecordWithParty(
            List<UUID> uuids, Consumer<IdentifierRecord>... constraints) {
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
    public static List<PartyIdentifiedRecord> anyPartyIdentifiedRecordWith(
            List<UUID> uuids, Consumer<PartyIdentifiedRecord>... constraints) {
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
