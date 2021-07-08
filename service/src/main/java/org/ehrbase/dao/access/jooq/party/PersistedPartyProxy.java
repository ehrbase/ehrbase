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
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.util.List;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;

import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;

/**
 * Facade to interact with PartyProxy specialization
 */
public class PersistedPartyProxy {

    I_DomainAccess domainAccess;

    public PersistedPartyProxy(I_DomainAccess domainAccess) {
        this.domainAccess = domainAccess;
    }

    public PartyProxy retrieve(UUID id){
        PartyProxy partyProxy;

        if (!(domainAccess.getContext().fetchExists(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(id))))
            partyProxy =  null;
        else {

            //identify the party type
            PartyIdentifiedRecord identifiedRecord = domainAccess.getContext().fetchOne(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(id));

            switch (identifiedRecord.getPartyType()) {
                case party_self:
                    partyProxy = new PersistedPartySelf(domainAccess).render(identifiedRecord);
                    break;
                case party_identified:
                    partyProxy = new PersistedPartyIdentified(domainAccess).render(identifiedRecord);
                    break;
                case party_related:
                    partyProxy = new PersistedPartyRelated(domainAccess).render(identifiedRecord);
                    break;
                default:
                    throw new InternalServerException("Inconsistent Party type detected:" + identifiedRecord.getPartyRefType());
            }
        }
        return partyProxy;
    }

    public UUID getOrCreate(PartyProxy partyProxy){

        UUID partyUUID = null;

        switch (partyProxy.getClass().getSimpleName()){
            case "PartySelf":
                partyUUID = new PersistedPartySelf(domainAccess).getOrCreate(partyProxy);
                break;
            case "PartyIdentified":
                partyUUID = new PersistedPartyIdentified(domainAccess).getOrCreate(partyProxy);
                break;
            case "PartyRelated":
                partyUUID = new PersistedPartyRelated(domainAccess).getOrCreate(partyProxy);
                break;
            default:
                throw new InternalServerException("Unhandled Party type detected:" + partyProxy.getClass().getSimpleName());
        }
        return partyUUID;

    }

    /**
     * Get or create a PartyIdentified instance with the given parameters.
     */
    public UUID getOrCreate(String name, String code, String scheme, String namespace, String type, List<DvIdentifier> identifiers){
        // Check conformance to openEHR spec
        if (identifiers == null || identifiers.isEmpty()) {
            throw new IllegalArgumentException("Can't create PartyIdentified with invalid list of identifiers.");
        }
        identifiers.forEach(dv -> {
            if (!isValidDvIdentifier(dv))
                throw new IllegalArgumentException("Can't create PartyIdentified with an invalid identifier.");
        });
        // Create and persist object
        var partyIdentified = new PartyIdentified(new PartyRef(new GenericId(code, scheme), namespace, type), name, identifiers);
        return getOrCreate(partyIdentified);
    }

    /**
     * Checks conformance to openEHR spec.<br>
     * A DvIdentifier needs to have an ID.
     * @param identifier Input object
     * @return True if minimal valid object instance
     */
    private boolean isValidDvIdentifier(DvIdentifier identifier) {
        return (identifier.getId() != null) && (!identifier.getId().isBlank());
    }
}
