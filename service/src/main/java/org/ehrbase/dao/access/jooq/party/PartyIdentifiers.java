/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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

import static org.ehrbase.jooq.pg.Tables.IDENTIFIER;

import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyIdentified;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;

/**
 * Deals with identifiers attribute of PARTY_IDENTIFIED and PARTY_RELATED
 */
class PartyIdentifiers {

    I_DomainAccess domainAccess;

    PartyIdentifiers(I_DomainAccess domainAccess) {
        this.domainAccess = domainAccess;
    }

    /**
     * store the identifiers respectively to a partyIdentified UUID
     * @param partyIdentified
     * @param partyIdentifiedUuid
     */
    void store(PartyIdentified partyIdentified, UUID partyIdentifiedUuid, Short sysTenant) {
        List<DvIdentifier> identifierList = partyIdentified.getIdentifiers();

        if (identifierList != null) {
            for (DvIdentifier identifier : identifierList) {
                if (identifier.getId() != null)
                    domainAccess
                            .getContext()
                            .insertInto(
                                    IDENTIFIER,
                                    IDENTIFIER.PARTY,
                                    IDENTIFIER.ID_VALUE,
                                    IDENTIFIER.ISSUER,
                                    IDENTIFIER.ASSIGNER,
                                    IDENTIFIER.TYPE_NAME,
                                    IDENTIFIER.SYS_TENANT)
                            .values(
                                    partyIdentifiedUuid,
                                    identifier.getId(),
                                    identifier.getIssuer(),
                                    identifier.getAssigner(),
                                    identifier.getType(),
                                    sysTenant)
                            .execute();
            }
        }
    }

    private static final BiFunction<Collection<IdentifierRecord>, UUID, Collection<IdentifierRecord>> allMatchingIdRec =
            (col, uuid) -> col.stream()
                    .filter(irec -> uuid.equals(irec.getParty()))
                    .collect(Collectors.toCollection(HashSet::new));

    List<Pair<PartyIdentifiedRecord, List<DvIdentifier>>> retrieveMultiple(
            Collection<PartyIdentifiedRecord> partyIdentifiedRecords) {

        Set<UUID> allIds =
                partyIdentifiedRecords.stream().map(pir -> pir.getId()).collect(Collectors.toSet());

        Collection<IdentifierRecord> allIdRecs = domainAccess
                .getContext()
                .selectFrom(IDENTIFIER)
                .where(IDENTIFIER.PARTY.in(allIds))
                .fetch()
                .collect(Collectors.toCollection(HashSet::new));

        List<Pair<PartyIdentifiedRecord, List<DvIdentifier>>> result = partyIdentifiedRecords.stream()
                .map(pir -> {
                    List<DvIdentifier> dvIds = allMatchingIdRec.apply(allIdRecs, pir.getId()).stream()
                            .map(record -> idConvert.apply(record))
                            .collect(Collectors.toList());
                    return Pair.of(pir, dvIds);
                })
                .collect(Collectors.toList());

        return result;
    }

    private static final Function<IdentifierRecord, DvIdentifier> idConvert = record -> {
        DvIdentifier identifier = new DvIdentifier();
        identifier.setIssuer(record.getIssuer());
        identifier.setAssigner(record.getAssigner());
        identifier.setId(record.getIdValue());
        identifier.setType(record.getTypeName());
        return identifier;
    };

    List<DvIdentifier> retrieve(PartyIdentifiedRecord partyIdentifiedRecord) {
        return domainAccess.getContext().fetch(IDENTIFIER, IDENTIFIER.PARTY.eq(partyIdentifiedRecord.getId())).stream()
                .map(record -> idConvert.apply(record))
                .collect(Collectors.toList());
    }

    /**
     * compare two lists of identifiers for logical equality.
     * @param identifiersFromDB
     * @param identifiers
     * @return
     */
    public boolean compare(List<DvIdentifier> identifiersFromDB, List<DvIdentifier> identifiers) {

        if (identifiersFromDB == null && identifiers == null) return true;

        if ((identifiersFromDB == null && identifiers != null) || (identifiersFromDB != null && identifiers == null))
            return false;

        if (identifiersFromDB.size() != identifiers.size()) return false;

        List<DvIdentifier> filteredList = identifiersFromDB.stream()
                .filter(identifier -> identifiers.stream()
                        .anyMatch(identifier1 -> Objects.equals(identifier1.getType(), identifier.getType())
                                && Objects.equals(identifier1.getId(), identifier.getId())
                                && Objects.equals(identifier1.getAssigner(), identifier.getAssigner())
                                && Objects.equals(identifier1.getIssuer(), identifier.getIssuer())))
                .collect(Collectors.toList());

        return filteredList.size() == identifiersFromDB.size();
    }

    /**
     * compare the list of identifiers from a record with a list passed as argument
     * @param partyIdentifiedRecord
     * @param identifiers
     * @return
     */
    public boolean compare(PartyIdentifiedRecord partyIdentifiedRecord, List<DvIdentifier> identifiers) {
        List<DvIdentifier> identifiersFromDB = retrieve(partyIdentifiedRecord);

        return compare(identifiersFromDB, identifiers);
    }
}
