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

import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.enums.PartyType;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.jooq.Record;

/**
 * PARTY_IDENTIFIED DB operations
 */
public class PersistedPartyIdentified extends PersistedParty {

    public static final String SECURITY_USER_TYPE = "EHRbase Security Authentication User";

    public static final String EHRBASE = "EHRbase";

    public PersistedPartyIdentified(I_DomainAccess domainAccess) {
        super(domainAccess);
    }

    private static final String ERR_MISSING_PROXY = "Missing PartyProxy for PartyIdentifiedRecord[%s]";

    @Override
    public PartyProxy render(PartyIdentifiedRecord partyIdentifiedRecord) {
        return renderMultiple(List.of(partyIdentifiedRecord)).stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(String.format(ERR_MISSING_PROXY, partyIdentifiedRecord.getId())));
    }

    @Override
    public List<PartyProxy> renderMultiple(Collection<PartyIdentifiedRecord> partyIdentifiedRecords) {

        List<Pair<PartyIdentifiedRecord, List<DvIdentifier>>> partyIdPair =
                new PartyIdentifiers(domainAccess).retrieveMultiple(partyIdentifiedRecords);

        return partyIdPair.stream()
                .map(pair -> {
                    PartyIdentifiedRecord pir = pair.getLeft();

                    PartyRef partyRef = Optional.ofNullable(pir.getPartyRefType())
                            .map(ref -> new PartyRef(
                                    new PersistedObjectId().fromDB(pir),
                                    pir.getPartyRefNamespace(),
                                    pir.getPartyRefType()))
                            .orElse(null);

                    List<DvIdentifier> identifierList = pair.getRight();
                    return new PartyIdentified(
                            partyRef, pir.getName(), identifierList.isEmpty() ? null : identifierList);
                })
                .collect(Collectors.toList());
    }

    @Override
    public UUID store(PartyProxy partyProxy, Short sysTenant) {
        PartyRefValue partyRefValue = new PartyRefValue(partyProxy).attributes();

        // store a new party identified
        UUID partyIdentifiedUuid = domainAccess
                .getContext()
                .insertInto(
                        PARTY_IDENTIFIED,
                        PARTY_IDENTIFIED.NAME,
                        PARTY_IDENTIFIED.PARTY_REF_NAMESPACE,
                        PARTY_IDENTIFIED.PARTY_REF_VALUE,
                        PARTY_IDENTIFIED.PARTY_REF_SCHEME,
                        PARTY_IDENTIFIED.PARTY_REF_TYPE,
                        PARTY_IDENTIFIED.PARTY_TYPE,
                        PARTY_IDENTIFIED.OBJECT_ID_TYPE,
                        PARTY_IDENTIFIED.SYS_TENANT)
                .values(
                        ((PartyIdentified) partyProxy).getName(),
                        partyRefValue.getNamespace(),
                        partyRefValue.getValue(),
                        partyRefValue.getScheme(),
                        partyRefValue.getType(),
                        PartyType.party_identified,
                        partyRefValue.getObjectIdType(),
                        sysTenant)
                .returning(PARTY_IDENTIFIED.ID)
                .fetchOne()
                .getId();
        // store identifiers
        new PartyIdentifiers(domainAccess).store((PartyIdentified) partyProxy, partyIdentifiedUuid, sysTenant);

        return partyIdentifiedUuid;
    }

    /**
     * Retrieve a party identified by:
     * External Ref
     * if none, by matching name and matching identifiers if any
     * @param partyProxy
     * @return
     */
    @Override
    public UUID findInDB(PartyProxy partyProxy) {
        UUID uuid = new PersistedPartyRef(domainAccess).findInDB(partyProxy.getExternalRef());

        // check that name matches the one already stored in DB, otherwise throw an exception (conflicting
        // identification)
        if (uuid != null) {
            Record record = domainAccess.getContext().fetchAny(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(uuid));
            if (record == null) throw new InternalServerException("Inconsistent PartyIdentified UUID:" + uuid);
            if (!record.get(PARTY_IDENTIFIED.NAME).equals(((PartyIdentified) partyProxy).getName()))
                throw new IllegalArgumentException(
                        "Conflicting identification, existing name was:" + record.get(PARTY_IDENTIFIED.NAME)
                                + ", but found passed name:"
                                + ((PartyIdentified) partyProxy).getName());
        }

        return uuid;
    }
}
