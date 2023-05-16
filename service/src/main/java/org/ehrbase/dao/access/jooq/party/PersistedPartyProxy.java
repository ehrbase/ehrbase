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
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.enums.PartyType;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.ehrbase.util.PartyUtils;
import org.jooq.Result;

/**
 * Facade to interact with PartyProxy specialization
 */
public class PersistedPartyProxy {

    I_DomainAccess domainAccess;

    public PersistedPartyProxy(I_DomainAccess domainAccess) {
        this.domainAccess = domainAccess;
    }

    public UUID create(PartyProxy partyProxy, Short sysTenant) {
        if (PartyUtils.isPartySelf(partyProxy)) {
            return new PersistedPartySelf(domainAccess).store(partyProxy, sysTenant);
        } else if (PartyUtils.isPartyRelated(partyProxy)) {
            return new PersistedPartyRelated(domainAccess).store(partyProxy, sysTenant);
        } else if (PartyUtils.isPartyIdentified(partyProxy)) {
            return new PersistedPartyIdentified(domainAccess).store(partyProxy, sysTenant);
        } else {
            throw new InternalServerException(
                    "Unhandled Party type detected:" + partyProxy.getClass().getSimpleName());
        }
    }

    private final BiFunction<Result<PartyIdentifiedRecord>, PartyType, Set<PartyIdentifiedRecord>> partyTypeFilter =
            (result, pt) -> result.stream().filter(r -> r.getPartyType() == pt).collect(Collectors.toSet());

    private final Consumer<Result<PartyIdentifiedRecord>> partyTypeValidator =
            result -> result.stream().forEach(p -> {
                switch (p.getPartyType()) {
                    case party_self:
                    case party_identified:
                    case party_related:
                        return;
                    default:
                        throw new InternalServerException("Inconsistent Party type detected:" + p.getPartyRefType());
                }
            });

    public Collection<PartyProxy> retrieveMany(List<UUID> uuids) {
        Collection<PartyProxy> partyProxies = new ArrayList<>();

        if (uuids.isEmpty()) return partyProxies;

        Result<PartyIdentifiedRecord> result = domainAccess
                .getContext()
                .selectFrom(PARTY_IDENTIFIED)
                .where(PARTY_IDENTIFIED.ID.in(uuids))
                .fetch();

        partyTypeValidator.accept(result);

        Set<PartyIdentifiedRecord> self = partyTypeFilter.apply(result, PartyType.party_self);

        List<PartyProxy> allParties = new PersistedPartySelf(domainAccess).renderMultiple(self);

        Set<PartyIdentifiedRecord> identified = partyTypeFilter.apply(result, PartyType.party_identified);
        allParties.addAll(new PersistedPartyIdentified(domainAccess).renderMultiple(identified));

        Set<PartyIdentifiedRecord> related = partyTypeFilter.apply(result, PartyType.party_related);
        allParties.addAll(new PersistedPartyRelated(domainAccess).renderMultiple(related));

        return allParties;
    }

    public PartyProxy retrieve(UUID id) {
        Iterator<PartyProxy> proxy = retrieveMany(List.of(id)).iterator();
        return proxy.hasNext() ? proxy.next() : null;
    }

    public UUID getOrCreate(PartyProxy partyProxy, Short sysTenant) {
        if (PartyUtils.isPartySelf(partyProxy)) {
            return new PersistedPartySelf(domainAccess).getOrCreate(partyProxy, sysTenant);
        } else if (PartyUtils.isPartyRelated(partyProxy)) {
            return new PersistedPartyRelated(domainAccess).getOrCreate(partyProxy, sysTenant);
        } else if (PartyUtils.isPartyIdentified(partyProxy)) {
            return new PersistedPartyIdentified(domainAccess).getOrCreate(partyProxy, sysTenant);
        } else {
            throw new InternalServerException(
                    "Unhandled Party type detected:" + partyProxy.getClass().getSimpleName());
        }
    }

    /**
     * Get or create a PartyIdentified instance with the given parameters.
     */
    public UUID getOrCreate(
            String name,
            String code,
            String scheme,
            String namespace,
            String type,
            List<DvIdentifier> identifiers,
            Short sysTenant) {
        // Check conformance to openEHR spec
        if (identifiers == null || identifiers.isEmpty()) {
            throw new IllegalArgumentException("Can't create PartyIdentified with invalid list of identifiers.");
        }
        identifiers.forEach(dv -> {
            if (!isValidDvIdentifier(dv))
                throw new IllegalArgumentException("Can't create PartyIdentified with an invalid identifier.");
        });
        // Create and persist object
        var partyIdentified =
                new PartyIdentified(new PartyRef(new GenericId(code, scheme), namespace, type), name, identifiers);
        return getOrCreate(partyIdentified, sysTenant);
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
