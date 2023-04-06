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

import com.nedap.archie.rm.generic.AuditDetails;
import java.sql.Timestamp;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.jooq.AuditDetailsAccess;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.tables.records.AuditDetailsRecord;

public interface I_AuditDetailsAccess extends I_SimpleCRUD {

    /**
     * get a new minimal AuditDetails access layer instance
     *
     * @param dataAccess      general data access
     * @return new access instance
     */
    static I_AuditDetailsAccess getInstance(I_DomainAccess dataAccess, Short sysTenant) {
        return new AuditDetailsAccess(dataAccess, sysTenant);
    }

    /**
     * get a new AuditDetails access layer instance
     *
     * @param dataAccess    general data access
     * @param systemId      system on which this is initiated
     * @param committer     committer ID (Party Identified)
     * @param changeType    audit change type, indicating creation, modification and so on
     * @param description
     * @return new access instance
     * @throws InternalServerException if creating or retrieving system failed
     */
    static I_AuditDetailsAccess getInstance(
            I_DomainAccess dataAccess,
            UUID systemId,
            UUID committer,
            I_ConceptAccess.ContributionChangeType changeType,
            String description,
            Short sysTenant) {
        return new AuditDetailsAccess(dataAccess, systemId, committer, changeType, description, sysTenant);
    }

    /**
     * Retrieve a specific audit instance via UUID
     * @param dataAccess    general data access
     * @param auditId ID of audit to retrieve
     * @return access to instance
     * @throws InternalServerException when retrieval failed
     */
    I_AuditDetailsAccess retrieveInstance(I_DomainAccess dataAccess, UUID auditId);

    /**
     * Convenience commit, that sets values on an empty/minimal {@link AuditDetailsAccess} before committing. Assumes creation as change type.
     * @param systemId      system on which this is initiated
     * @param committerId   committer ID (Party Identified)
     * @param description   optional description
     * @return ID of created audit DB entry
     * @throws IllegalArgumentException when systemId or committerId aren't set
     */
    UUID commit(UUID systemId, UUID committerId, String description);

    /**
     * Update method with all optional parameters to only set and invoke update with given parameters.
     * @param systemId Optional
     * @param committer Optional committer ID (Party Identified)
     * @param changeType Optional audit change type, indicating creation, modification and so on
     * @param description Optional
     * @return Indicating success of operation
     */
    Boolean update(
            UUID systemId, UUID committer, I_ConceptAccess.ContributionChangeType changeType, String description);

    UUID getId();

    void setSystemId(UUID systemId);

    UUID getSystemId();

    /**
     * @param committer a Party Identified
     */
    void setCommitter(UUID committer);

    UUID getCommitter();

    void setChangeType(UUID changeType);

    void setChangeType(I_ConceptAccess.ContributionChangeType changeType);

    ContributionChangeType getChangeType();

    void setDescription(String description);

    String getDescription();

    Timestamp getTimeCommitted();

    String getTimeCommittedTzId();

    void setRecord(AuditDetailsRecord record);

    AuditDetails getAsAuditDetails();

    Short getSysTenant();
}
