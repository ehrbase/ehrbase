/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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
package org.ehrbase.dao.access.interfaces;

import com.nedap.archie.rm.datastructures.ItemStructure;
import org.ehrbase.dao.access.jooq.StatusAccess;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Status access layer interface<br>
 * the status entry holds data pertaining to an Ehr owner, generally a patient
 * Created by Christian Chevalley on 4/21/2015.
 */
public interface I_StatusAccess extends I_SimpleCRUD {

    /**
     * retrieve a status by given status ID
     *
     * @param domainAccess  SQL access
     * @param statusId      Id of an status to retrieve
     * @return UUID or null
     */
    static I_StatusAccess retrieveInstance(I_DomainAccess domainAccess, UUID statusId) {
        return StatusAccess.retrieveInstance(domainAccess, statusId);
    }

    /**
     * retrieve a status by an identified party id
     *
     * @param domainAccess    SQL access
     * @param partyIdentified Id of an identified party
     * @return UUID or null
     */
    static I_StatusAccess retrieveInstanceByParty(I_DomainAccess domainAccess, UUID partyIdentified) {
        return StatusAccess.retrieveInstanceByParty(domainAccess, partyIdentified);
    }

    /**
     * retrieve latest status by given EHR ID
     *
     * @param domainAccess  SQL access
     * @param ehrId         Id of associated EHR
     * @return UUID or null
     */
    static I_StatusAccess retrieveInstanceByEhrId(I_DomainAccess domainAccess, UUID ehrId) {
        return StatusAccess.retrieveInstanceByEhrId(domainAccess, ehrId);
    }

    /**
     * retrieve a status for a named subject (patient)<br>
     * NB. for security reason, most deployment will not provide an explicit subject name, this method is provided
     * for small deployment or test purpose.
     *
     * @param domainAccess SQL access
     * @param partyName    a subject name
     * @return UUID or null
     */
    static I_StatusAccess retrieveInstanceByNamedSubject(I_DomainAccess domainAccess, String partyName) {
        return StatusAccess.retrieveInstanceByNamedSubject(domainAccess, partyName);
    }

    /**
     * Commit this status instance.
     * @param transactionTime Time of transaction
     * @param ehrId Associated EHR
     * @param otherDetails Object representation of otherDetails
     * @return ID of DB entry if successful
     */
    UUID commit(Timestamp transactionTime, UUID ehrId, ItemStructure otherDetails);

    /**
     * commit this instance, which has contribution already set with setContributionId(...) beforehand
     * @param transactionTime Time of transaction
     * @param ehrId Associated EHR
     * @param otherDetails Object representation of otherDetails
     * @return ID of DB entry if successful
     */
    UUID commitWithCustomContribution(Timestamp transactionTime, UUID ehrId, ItemStructure otherDetails);

    /**
     * Update this status instance.
     * @param otherDetails Object representation of otherDetails
     * @param transactionTime Time of transaction
     * @param force Option to force
     * @return True if successful
     */
    Boolean update(ItemStructure otherDetails, Timestamp transactionTime, boolean force);

    UUID getId();

    void setStatusRecord(StatusRecord record);

    StatusRecord getStatusRecord();

    void setAuditDetailsAccess(I_AuditDetailsAccess auditDetailsAccess);

    void setContributionAccess(I_ContributionAccess contributionAccess);

    I_AuditDetailsAccess getAuditDetailsAccess();

    UUID getAuditDetailsId();

    void setContributionId(UUID contribution);

    UUID getContributionId();

    /**
     * Helper that sets values in Status' direct audit and Status' implicit contribution audit
     * @param systemId ID of committing system
     * @param committerId ID of committer
     * @param description Optional description
     */
    void setAuditAndContributionAuditValues(UUID systemId, UUID committerId, String description);

    /**
     * Get latest version number of EHR_STATUS by versioned object UID.
     * @param domainAccess access
     * @param statusId versioned object UID
     * @return version number
     */
    static Integer getLatestVersionNumber(I_DomainAccess domainAccess, UUID statusId) {
        return StatusAccess.getLatestVersionNumber(domainAccess, statusId);
    }

    /**
     * Get a specific version number of the associated EHR_STATUS of this instance by timestamp.
     * General idea behind the algorithm: 'what version was the top version at moment T?'
     * @param time Timestamp
     * @return version number
     */
    int getEhrStatusVersionFromTimeStamp(Timestamp time);

    /**
     * Get initial time (or time of oldest record) of the status object of this instance.
     * @return time as {@link Timestamp}
     */
    Timestamp getInitialTimeOfVersionedEhrStatus();
}
