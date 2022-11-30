/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.dao.access.interfaces;

import java.util.UUID;
import org.ehrbase.dao.access.jooq.CompositionAccess;
import org.ehrbase.dao.access.jooq.CompositionHistoryAccess;
import org.ehrbase.jooq.pg.tables.records.CompositionHistoryRecord;

/**
 * Stripped down DAO interface to perform some `*_history` table related actions. Composition access handling in general is
 * done through the {@link CompositionAccess} class.
 */
public interface I_CompositionHistoryAccess extends I_SimpleCRUD, I_Compensatable {

    /**
     * Creates DAO object with the latest existing history record matching the given composition ID.
     * @param domainAccess Config
     * @param compositionId Composition ID
     * @return DAO instance or null if no history is available
     */
    static I_CompositionHistoryAccess retrieveLatest(I_DomainAccess domainAccess, UUID compositionId) {
        return CompositionHistoryAccess.retrieveLatest(domainAccess, compositionId);
    }

    static I_CompositionHistoryAccess retrieveByVersion(I_DomainAccess domainAccess, UUID compositionId, int version) {
        return CompositionHistoryAccess.retrieveByVersion(domainAccess, compositionId, version);
    }

    void setRecord(CompositionHistoryRecord record);

    CompositionHistoryRecord getRecord();

    void setInContribution(UUID contribution);

    void setHasAudit(UUID audit);
}
