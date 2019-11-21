/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School), and Luis Marco-Ruiz (Hannover Medical School).

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

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.jooq.ContributionAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.jooq.pg.enums.ContributionDataType;

import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;

/**
 * Access layer to Contributions
 * Created by Christian Chevalley on 4/21/2015.
 */
public interface I_ContributionAccess extends I_SimpleCRUD<I_ContributionAccess, UUID> {

    /**
     * get a new minimal contribution access layer instance
     *
     * @param domain SQL context
     * @param ehrId  the EHR uuid this contribution belong to
     * @return a new minimal {@link I_ContributionAccess}
     */
    static I_ContributionAccess getInstance(I_DomainAccess domain, UUID ehrId) {
        return new ContributionAccess(domain.getContext(), domain.getKnowledgeManager(), domain.getIntrospectService(), domain.getServerConfig(), ehrId);
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

    /**
     * retrieve a contribution from its object ID and a timestamp
     * FIXME: note: this only looks for instances in *_history table, see called method for details on problem - also naming suggestively wrong?
     *
     * @param domainAccess      SQL context
     * @param contributionObjId the contribution object UUID
     * @param timestamp         time in history to calculate related version
     * @return an {@link I_ContributionAccess} instance or null
     */
    static I_ContributionAccess retrieveVersionedInstance(I_DomainAccess domainAccess, UUID contributionObjId, Timestamp timestamp) {
        return ContributionAccess.retrieveVersionedInstance(domainAccess, contributionObjId, timestamp);
    }

    /**
     * add a new composition to this contribution<br>
     * NB: The contribution and composition requires commit() to be saved in the DB
     *
     * @param compositionAccess a valid I_CompositionAccess
     * @throws IllegalArgumentException when composition is not complete, e.g. has not embedded audit
     */
    void addComposition(I_CompositionAccess compositionAccess);

    UUID updateWithSignature(String signature);

    /**
     * updateComposition an <b>existing</b> composition<br>
     * only a composition with the same id is effectively updated with this method<br>
     * NB: The contribution and composition requires commit() to be saved in the DB
     *
     * @param compositionAccess access instance
     */
    void updateComposition(I_CompositionAccess compositionAccess);

    /**
     * TODO: doc. what exactly happens or need to happen, i.e. also commit(..) necessary afterwards?
     *
     * @param compositionAccess access instance
     * @return ?
     */
    boolean removeComposition(I_CompositionAccess compositionAccess);

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
    UUID commit(Timestamp transactionTime, UUID committerId, UUID systemId, ContributionDataType contributionType, ContributionDef.ContributionState contributionState, I_ConceptAccess.ContributionChangeType contributionChangeType, String description);

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
    Boolean update(Timestamp transactionTime, UUID committerId, UUID systemId, String contributionType, String contributionState, String contributionChangeType, String description);

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
    Boolean update(Timestamp transactionTime, UUID committerId, UUID systemId, ContributionDataType contributionType, ContributionDef.ContributionState contributionState, I_ConceptAccess.ContributionChangeType contributionChangeType, String description);

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

    I_CompositionAccess getComposition(UUID id);

    Set<UUID> getCompositionIds();

    String getDataType();

    void setDataType(ContributionDataType contributionDataType);

    UUID getId();

    /**
     * Convenience setter for contribution's audit
     * @param committer     committer ID (Party Identified)
     * @param system        system on which this is initiated
     * @param description   description
     */
    void setAuditDetailsValues(UUID committer, UUID system, String description);

    void setAuditDetailsChangeType(UUID changeType);

    void setHasAuditDetails(UUID auditId);

    UUID getHasAuditDetails();
}
