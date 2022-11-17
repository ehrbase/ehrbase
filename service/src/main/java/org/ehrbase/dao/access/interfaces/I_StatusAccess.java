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

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.jooq.StatusAccess;
import org.ehrbase.jooq.pg.tables.records.StatusHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;

/**
 * Status access layer interface<br>
 * the status entry holds data pertaining to an Ehr owner, generally a patient
 * Created by Christian Chevalley on 4/21/2015.
 */
public interface I_StatusAccess extends I_VersionedCRUD, I_Compensatable {

    public static I_StatusAccess retrieveByVersion(I_DomainAccess domainAccess, UUID statusId, int version) {
        return StatusAccess.retrieveByVersion(domainAccess, statusId, version);
    }

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
     * Retrieve a map of status accesses for all statuses referencing a contribution
     *
     * @param domainAccess   SQL context, knowledge
     * @param contributionId contribution object uuid
     * @param node Name of local node, for creation of object version ID
     * @return a map of {@link I_StatusAccess} and their version ID, that match the condition
     * @throws IllegalArgumentException on DB inconsistency
     */
    static Map<ObjectVersionId, I_StatusAccess> retrieveInstanceByContribution(
            I_DomainAccess domainAccess, UUID contributionId, String node) {
        return StatusAccess.retrieveInstanceByContribution(domainAccess, contributionId, node);
    }

    UUID getId();

    void setStatusRecord(StatusRecord record);

    void setStatusRecord(StatusHistoryRecord input);

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
     * @param changeType Change type of operation
     */
    void setAuditAndContributionAuditValues(
            UUID systemId, UUID committerId, String description, ContributionChangeType changeType);

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

    /**
     * Checks existence of given EHR_STATUS.
     * @param domainAccess domain access
     * @param ehrStatusId given EHR_STATUS
     * @return True if object with ID exists, false if not
     */
    static boolean exists(I_DomainAccess domainAccess, UUID ehrStatusId) {
        return StatusAccess.exists(domainAccess, ehrStatusId);
    }

    /**
     * Get complete version list, mapped to their version number.
     * @param domainAccess Access
     * @param statusId
     * @return
     */
    static Map<Integer, I_StatusAccess> getVersionMapOfStatus(I_DomainAccess domainAccess, UUID statusId) {
        return StatusAccess.getVersionMapOfStatus(domainAccess, statusId);
    }

    /**
     * Get current record as {@link EhrStatus} representation.
     * @return Current status object
     */
    EhrStatus getStatus();

    void setOtherDetails(ItemStructure otherDetails);

    ItemStructure getOtherDetails();

    void setEhrId(UUID ehrId);

    UUID getEhrId();
}
