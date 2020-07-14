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

import com.nedap.archie.rm.generic.PartyProxy;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;

import java.util.UUID;

public interface I_PersistedParty {

    /**
     * Render a PartyProxy from a retrieved DB record (from PartyIdentified table)
     * @param partyIdentifiedRecord
     * @return
     */
    PartyProxy render(PartyIdentifiedRecord partyIdentifiedRecord);

    /**
     * store a party proxy relatively to its actual type
     * @param partyProxy
     * @return
     */
    UUID store(PartyProxy partyProxy);

    /**
     * find an existing party proxy or null if none
     * @param partyProxy
     * @return
     */
    UUID findInDB(PartyProxy partyProxy);

    /**
     * retrieve a party proxy from the DB or create a new one if not yet stored
     * @param partyProxy
     * @return
     */
    UUID getOrCreate(PartyProxy partyProxy);
}
