/*
 * Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.ehrbase.jooq.pg.Tables.IDENTIFIER;

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
    void store(PartyIdentified partyIdentified, UUID partyIdentifiedUuid){
        List<DvIdentifier> identifierList = partyIdentified.getIdentifiers();

        if (identifierList != null) {
            for (DvIdentifier identifier : identifierList) {
                if (identifier.getId() != null)
                    domainAccess.getContext().insertInto(IDENTIFIER, IDENTIFIER.PARTY, IDENTIFIER.ID_VALUE, IDENTIFIER.ISSUER, IDENTIFIER.ASSIGNER, IDENTIFIER.TYPE_NAME)
                            .values(partyIdentifiedUuid, identifier.getId(), identifier.getIssuer(), identifier.getAssigner(), identifier.getType())
                            .execute();
            }
        }
    }

    List<DvIdentifier> retrieve(PartyIdentifiedRecord partyIdentifiedRecord){
        List<DvIdentifier> identifierList = new ArrayList<>();

        domainAccess.getContext().fetch(IDENTIFIER, IDENTIFIER.PARTY.eq(partyIdentifiedRecord.getId())).forEach(record -> {
            DvIdentifier identifier = new DvIdentifier();
            identifier.setIssuer(record.getIssuer());
            identifier.setAssigner(record.getAssigner());
            identifier.setId(record.getIdValue());
            identifier.setType(record.getTypeName());
            identifierList.add(identifier);
        });

        return identifierList;
    }

    /**
     * compare two lists of identifiers for logical equality.
     * @param identifiersFromDB
     * @param identifiers
     * @return
     */
    public boolean compare(List<DvIdentifier> identifiersFromDB, List<DvIdentifier> identifiers){

        if (identifiersFromDB == null && identifiers == null)
            return true;

        if ((identifiersFromDB == null && identifiers != null) || (identifiersFromDB != null && identifiers == null))
            return false;

        if (identifiersFromDB.size() != identifiers.size())
            return false;

        List<DvIdentifier> filteredList =
        identifiersFromDB
                .stream()
                .filter(identifier -> identifiers.stream()
                        .anyMatch(identifier1 ->
                                Objects.equals(identifier1.getType(),identifier.getType()) &&
                                Objects.equals(identifier1.getId(),identifier.getId()) &&
                                Objects.equals(identifier1.getAssigner(),identifier.getAssigner()) &&
                                Objects.equals(identifier1.getIssuer(), identifier.getIssuer())))
                        .collect(Collectors.toList());

        return filteredList.size() == identifiersFromDB.size();
    }

    /**
     * compare the list of identifiers from a record with a list passed as argument
     * @param partyIdentifiedRecord
     * @param identifiers
     * @return
     */
    public boolean compare(PartyIdentifiedRecord partyIdentifiedRecord, List<DvIdentifier> identifiers){
        List<DvIdentifier> identifiersFromDB = retrieve(partyIdentifiedRecord);

        return compare(identifiersFromDB, identifiers);
    }
}
