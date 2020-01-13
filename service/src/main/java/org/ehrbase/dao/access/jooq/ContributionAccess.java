/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH,  Hannover Medical School,
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
package org.ehrbase.dao.access.jooq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.*;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.enums.ContributionState;
import org.ehrbase.jooq.pg.tables.records.ContributionHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.ContributionRecord;
import org.ehrbase.service.IntrospectService;
import org.jooq.DSLContext;

import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static org.ehrbase.jooq.pg.Tables.*;

/**
 * Created by Christian Chevalley on 4/17/2015.
 */
public class ContributionAccess extends DataAccess implements I_ContributionAccess {

    private final String signature = "$system$"; //used to sign a contribution during commit
    Logger log = LogManager.getLogger(CompositionAccess.class);
    private ContributionRecord contributionRecord;
    private Map<UUID, I_CompositionAccess> compositions = new HashMap<>();
    private I_AuditDetailsAccess auditDetails; // audit associated with this contribution

    /**
     * Basic constructor for contribution.
     * @param context DB context object of current server context
     * @param knowledgeManager Knowledge cache object of current server context
     * @param introspectCache Introspect cache object of current server context
     * @param serverConfig Server config object of current server context
     * @param ehrId Given ID of EHR this contribution will be created for
     */
    public ContributionAccess(DSLContext context, I_KnowledgeCache knowledgeManager, IntrospectService introspectCache, ServerConfig serverConfig, UUID ehrId) {

        super(context, knowledgeManager, introspectCache, serverConfig);

        this.contributionRecord = context.newRecord(CONTRIBUTION);

        contributionRecord.setEhrId(ehrId);

        // create and attach new minimal audit instance to this contribution
        this.auditDetails = I_AuditDetailsAccess.getInstance(this.getDataAccess());
    }

    /**
     * Constructor with convenient {@link I_DomainAccess} parameter, for better readability.
     * @param domainAccess Current domain access object
     * @param ehrId Given ID of EHR this contribution will be created for
     */
    public ContributionAccess(I_DomainAccess domainAccess, UUID ehrId) {

        super(domainAccess.getContext(), domainAccess.getKnowledgeManager(), domainAccess.getIntrospectService(), domainAccess.getServerConfig());

        this.contributionRecord = domainAccess.getContext().newRecord(CONTRIBUTION);

        contributionRecord.setEhrId(ehrId);

        // create and attach new minimal audit instance to this contribution
        this.auditDetails = I_AuditDetailsAccess.getInstance(this.getDataAccess());
    }

    // internal minimal constructor - needs proper initialization before following usage
    private ContributionAccess(I_DomainAccess domainAccess) {
        super(domainAccess);
    }

    /**
     * @throws InternalServerException on failed fetching of contribution
     */
    public static I_ContributionAccess retrieveInstance(I_DomainAccess domainAccess, UUID contributionId) {

        ContributionAccess contributionAccess = new ContributionAccess(domainAccess);

        try {
            contributionAccess.contributionRecord = domainAccess.getContext().fetchOne(CONTRIBUTION, CONTRIBUTION.ID.eq(contributionId));
        } catch (Exception e) {
            throw new InternalServerException("fetching contribution failed", e);
        }

        if (contributionAccess.contributionRecord == null)
            return null;

        contributionAccess.compositions = CompositionAccess
                .retrieveCompositionsInContributionVersion(domainAccess, contributionAccess.contributionRecord.getId(), 0);

        // also retrieve attached audit
        contributionAccess.auditDetails = new AuditDetailsAccess(domainAccess.getDataAccess()).retrieveInstance(domainAccess.getDataAccess(), contributionAccess.getHasAuditDetails());

        return contributionAccess;

    }

    // FIXME: should this really only look into *_history table? semantically this only accesses "versions" other than latest (not actual openEHR versions, since contribution has none) - also naming suggestively wrong?
    public static I_ContributionAccess retrieveVersionedInstance(I_DomainAccess domainAccess, UUID contributionVersionedObjId, Timestamp transactionTime) {

        ContributionAccess contributionAccess = new ContributionAccess(domainAccess);

        ContributionHistoryRecord contributionHistoryRecord = domainAccess.getContext()
                .fetchOne(CONTRIBUTION_HISTORY,
                        CONTRIBUTION_HISTORY.ID.eq(contributionVersionedObjId)
                                .and(CONTRIBUTION_HISTORY.SYS_TRANSACTION.eq(transactionTime)));

        if (contributionHistoryRecord != null) {
            contributionAccess.contributionRecord = domainAccess.getContext().newRecord(CONTRIBUTION);
            contributionAccess.contributionRecord.from(contributionHistoryRecord);
            return contributionAccess;
        } else
            return null;
    }

    @Override
    public void addComposition(I_CompositionAccess compositionAccess) {
        //set local composition field from this contribution
        compositionAccess.setEhrid(contributionRecord.getEhrId());
        if (compositionAccess.getComposerId() == null)
            compositionAccess.setComposerId(auditDetails.getCommitter());   // FIXME: does this work, is there an audit instance available here? not tested, function not used right now...
        if (compositionAccess.getAuditDetailsId() == null)
            throw new IllegalArgumentException("Composition has no embedded audit");
        compositionAccess.setContributionId(contributionRecord.getId()); //this is the ContributionVersionId!!!

        compositions.put(compositionAccess.getId(), compositionAccess);
    }

    @Override
    public boolean removeComposition(I_CompositionAccess compositionAccess) {
        I_CompositionAccess removed = compositions.remove(compositionAccess.getId());
        return removed != null;
    }

    @Override
    public UUID commit(Timestamp transactionTime) {

        // first create DB entry of auditDetails so they can get referenced in this contribution
        UUID auditId = this.auditDetails.commit();
        contributionRecord.setHasAudit(auditId);

        if (contributionRecord.getState() == ContributionState.incomplete) {
            log.warn("Contribution state has not been set");
        }


//        if (compositions.isEmpty())
//            log.warn("Contribution does not contain any composition...");

        contributionRecord.setSysTransaction(transactionTime);
        contributionRecord.setEhrId(this.getEhrId());
        contributionRecord.store();
        UUID contributionId = contributionRecord.getId();

        //commit the compositions
        for (I_CompositionAccess compositionAccess : compositions.values()) {
            // composition can only be added when having an audit attached, so this is assumed to be the case here
            compositionAccess.commit(transactionTime);
        }

        return contributionId;
    }

    @Override
    public UUID commit() {
        return commit(Timestamp.valueOf(LocalDateTime.now()));
    }

    /**
     * Commit the contribution with (optional) given values (incl. audit data, which is handled embedded)
     * @throws InternalServerException when contribution couldn't be created because of an internal problem
     */
    @Override
    public UUID commit(Timestamp transactionTime, UUID committerId, UUID systemId, ContributionDataType contributionType, ContributionDef.ContributionState state, I_ConceptAccess.ContributionChangeType contributionChangeType, String description) {
        // create new audit_details instance for this contribution
        this.auditDetails = I_AuditDetailsAccess.getInstance(this.getDataAccess());

        if (transactionTime == null) {
            transactionTime = Timestamp.valueOf(LocalDateTime.now());
        }

        //set contribution attributes
        if (contributionType == null)
            setContributionDataType(ContributionDataType.other);
        else
            setContributionDataType(contributionType);

        if (state != null)
            setState(state);
        else
            setState(ContributionDef.ContributionState.COMPLETE);

        // audit attributes
        if (committerId == null) {
            //get current user from JVM
            String defaultUser = System.getProperty("user.name");
            //check for that user in the DB
            java.net.InetAddress localMachine;
            try {
                localMachine = java.net.InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new InternalServerException("Problem while getting information about server", e);
            }
            String scheme = System.getProperty("host.name");
            if (scheme == null)
                scheme = "local";
            committerId = I_PartyIdentifiedAccess.getOrCreatePartyByExternalRef(this, defaultUser, UUID.randomUUID().toString(), scheme, localMachine.getCanonicalHostName(), "PARTY");
        }
        auditDetails.setCommitter(committerId);

        if (systemId != null) {
            auditDetails.setSystemId(systemId);
        } else {
            auditDetails.setSystemId(I_SystemAccess.createOrRetrieveLocalSystem(this));
        }

        if (contributionChangeType != null)
            auditDetails.setChangeType(I_ConceptAccess.fetchContributionChangeType(this, contributionChangeType.name()));
        else
            auditDetails.setChangeType(I_ConceptAccess.fetchContributionChangeType(this, I_ConceptAccess.ContributionChangeType.CREATION));

        if (description != null) {
            auditDetails.setDescription(description);
        }
        return commit(transactionTime);
    }

    @Override
    public Boolean update(Timestamp transactionTime, UUID committerId, UUID systemId, String contributionType, String contributionState, String contributionChangeType, String description) {
        //set contribution  attributes
        ContributionDataType type = null;
        ContributionDef.ContributionState state = null;
        I_ConceptAccess.ContributionChangeType changeType = null;

        if (contributionType == null)
            type = ContributionDataType.valueOf(contributionType);

        if (contributionState != null)
            state = ContributionDef.ContributionState.valueOf(contributionState);

        if (contributionChangeType != null)
            changeType = I_ConceptAccess.ContributionChangeType.valueOf(contributionChangeType);

        // audit handling will be executed centralized in the following called method
        return update(transactionTime, committerId, systemId, type, state, changeType, description);
    }

    @Override
    public Boolean update(Timestamp transactionTime, UUID committerId, UUID systemId, ContributionDataType contributionType, ContributionDef.ContributionState state, I_ConceptAccess.ContributionChangeType contributionChangeType, String description) {
        //set contribution  attributes
        if (contributionType != null)
            setContributionDataType(contributionType);
        if (state != null)
            setState(state);

        // embedded audit handling
        I_AuditDetailsAccess auditDetailsAccess = new AuditDetailsAccess(this.getDataAccess()).retrieveInstance(this.getDataAccess(), getHasAuditDetails());
        if (committerId != null)
            auditDetailsAccess.setCommitter(committerId);
        if (systemId != null)
            auditDetailsAccess.setSystemId(systemId);
        if (contributionChangeType != null)
            auditDetailsAccess.setChangeType(I_ConceptAccess.fetchContributionChangeType(this, contributionChangeType));
        if (description != null)
            auditDetailsAccess.setDescription(description);

        return update(transactionTime);
    }

    @Override
    public UUID commitWithSignature(String signature) {
        contributionRecord.setSignature(signature);
        contributionRecord.setState(ContributionState.valueOf("complete"));
        contributionRecord.store();

        return contributionRecord.getId();
    }

    @Override
    public UUID updateWithSignature(String signature) {
        contributionRecord.setSignature(signature);
        contributionRecord.setState(ContributionState.valueOf("complete"));
        contributionRecord.update();

        return contributionRecord.getId();
    }

    @Override
    public void updateComposition(I_CompositionAccess compositionAccess) {

        compositions.remove(compositionAccess.getId());
        compositions.put(compositionAccess.getId(), compositionAccess);
        log.info("Updated composition with id:" + compositionAccess.getId());
        contributionRecord.changed(true);
        update(Timestamp.valueOf(LocalDateTime.now()));
    }

    @Override
    public Boolean update(Timestamp transactionTime) {
        return update(transactionTime, false);
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        boolean updated = false;

//        if (contributionRecord.getState() == ContributionState.incomplete){
//            log.warn("Contribution state has not been set");
//        }

        if (force || contributionRecord.changed()) {

            if (!contributionRecord.changed()) {
                //hack: force tell jOOQ to perform updateComposition whatever...
                contributionRecord.changed(true);
                //jOOQ limited support of TSTZRANGE, exclude sys_period from updateComposition!
                contributionRecord.changed(CONTRIBUTION.SYS_PERIOD, false); //managed by an external trigger anyway...
            }
            contributionRecord.setSysTransaction(transactionTime);

            // update contribution's audit with modification change type and execute update of it, too
            this.auditDetails.setChangeType(I_ConceptAccess.fetchContributionChangeType(this, I_ConceptAccess.ContributionChangeType.MODIFICATION));
            this.auditDetails.update(transactionTime, force);

            // execute update of contribution itself
            updated = contributionRecord.store() == 1;
        }

        //commit or updateComposition the compositions
        //TODO: ---- not complete !!!
        //get the list of composition uuids *referencing* the current contribution
//        List<UUID> allUuids = context.select(COMPOSITION.ID).from(COMPOSITION).where(COMPOSITION.IN_CONTRIBUTION.eq(contributionRecord.getId())).fetch(COMPOSITION.ID);
//        updateChangedCompositions(CollectionUtils.intersection(allUuids, compositions.keySet()), transactionTime, force);
//        commitAddedCompositions(CollectionUtils.subtract(compositions.keySet(), allUuids), transactionTime);
//        deleteRemovedCompositions(CollectionUtils.subtract(allUuids, compositions.keySet()));

        return updated;
    }

    @Override
    public Boolean update() {
        return update(Timestamp.valueOf(LocalDateTime.now()));
    }

    @Override
    public Boolean update(Boolean force) {
        return update(Timestamp.valueOf(LocalDateTime.now()));
    }

    @Override
    public Integer delete() {
        int count = 0;
        //delete contribution record
        count += contributionRecord.delete();

        return count;
    }

    private void deleteRemovedCompositions(Collection<UUID> removed) {
        if (removed.isEmpty())
            return;

        for (UUID uuid : removed) {
            getContext().delete(COMPOSITION).where(COMPOSITION.ID.eq(uuid));
            log.debug("Deleted composition:" + uuid);
        }
    }

    private void commitAddedCompositions(Collection<UUID> added, Timestamp transactionTime) {
        if (added.isEmpty())
            return;

        for (UUID uuid : added) {
            compositions.get(uuid).commit(transactionTime);
            log.debug("Committed composition:" + uuid);
        }
    }

    private void updateChangedCompositions(Collection<UUID> updated, Timestamp transactionTime, boolean force) {
        if (updated.isEmpty())
            return;

        for (UUID uuid : updated) {
            compositions.get(uuid).update(transactionTime, force);
            log.debug("Updated composition:" + uuid);
        }
    }

    /**
     * @throws InternalServerException on failed fetching of contribution
     */
    public I_ContributionAccess retrieve(UUID id) {
        return retrieveInstance(this, id);
    }


    @Override
    public UUID getContributionId() {
        return contributionRecord.getId();
    }

    @Override
    public void setAuditDetailsChangeType(UUID changeType) {
        auditDetails.setChangeType(changeType);
    }

    /*@Override
    public void setChangeType(I_ConceptAccess.ContributionChangeType changeType) {
        auditDetails.setChangeType(ContributionChangeType.valueOf(changeType.name()));
    }*/

    @Override
    public ContributionDataType getContributionDataType() {
        return contributionRecord.getContributionType();
    }

    @Override
    public void setContributionDataType(ContributionDataType contributionDataType) {
        contributionRecord.setContributionType(contributionDataType);
    }

    @Override
    public void setState(ContributionDef.ContributionState state) {
        contributionRecord.setState(ContributionState.valueOf(state.getLiteral()));
    }

    @Override
    public void setComplete() {
        contributionRecord.setState(ContributionState.valueOf(ContributionState.complete.getLiteral()));
    }

    @Override
    public void setIncomplete() {
        contributionRecord.setState(ContributionState.valueOf(ContributionState.incomplete.getLiteral()));
    }

    @Override
    public void setDeleted() {
        contributionRecord.setState(ContributionState.valueOf(ContributionState.deleted.getLiteral()));
    }

/*    @Override
    public UUID getChangeTypeId() {
        ContributionChangeType contributionChangeType = contributionRecord.getChangeType();
        I_ConceptAccess.ContributionChangeType contributionChangeType1 = I_ConceptAccess.ContributionChangeType.valueOf(contributionChangeType.getLiteral());
        return I_ConceptAccess.fetchContributionChangeType(this, contributionChangeType1);
    }

    @Override
    public String getChangeTypeLitteral() {
        ContributionChangeType contributionChangeType = contributionRecord.getChangeType();
        return contributionChangeType.getLiteral();
    }

    @Override
    public UUID getCommitter() {
        return contributionRecord.getCommitter();
    }

    @Override
    public void setCommitter(UUID committer) {
        contributionRecord.setCommitter(committer);
    }

    @Override
    public String getDescription() {
        return contributionRecord.getDescription();
    }

    @Override
    public void setDescription(String description) {
        contributionRecord.setDescription(description);
    }

    @Override
    public Timestamp getTimeCommitted() {
        return contributionRecord.getTimeCommitted();
    }

    @Override
    public void setTimeCommitted(Timestamp timeCommitted) {
        contributionRecord.setTimeCommitted(timeCommitted);
    }

    @Override
    public UUID getSystemId() {
        return contributionRecord.getSystemId();
    }

    @Override
    public void setSystemId(UUID systemId) {
        contributionRecord.setSystemId(systemId);
    }*/

    public void setAuditDetailsValues(UUID committer, UUID system, String description) {
        if (committer == null || system == null || description == null)
            throw new IllegalArgumentException("arguments not optional");
        auditDetails.setCommitter(committer);
        auditDetails.setSystemId(system);
        auditDetails.setDescription(description);
    }

    @Override
    public ContributionDef.ContributionType getContributionType() {
        return ContributionDef.ContributionType.valueOf(contributionRecord.getContributionType().getLiteral());
    }

    @Override
    public ContributionDef.ContributionState getContributionState() {
        return ContributionDef.ContributionState.valueOf(contributionRecord.getState().getLiteral());
    }

    @Override
    public UUID getEhrId() {
        return contributionRecord.getEhrId();
    }

    @Override
    public Set<UUID> getCompositionIds() {
        return compositions.keySet();
    }

    @Override
    public I_CompositionAccess getComposition(UUID id) {
        return compositions.get(id);
    }

    @Override
    public String getDataType() {
        return contributionRecord.getContributionType().getLiteral();
    }

    @Override
    public void setDataType(ContributionDataType contributionDataType) {
        contributionRecord.setContributionType(contributionDataType);
    }

    @Override
    public UUID getId() {
        return contributionRecord.getId();
    }

    @Override
    public void setEhrId(UUID ehrId) {
        contributionRecord.setEhrId(ehrId);
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    @Override
    public void setHasAuditDetails(UUID auditId) {
        contributionRecord.setHasAudit(auditId);
    }

    @Override
    public UUID getHasAuditDetails() {
        return contributionRecord.getHasAudit();
    }
}
