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

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.enums.PartyType;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.ehrbase.util.PartyUtils;
import org.jooq.Result;

import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.PartyRef;

/**
 * Facade to interact with PartyProxy specialization
 */
public class PersistedPartyProxy {

    I_DomainAccess domainAccess;

    public PersistedPartyProxy(I_DomainAccess domainAccess) {
        this.domainAccess = domainAccess;
    }

    public UUID create(PartyProxy partyProxy){
        if (PartyUtils.isPartySelf(partyProxy)) {
            return new PersistedPartySelf(domainAccess).store(partyProxy);
        } else if (PartyUtils.isPartyRelated(partyProxy)) {
            return new PersistedPartyRelated(domainAccess).store(partyProxy);
        } else if (PartyUtils.isPartyIdentified(partyProxy)) {
            return new PersistedPartyIdentified(domainAccess).store(partyProxy);
        } else {
            throw new InternalServerException("Unhandled Party type detected:" + partyProxy.getClass().getSimpleName());
        }
    }

    //------------------------------------------------------------------------------------------------
    
    public Collection<PartyProxy> retrieveMany(UUID...ids) {
      BiFunction<Result<PartyIdentifiedRecord>,PartyType, Set<PartyIdentifiedRecord>> partyTypeFilter =
          (result, pt) -> result.stream().filter(r -> r.getPartyType() == PartyType.party_self).collect(Collectors.toSet());
      
      Collection<PartyProxy> partyProxies = new ArrayList<>();
      
      if(ids.length == 0)
          return partyProxies;
      
      Result<PartyIdentifiedRecord> result = domainAccess.getContext()
          .selectFrom(PARTY_IDENTIFIED)
          .where(PARTY_IDENTIFIED.ID.in(ids))
          .fetch();

      
      Set<PartyIdentifiedRecord> self = partyTypeFilter.apply(result, PartyType.party_self);
      List<PartyProxy> renderMultiple = new PersistedPartySelf(domainAccess).renderMultiple(self);
      
      
      Set<PartyIdentifiedRecord> identified = partyTypeFilter.apply(result, PartyType.party_identified);
      List<PartyProxy> renderMultiple2 = new PersistedPartyIdentified(domainAccess).renderMultiple(identified);
      
      
      Set<PartyIdentifiedRecord> related = partyTypeFilter.apply(result, PartyType.party_related);
      List<PartyProxy> renderMultiple3 = new PersistedPartyRelated(domainAccess).renderMultiple(related);
      
      
      
      
      
      return null;
      
//      fetch2.stream()
//          .filter(pir -> pir != null)
//          .map(pir -> {
//            return null;
//          })
//          .collect(Collectors.toCollection(ArrayList::new));
//      return collect;
    }
    
    //------------------------------------------------------------------------------------------------
    
    public PartyProxy retrieve(UUID id){
        PartyIdentifiedRecord identifiedRecord = domainAccess.getContext().fetchOne(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(id));
        if(identifiedRecord == null)
          return null;

        switch (identifiedRecord.getPartyType()) {
            case party_self:
                return new PersistedPartySelf(domainAccess).render(identifiedRecord);
            case party_identified:
                return new PersistedPartyIdentified(domainAccess).render(identifiedRecord);
            case party_related:
                return new PersistedPartyRelated(domainAccess).render(identifiedRecord);
            default:
                throw new InternalServerException("Inconsistent Party type detected:" + identifiedRecord.getPartyRefType());
        }
    }

    public UUID getOrCreate(PartyProxy partyProxy){
        if (PartyUtils.isPartySelf(partyProxy)) {
            return new PersistedPartySelf(domainAccess).getOrCreate(partyProxy);
        } else if (PartyUtils.isPartyRelated(partyProxy)) {
            return new PersistedPartyRelated(domainAccess).getOrCreate(partyProxy);
        } else if (PartyUtils.isPartyIdentified(partyProxy)) {
            return new PersistedPartyIdentified(domainAccess).getOrCreate(partyProxy);
        } else {
            throw new InternalServerException("Unhandled Party type detected:" + partyProxy.getClass().getSimpleName());
        }
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
