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

import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartyRelated;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.jooq.rmdatavalue.JooqDvCodedText;
import org.ehrbase.jooq.pg.enums.PartyType;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.ehrbase.service.PersistentCodePhrase;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;

/**
 * Manages PartyRelated persistence, in particular handles attribute 'relationship' (DvCodedText)
 * TODO: relationship should be normalized (e.g. in another table) since the same person can play different relationship roles (mother, spouse etc.)
 *
 */
class PersistedPartyRelated extends PersistedParty {

    PersistedPartyRelated(I_DomainAccess domainAccess) {
        super(domainAccess);
    }


    @Override
    public PartyProxy render(PartyIdentifiedRecord partyIdentifiedRecord) {
        //a party identified with a relationship!

        PartyIdentified partyIdentified = (PartyIdentified)new PersistedPartyIdentified(domainAccess).render(partyIdentifiedRecord);

        PartyRelated partyRelated = new PartyRelated();

        partyRelated.setExternalRef(partyIdentified.getExternalRef());
        partyRelated.setName(partyIdentified.getName());
        partyRelated.setIdentifiers(partyIdentified.getIdentifiers());
        partyRelated.setRelationship(new JooqDvCodedText(partyIdentifiedRecord.getRelationship()).toRmInstance());

        return partyRelated;
    }

    @Override
    public UUID store(PartyProxy partyProxy) {
        PartyRefValue partyRefValue = new PartyRefValue(partyProxy).attributes();

        //store a new party identified
        UUID partyIdentifiedUuid = domainAccess.getContext()
                .insertInto(PARTY_IDENTIFIED,
                        PARTY_IDENTIFIED.NAME,
                        PARTY_IDENTIFIED.PARTY_REF_NAMESPACE,
                        PARTY_IDENTIFIED.PARTY_REF_VALUE,
                        PARTY_IDENTIFIED.PARTY_REF_SCHEME,
                        PARTY_IDENTIFIED.PARTY_REF_TYPE,
                        PARTY_IDENTIFIED.PARTY_TYPE,
                        PARTY_IDENTIFIED.OBJECT_ID_TYPE,
                        PARTY_IDENTIFIED.RELATIONSHIP)
                .values(((PartyIdentified)partyProxy).getName(),
                        partyRefValue.getNamespace(),
                        partyRefValue.getValue(),
                        partyRefValue.getScheme(),
                        partyRefValue.getType(),
                        PartyType.party_related,
                        partyRefValue.getObjectIdType(),
                        relationshipAsRecord(partyProxy))
                .returning(PARTY_IDENTIFIED.ID)
                .fetchOne().getId();
        //store identifiers
        new PartyIdentifiers(domainAccess).store((PartyIdentified)partyProxy, partyIdentifiedUuid);

        return partyIdentifiedUuid;
    }

    @Override
    public UUID findInDB(PartyProxy partyProxy) {
        UUID uuid = new PersistedPartyRef(domainAccess).findInDB(partyProxy.getExternalRef());

        //see https://www.postgresql.org/docs/11/rowtypes.html for syntax on accessing specific attributes in UDT
        if (uuid == null){
            if (partyProxy.getExternalRef() == null) { //check for the same name and same relationship
                Record record = domainAccess.getContext().fetchAny(PARTY_IDENTIFIED,
                        PARTY_IDENTIFIED.PARTY_REF_VALUE.isNull()
                                .and(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE.isNull())
                                .and(PARTY_IDENTIFIED.PARTY_REF_SCHEME.isNull())
                                .and(PARTY_IDENTIFIED.PARTY_REF_TYPE.isNull())
                                .and(PARTY_IDENTIFIED.NAME.eq(((PartyIdentified) partyProxy).getName()))
                                .and(DSL.field("("+PARTY_IDENTIFIED.RELATIONSHIP+").value").eq(relationshipAsRecord(partyProxy).getValue()))
                                .and(PARTY_IDENTIFIED.PARTY_TYPE.eq(PartyType.party_related)));

                if (record != null) {
                    uuid = ((PartyIdentifiedRecord) record).getId();
                    //check for identifiers
                    if (!new PartyIdentifiers(domainAccess).compare((PartyIdentifiedRecord) record, ((PartyRelated) partyProxy).getIdentifiers()))
                        uuid = null;
                }
            }
        }

        return uuid;
    }

    private DvCodedTextRecord relationshipAsRecord(PartyProxy partyProxy){

        DvCodedText relationship = ((PartyRelated)partyProxy).getRelationship();

        return new DvCodedTextRecord(relationship.getValue(),
                        new PersistentCodePhrase(relationship.getDefiningCode()).encode(), relationship.getFormatting(),
                        new PersistentCodePhrase(relationship.getLanguage()).encode(),
                        new PersistentCodePhrase(relationship.getEncoding()).encode());
    }
}
