/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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
package org.ehrbase.dao.access.interfaces;

import org.ehrbase.dao.access.jooq.PartyIdentifiedAccess;
import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.PartyRef;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.IDENTIFIER;
import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;

/**
 * Party Identified access layer
 * Created by Christian Chevalley on 4/21/2015.
 */
public interface I_PartyIdentifiedAccess extends I_SimpleCRUD<I_PartyIdentifiedAccess, UUID> {

    /**
     * get a new access layer instance for a party name
     *
     * @param domain    SQL access
     * @param partyName a subject name or a dummy string...
     * @return an new access layer instance
     */
    static I_PartyIdentifiedAccess getInstance(I_DomainAccess domain, String partyName) {
        return new PartyIdentifiedAccess(domain.getContext(), domain.getServerConfig(), partyName);
    }


    /**
     * retrieve an instance by its name<br>
     * should be used for test purpose as the subject should not be known in this context
     *
     * @param domainAccess SQL access
     * @param partyName    a subject name
     * @return
     */
    static UUID retrievePartyIdByPartyName(I_DomainAccess domainAccess, String partyName) {
        return PartyIdentifiedAccess.retrievePartyIdByPartyName(domainAccess, partyName);
    }

    /**
     * retrieve a party identified from its UUID
     *
     * @param domainAccess SQL access
     * @param id           UUID
     * @return an access layer instance
     */
    static I_PartyIdentifiedAccess retrieveInstance(I_DomainAccess domainAccess, UUID id) {
        return PartyIdentifiedAccess.retrieveInstance(domainAccess, id);
    }

    /**
     * delete an instance and all its identifiers
     *
     * @param domainAccess SQL access
     * @param id           UUID
     * @return number of records deleted
     */
    static int deleteInstance(I_DomainAccess domainAccess, UUID id) {
        domainAccess.getContext().delete(IDENTIFIER).where(IDENTIFIER.PARTY.eq(id)).execute();
        return domainAccess.getContext().delete(PARTY_IDENTIFIED).where(PARTY_IDENTIFIED.ID.eq(id)).execute();
    }

    /**
     * get the list of identifiers for a party
     *
     * @param domainAccess
     * @param partyId
     * @return
     */
    static List<DvIdentifier> getPartyIdentifiers(I_DomainAccess domainAccess, UUID partyId) {
        List<DvIdentifier> resList = new ArrayList<>();
        domainAccess.getContext().selectFrom(IDENTIFIER).where(IDENTIFIER.PARTY.eq(partyId)).fetch().forEach(records -> {
            DvIdentifier identifier = new DvIdentifier();
            identifier.setIssuer(records.getIssuer());
            identifier.setAssigner(records.getAssigner());
            identifier.setId(records.getIdValue());
            identifier.setType(records.getTypeName());
            resList.add(identifier);
        });

        return resList;
    }

    /**
     * retrieve an identified party from its identification code and issuer
     *
     * @param domainAccess SQL access
     * @param value        issued identification code
     * @param issuer       authority issuing the code
     * @return UUID of identified party or null
     */
    static UUID retrievePartyByIdentifier(I_DomainAccess domainAccess, String value, String issuer) {
        UUID uuid = domainAccess.getContext().fetchAny(IDENTIFIER, IDENTIFIER.ID_VALUE.eq(value).and(IDENTIFIER.ISSUER.eq(issuer))).getParty();

        return uuid;

    }

    /**
     * retrieve or create a party with an identifier (NB. a party has n identifiers)
     *
     * @param domainAccess
     * @param name         the party name
     * @param value        the identifier value
     * @param issuer       identifier issuer
     * @param assigner     identifier assigner
     * @param type         identifier type
     * @return
     */
    static UUID getOrCreateParty(I_DomainAccess domainAccess, String name, String value, String issuer, String assigner, String type) {
        return PartyIdentifiedAccess.getOrCreateParty(domainAccess, name, value, issuer, assigner, type);
    }

    /**
     * retrieve or create a party by an external reference
     *
     * @param domainAccess
     * @param name         the party name (optional)
     * @param code         the external ref value
     * @param scheme       the external ref  scheme
     * @param namespace    the external ref namespace
     * @param type         the external ref type
     * @return
     */
    static UUID getOrCreatePartyByExternalRef(I_DomainAccess domainAccess, String name, String code, String scheme, String namespace, String type) {
        PartyIdentified partyIdentified = new PartyIdentified(new PartyRef(new GenericId(code, scheme), namespace, type), name, null);
        return PartyIdentifiedAccess.getOrCreateParty(domainAccess, partyIdentified);
    }

    /**
     * retrieve or create a party with a PartyIdentified (external ref)
     *
     * @param domainAccess
     * @param partyIdentified
     * @return
     */
    static UUID getOrCreateParty(I_DomainAccess domainAccess, PartyIdentified partyIdentified) {
        return PartyIdentifiedAccess.getOrCreateParty(domainAccess, partyIdentified);
    }

    /**
     * retrieve an identified party by its UUID
     *
     * @param domainAccess
     * @param id
     * @return
     */
    static PartyIdentified retrievePartyIdentified(I_DomainAccess domainAccess, UUID id) {
        return PartyIdentifiedAccess.retrievePartyIdentified(domainAccess, id);
    }

    /**
     * retrieve an identified party from its identifier list. The list can be partial.
     *
     * @param domainAccess
     * @param identifierList
     * @return
     */
    static UUID findIdentifiedParty(I_DomainAccess domainAccess, List<DvIdentifier> identifierList) {
        return PartyIdentifiedAccess.findIdentifiedParty(domainAccess.getContext(), identifierList);
    }

    /**
     * retrieve a party by a party reference (NB. party ref Type is ignored)
     *
     * @param domainAccess
     * @param partyRef
     * @return
     */
    static UUID findReferencedParty(I_DomainAccess domainAccess, PartyRef partyRef) {
        return PartyIdentifiedAccess.findReferencedParty(domainAccess.getContext(), partyRef);
    }

    /**
     * retrieve a party by a party reference (NB. party ref Type is ignored)
     *
     * @param domainAccess
     * @param value        party ref value
     * @param scheme       party ref scheme
     * @param namespace    party ref namespace
     * @param type         party ref type
     * @return
     */
    static UUID findReferencedParty(I_DomainAccess domainAccess, String value, String scheme, String namespace, String type) {
        PartyRef partyRef = new PartyRef(new GenericId(value, scheme), namespace, type);
        return PartyIdentifiedAccess.findReferencedParty(domainAccess.getContext(), partyRef);
    }

    /**
     * add an identifier to a party
     *
     * @param value    a subject id code
     * @param issuer   the authority issuing the subject Id Code (ex. NHS)
     * @param assigner the authority that assign the id to the identified item
     * @param type     a descriptive literal following a conventional vocabulary (SSN, prescription  etc.)
     * @return number of record added
     */
    Integer addIdentifier(String value, String issuer, String assigner, String type);

    /**
     * delete a specific identifier for the current party
     *
     * @param idCode the subject code
     * @param issuer the issuer id
     * @return number of record deleted
     */
    Integer deleteIdentifier(String idCode, String issuer);

    /**
     * get the party name
     *
     * @return
     */
    String getPartyName();

    /**
     * set the party name
     *
     * @param name
     */
    void setPartyName(String name);

    String getPartyRefValue();

    String getPartyRefNamespace();

    /**
     * get the list of identifier keys<br>
     * a key is formatted as 'code:issuer'
     *
     * @return
     */
    String[] getIdentifiersKeySet();

    UUID getId();
}
