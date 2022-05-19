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

import com.nedap.archie.rm.support.identification.*;
import java.util.UUID;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.jooq.Record;

/**
 * Manages persisted PartyRef
 */
public class PersistedPartyRef {

    I_DomainAccess domainAccess;

    public PersistedPartyRef(I_DomainAccess domainAccess) {
        this.domainAccess = domainAccess;
    }

    /**
     * find matching Party for a given PartyRef depending on object_id specialization
     * @param partyRef
     * @return
     */
    public UUID findInDB(PartyRef partyRef) {
        if (partyRef == null) return null;

        Object ref = partyRef.getId();
        Record record;

        if (ref instanceof ObjectVersionId || ref instanceof HierObjectId) {

            ObjectId objectId = (ObjectId) ref;

            record = domainAccess
                    .getContext()
                    .fetchAny(
                            PARTY_IDENTIFIED,
                            PARTY_IDENTIFIED
                                    .PARTY_REF_NAMESPACE
                                    .eq(partyRef.getNamespace())
                                    .and(PARTY_IDENTIFIED.PARTY_REF_VALUE.eq(objectId.getValue())));

        } else if (ref instanceof GenericId) {
            GenericId genericId = (GenericId) ref;

            record = domainAccess
                    .getContext()
                    .fetchAny(
                            PARTY_IDENTIFIED,
                            PARTY_IDENTIFIED
                                    .PARTY_REF_NAMESPACE
                                    .eq(partyRef.getNamespace())
                                    .and(PARTY_IDENTIFIED.PARTY_REF_SCHEME.eq(genericId.getScheme()))
                                    .and(PARTY_IDENTIFIED.PARTY_REF_VALUE.eq(genericId.getValue())));
        } else
            throw new IllegalStateException(
                    "Unsupported PartyRef identification:" + ref.getClass().getSimpleName());

        if (record != null) return (UUID) record.get("id");
        else return null;
    }

    /**
     * retrieve an assumed partyRef with GenericId
     * @param value
     * @param scheme
     * @param namespace
     * @param type
     * @return
     */
    public UUID findInDB(String value, String scheme, String namespace, String type) {
        PartyRef partyRef = new PartyRef(new GenericId(value, scheme), namespace, type);
        return findInDB(partyRef);
    }

    /**
     * to retrieve a subject with subject_id and namespace (REST API requirement)
     * @param id
     * @param namespace
     * @return
     */
    public UUID findInDB(String id, String namespace) {
        PartyRef partyRef = new PartyRef(new HierObjectId(id), namespace, "PERSON");
        return findInDB(partyRef);
    }
}
