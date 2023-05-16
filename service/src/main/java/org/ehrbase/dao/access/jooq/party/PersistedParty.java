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

import com.nedap.archie.rm.generic.PartyProxy;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;

/**
 * Abstract class for PartyProxy DB operations
 */
public abstract class PersistedParty implements I_PersistedParty {

    I_DomainAccess domainAccess;

    public PersistedParty(I_DomainAccess domainAccess) {
        this.domainAccess = domainAccess;
    }

    @Override
    public UUID findInDB(PartyProxy partyProxy) {
        return null;
    }

    @Override
    public UUID store(PartyProxy partyProxy, Short sysTenant) {
        return null;
    }

    @Override
    public UUID getOrCreate(PartyProxy partyProxy, Short sysTenant) {
        return Optional.ofNullable(findInDB(partyProxy)).orElseGet(() -> store(partyProxy, sysTenant));
    }
}
