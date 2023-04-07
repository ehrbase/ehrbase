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
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.jooq.ContributionAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.jooq.pg.enums.ContributionDataType;

/**
 * Access layer to Contributions
 * Created by Christian Chevalley on 4/21/2015.
 */
public interface I_ContributionAccess extends I_SimpleCRUD {

    /**
     * get a new minimal contribution access layer instance
     *
     * @param domain SQL context
     * @param ehrId  the EHR uuid this contribution belong to
     * @param sysTenant the tenant identifier to which the ContributionAccess object belongs to
     * @return a new minimal {@link I_ContributionAccess}
     */
    static I_ContributionAccess getInstance(I_DomainAccess domain, UUID ehrId, Short sysTenant) {
        return new ContributionAccess(domain, ehrId, sysTenant);
    }

    /**
     * retrieve an instance of I_ContributionAccess layer to the DB
     *
     * @param domainAccess   SQL context
     * @param contributionId the contribution id
     * @return an I_ContributionAccess instance or null
     * @throws InternalServerException on failed fetching of contribution
     */
    static I_ContributionAccess retrieveInstance(I_DomainAccess domainAccess, UUID contributionId) {
        return ContributionAccess.retrieveInstance(domainAccess, contributionId);
    }

    UUID updateWithSignature(String signature);

    /**
     * Commits given input as contribution record. Creation of audit is required beforehand. All parameters are optional and will be provided with default values if NULL.
     *
     * @param transactionTime Timestamp of transaction time
     * @param contributionType String representation of contribution type
     * @param state String representation of contribution state
     * @return @link UUID} of committed contribution
     */
    UUID commit(
            Timestamp transactionTime, ContributionDataType contributionType, ContributionDef.ContributionState state);

    /**
     * Commits given input as contribution record. Embeds creation of audit for this commit. All parameters are optional and will be provided with default values if NULL.
     *
     * @param transactionTime        Timestamp of transaction time
     * @param committerId            ID of committer
     * @param systemId               ID of committing system
     * @param contributionType       String representation of contribution type
     * @param contributionState      String representation of contribution state
     * @param contributionChangeType String representation of contribution change type
     * @param description            Description
     * @return {@link UUID} of committed contribution
     * @throws InternalServerException when contribution couldn't be created because of an internal problem
     */
    UUID commit(
            Timestamp transactionTime,
            UUID committerId,
            UUID systemId,
            ContributionDataType contributionType,
            ContributionDef.ContributionState contributionState,
            I_ConceptAccess.ContributionChangeType contributionChangeType,
            String description);

    /**
     * Update with embedded audit update.
     * @param transactionTime        Timestamp of transaction time
     * @param committerId            ID of committer (part of AuditDetails)
     * @param systemId               ID of committing system (part of AuditDetails)
     * @param contributionType       String representation of contribution type
     * @param contributionState      String representation of contribution state
     * @param contributionChangeType String representation of contribution change type (part of AuditDetails)
     * @param description            Description (part of AuditDetails)
     * @return  True for success
     */
    Boolean update(
            Timestamp transactionTime,
            UUID committerId,
            UUID systemId,
            String contributionType,
            String contributionState,
            String contributionChangeType,
            String description);

    /**
     * Update with embedded audit update.
     * @param transactionTime        Timestamp of transaction time
     * @param committerId            ID of committer (part of AuditDetails)
     * @param systemId               ID of committing system (part of AuditDetails)
     * @param contributionType       ContributionDataType representation of contribution type
     * @param contributionState      ContributionState representation of contribution state
     * @param contributionChangeType ContributionChangeType representation of contribution change type (part of AuditDetails)
     * @param description            Description (part of AuditDetails)
     * @return True for success
     */
    Boolean update(
            Timestamp transactionTime,
            UUID committerId,
            UUID systemId,
            ContributionDataType contributionType,
            ContributionDef.ContributionState contributionState,
            I_ConceptAccess.ContributionChangeType contributionChangeType,
            String description);

    /**
     * commit the contribution with a certifying signature<br>
     * the signature is stored in the Contribution Version entry, the state of the contribution is then 'complete'
     *
     * @param signature String representing the certification
     * @return UUID of committed contribution
     */
    UUID commitWithSignature(String signature);

    /**
     * get the contribution UUID
     *
     * @return ID of contribution
     */
    UUID getContributionId();

    ContributionDataType getContributionDataType();

    void setContributionDataType(ContributionDataType contributionDataType);

    /**
     * set the state of contribution
     *
     * @param state ContributionDef
     * @see ContributionDef
     */
    void setState(ContributionDef.ContributionState state);

    /**
     * set the contribution as complete
     */
    void setComplete();

    /**
     * set the contribution as incomplete
     */
    void setIncomplete();

    /**
     * set the contribution as deleted
     */
    void setDeleted();

    /**
     * get the contribution type
     *
     * @return type
     * @see ContributionDef.ContributionType
     */
    ContributionDef.ContributionType getContributionType();

    /**
     * get the contribution state
     *
     * @return state
     * @see ContributionDef.ContributionState
     */
    ContributionDef.ContributionState getContributionState();

    /**
     * get the contribution Ehr Id it belongs to
     *
     * @return Ehr UUID
     */
    UUID getEhrId();

    /**
     * set the contribution Ehr Id it belongs to
     *
     */
    void setEhrId(UUID ehrId);

    /**
     * get the contribution system tenant
     */
    Short getSysTenant();

    String getDataType();

    void setDataType(ContributionDataType contributionDataType);

    UUID getId();

    /**
     * Convenience setter for contribution's audit
     * @param committer     committer ID (Party Identified)
     * @param system        system on which this is initiated
     * @param description   description
     * @param changeType    change type
     */
    void setAuditDetailsValues(UUID committer, UUID system, String description, ContributionChangeType changeType);

    void setAuditDetailsValues(AuditDetails auditObject);

    void setAuditDetailsChangeType(UUID changeType);

    void setAuditDetailsCommitter(UUID committer);

    void setAuditDetailsSystemId(UUID system);

    void setAuditDetailsDescription(String description);

    UUID getAuditsCommitter();

    UUID getAuditsSystemId();

    String getAuditsDescription();

    ContributionChangeType getAuditsChangeType();

    void setHasAuditDetails(UUID auditId);

    UUID getHasAuditDetails();

    /**
     * Invoke physical deletion.
     */
    void adminDelete();
}
