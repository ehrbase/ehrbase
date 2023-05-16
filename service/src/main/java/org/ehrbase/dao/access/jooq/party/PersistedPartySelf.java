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

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;

import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.util.UUID;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.enums.PartyType;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.jooq.Record;

/**
 * Manages party_self persistence
 */
class PersistedPartySelf extends PersistedParty {

    PersistedPartySelf(I_DomainAccess domainAccess) {
        super(domainAccess);
    }

    @Override
    public PartyProxy render(PartyIdentifiedRecord partyIdentifiedRecord) {
        PartyRef partyRef = null;

        if (partyIdentifiedRecord.getPartyRefType() != null) {
            ObjectId objectID = new PersistedObjectId().fromDB(partyIdentifiedRecord);
            partyRef = new PartyRef(
                    objectID, partyIdentifiedRecord.getPartyRefNamespace(), partyIdentifiedRecord.getPartyRefType());
        }

        return new PartySelf(partyRef);
    }

    @Override
    public UUID store(PartyProxy partyProxy, Short sysTenant) {

        PartyRefValue partyRefValue = new PartyRefValue(partyProxy).attributes();

        UUID partyIdentifiedUuid = domainAccess
                .getContext()
                .insertInto(
                        PARTY_IDENTIFIED,
                        PARTY_IDENTIFIED.PARTY_REF_NAMESPACE,
                        PARTY_IDENTIFIED.PARTY_REF_VALUE,
                        PARTY_IDENTIFIED.PARTY_REF_SCHEME,
                        PARTY_IDENTIFIED.PARTY_REF_TYPE,
                        PARTY_IDENTIFIED.PARTY_TYPE,
                        PARTY_IDENTIFIED.OBJECT_ID_TYPE,
                        PARTY_IDENTIFIED.SYS_TENANT)
                .values(
                        partyRefValue.getNamespace(),
                        partyRefValue.getValue(),
                        partyRefValue.getScheme(),
                        partyRefValue.getType(),
                        PartyType.party_self,
                        partyRefValue.getObjectIdType(),
                        sysTenant)
                .returning(PARTY_IDENTIFIED.ID)
                .fetchOne()
                .getId();

        return partyIdentifiedUuid;
    }

    @Override
    public UUID findInDB(PartyProxy partyProxy) {
        UUID partySelfUUID = new PersistedPartyRef(domainAccess).findInDB(partyProxy.getExternalRef());

        if (partySelfUUID == null) {
            if (partyProxy.getExternalRef() == null) { // find the generic PARTY_SELF in DB
                Record record = domainAccess
                        .getContext()
                        .fetchAny(
                                PARTY_IDENTIFIED,
                                PARTY_IDENTIFIED
                                        .PARTY_REF_VALUE
                                        .isNull()
                                        .and(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE.isNull())
                                        .and(PARTY_IDENTIFIED.PARTY_REF_SCHEME.isNull())
                                        .and(PARTY_IDENTIFIED.PARTY_REF_TYPE.isNull())
                                        .and(PARTY_IDENTIFIED.PARTY_TYPE.eq(PartyType.party_self)));

                if (record != null) partySelfUUID = ((PartyIdentifiedRecord) record).getId();
            }
        }

        return partySelfUUID;
    }
}
