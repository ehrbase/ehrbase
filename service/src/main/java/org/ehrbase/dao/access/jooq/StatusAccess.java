/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School), Luis Marco-Ruiz (Hannover Medical School).

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

import com.nedap.archie.rm.datastructures.ItemStructure;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.interfaces.*;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.dao.access.util.TransactionTime;
import org.ehrbase.jooq.pg.tables.records.CompositionHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.CompositionRecord;
import org.ehrbase.jooq.pg.tables.records.StatusHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.Result;

import java.sql.Timestamp;
import java.util.*;

import static org.ehrbase.jooq.pg.Tables.*;

/**
 * Created by Christian Chevalley on 4/20/2015.
 */
public class StatusAccess extends DataAccess implements I_StatusAccess {

    private static final Logger log = LogManager.getLogger(StatusAccess.class);

    private StatusRecord statusRecord;
    private I_ContributionAccess contributionAccess; // locally referenced contribution associated to this status
    private I_AuditDetailsAccess auditDetailsAccess; // audit associated with this status

    public StatusAccess(I_DomainAccess domainAccess, UUID ehrId) {
        super(domainAccess);

        statusRecord = getContext().newRecord(STATUS);

        //associate a contribution with this composition
        contributionAccess = I_ContributionAccess.getInstance(this, ehrId);
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);

        // associate status' own audit with this status access instance
        auditDetailsAccess = I_AuditDetailsAccess.getInstance(getDataAccess());
    }

    public static I_StatusAccess retrieveInstance(I_DomainAccess domainAccess, UUID statusId) {
        StatusRecord record = domainAccess.getContext().fetchOne(STATUS, STATUS.ID.eq(statusId));

        if (record == null)
            return null;

        return createStatusAccessForRetrieval(domainAccess, record, null);
    }

    public static I_StatusAccess retrieveInstanceByNamedSubject(I_DomainAccess domainAccess, String partyName) {

        DSLContext context = domainAccess.getContext();

        StatusRecord record = domainAccess.getContext().fetchOne(STATUS,
                STATUS.PARTY.eq
                        (context.select(PARTY_IDENTIFIED.ID)
                                .from(PARTY_IDENTIFIED)
                                .where(PARTY_IDENTIFIED.NAME.eq(partyName))
                        )
        );

        if (record == null)
            return null;

        return createStatusAccessForRetrieval(domainAccess, record, null);
    }

    public static I_StatusAccess retrieveInstanceByParty(I_DomainAccess domainAccess, UUID partyIdentified) {

        DSLContext context = domainAccess.getContext();

        StatusRecord record = domainAccess.getContext().fetchOne(STATUS,
                STATUS.PARTY.eq
                        (context.select(PARTY_IDENTIFIED.ID)
                                .from(PARTY_IDENTIFIED)
                                .where(PARTY_IDENTIFIED.ID.eq(partyIdentified))
                        )
                );

        if (record == null)
            return null;

        return createStatusAccessForRetrieval(domainAccess, record, null);

    }

    // fetch latest status
    public static I_StatusAccess retrieveInstanceByEhrId(I_DomainAccess domainAccess, UUID ehrId) {
        StatusRecord record = null;

        if (domainAccess.getContext().fetchExists(STATUS, STATUS.EHR_ID.eq(ehrId)))
            record = domainAccess.getContext().fetchOne(STATUS, STATUS.EHR_ID.eq(ehrId));
        else {
            if (!domainAccess.getContext().fetchExists(STATUS_HISTORY, STATUS_HISTORY.EHR_ID.eq(ehrId)))
                // no current one (premise from above) and no history --> inconsistency
                throw new InternalServerException("DB inconsistency. No STATUS for given EHR ID: " + ehrId);
            else {
                Result<StatusHistoryRecord> recordsRes = domainAccess.getContext()
                        .selectFrom(STATUS_HISTORY)
                        .where(STATUS_HISTORY.EHR_ID.eq(ehrId))
                        .orderBy(STATUS_HISTORY.SYS_TRANSACTION.desc())    // latest at top, i.e. [0]
                        .fetch();
                // get latest
                if (recordsRes.get(0) != null) {
                    record = historyRecToNormalRec(recordsRes.get(0));
                }
            }
        }

        if (record == null)
            return null;

        return createStatusAccessForRetrieval(domainAccess, record, null);
    }

    public static Map<Integer, I_StatusAccess> retrieveInstanceByContribution(I_DomainAccess domainAccess, UUID contributionId) {
        Set<UUID> statuses = new HashSet<>();   // Set, because of unique values
        // add all compositions having a link to given contribution
        domainAccess.getContext().select(STATUS.ID).from(STATUS).where(STATUS.IN_CONTRIBUTION.eq(contributionId)).fetch()
                .forEach(rec -> statuses.add(rec.value1()));
        // and older versions or deleted ones, too
        domainAccess.getContext().select(STATUS_HISTORY.ID).from(STATUS_HISTORY).where(STATUS_HISTORY.IN_CONTRIBUTION.eq(contributionId)).fetch()
                .forEach(rec -> statuses.add(rec.value1()));

        // get whole "version map" of each matching status and do fine-grain check for matching contribution
        // precondition: each UUID in `statuses` set is unique, so for each the "version map" is only created once below
        // (meta: can't do that as jooq query, because the specific version number isn't stored in DB)
        Map<Integer, I_StatusAccess> resultMap = new HashMap<>();
        for (UUID statusId : statuses) {
            Map<Integer, I_StatusAccess> map = getVersionMapOfStatus(domainAccess, statusId);
            // fine-grained contribution ID check
            map.forEach((k, v) -> {
                if (v.getContributionId().equals(contributionId))
                    resultMap.put(k, v);
            });
        }

        return resultMap;
    }

    public static Map<Integer, I_StatusAccess> getVersionMapOfStatus(I_DomainAccess domainAccess, UUID statusId) {
        Map<Integer, I_StatusAccess> versionMap = new HashMap<>();

        // create counter with highest version, to keep track of version number and allow check in the end
        Integer versionCounter = getLatestVersionNumber(domainAccess, statusId);

        // fetch matching entry
        StatusRecord record = domainAccess.getContext().fetchOne(STATUS, STATUS.ID.eq(statusId));
        if (record != null) {
            I_StatusAccess statusAccess = createStatusAccessForRetrieval(domainAccess, record, null);
            versionMap.put(versionCounter, statusAccess);

            versionCounter--;
        }

        // if composition was removed (i.e. from "COMPOSITION" table) *or* other versions are existing
        Result<StatusHistoryRecord> historyRecords = domainAccess.getContext()
                .selectFrom(STATUS_HISTORY)
                .where(STATUS_HISTORY.ID.eq(statusId))
                .orderBy(STATUS_HISTORY.SYS_TRANSACTION.desc())
                .fetch();

        for (StatusHistoryRecord historyRecord : historyRecords) {
            I_StatusAccess historyAccess = createStatusAccessForRetrieval(domainAccess, null, historyRecord);
            versionMap.put(versionCounter, historyAccess);
            versionCounter--;
        }

        if (versionCounter != 0)
            throw new InternalServerException("Version Map generation failed");

        return versionMap;
    }

    /**
     * Helper to create a new {@link StatusAccess} instance from a queried record (either as {@link StatusRecord} or
     * {@link StatusHistoryRecord}), to return to service layer.
     * @param domainAccess General access
     * @param record Queried {@link StatusRecord} which contains ID of linked EHR, audit and contribution. Give null if not used
     * @param historyRecord Option: Same as record but as history input type. Give null if not used
     * @return Resulting access object
     */
    private static I_StatusAccess createStatusAccessForRetrieval(I_DomainAccess domainAccess, StatusRecord record, StatusHistoryRecord historyRecord) {
        StatusAccess statusAccess;
        if (record != null) {
            statusAccess = new StatusAccess(domainAccess, record.getEhrId());
            statusAccess.setStatusRecord(record);
        } else if (historyRecord != null) {
            statusAccess = new StatusAccess(domainAccess, historyRecord.getEhrId());
            statusAccess.setStatusRecord(historyRecord);
        } else
            throw new InternalServerException("Error creating version map of EHR_STATUS");

        // retrieve corresponding audit
        I_AuditDetailsAccess auditAccess = new AuditDetailsAccess(domainAccess.getDataAccess()).retrieveInstance(domainAccess.getDataAccess(), statusAccess.getAuditDetailsId());
        statusAccess.setAuditDetailsAccess(auditAccess);

        // retrieve corresponding contribution
        I_ContributionAccess retContributionAccess = I_ContributionAccess.retrieveInstance(domainAccess, statusAccess.getContributionId());
        statusAccess.setContributionAccess(retContributionAccess);

        return statusAccess;
    }

    @Override
    public UUID getId() {
        return statusRecord.getId();
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public UUID commit(Timestamp transactionTime) {
        throw new InternalServerException("INTERNAL: commit is not valid");
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public UUID commit()  {
        throw new InternalServerException("INTERNAL: commit without transaction time is not legal");
    }

    @Override
    public UUID commit(Timestamp transactionTime, UUID ehrId, ItemStructure otherDetails) {
        contributionAccess.setAuditDetailsChangeType(I_ConceptAccess.fetchContributionChangeType(this, I_ConceptAccess.ContributionChangeType.CREATION));
        if (contributionAccess.getAuditsCommitter() == null || contributionAccess.getAuditsSystemId() == null)
            throw new InternalServerException("Illegal to commit the contribution's AuditDetailsAccess without setting mandatory fields.");
        UUID contributionId = this.contributionAccess.commit();
        setContributionId(contributionId);

        return internalCommit(transactionTime, ehrId, otherDetails);
    }

    @Override
    public UUID commitWithCustomContribution(Timestamp transactionTime, UUID ehrId, ItemStructure otherDetails) {
        return internalCommit(transactionTime, ehrId, otherDetails);
    }

    private UUID internalCommit(Timestamp transactionTime, UUID ehrId, ItemStructure otherDetails) {
        auditDetailsAccess.setChangeType(I_ConceptAccess.fetchContributionChangeType(this, I_ConceptAccess.ContributionChangeType.CREATION));
        if (auditDetailsAccess.getChangeType() == null || auditDetailsAccess.getSystemId() == null || auditDetailsAccess.getCommitter() == null)
            throw new InternalServerException("Illegal to commit AuditDetailsAccess without setting mandatory fields.");
        UUID auditId = auditDetailsAccess.commit();
        statusRecord.setHasAudit(auditId);

        statusRecord.setEhrId(ehrId);
        if (otherDetails != null) {
            statusRecord.setOtherDetails(otherDetails);
        }
        statusRecord.setSysTransaction(transactionTime);

        statusRecord.setHasAudit(auditId);

        if (statusRecord.store() == 0) {
            throw new InvalidApiParameterException("Input EHR couldn't be stored; Storing EHR_STATUS failed");
        }

        return statusRecord.getId();
    }

    @Override
    public Boolean update(Timestamp transactionTime) {
        return update(null, transactionTime, false);
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        return update(null, transactionTime, force);
    }

    @Override
    public Boolean update(Boolean force) {
        return update(null, TransactionTime.millis(), force);
    }

    @Override   // root update()
    public Boolean update(ItemStructure otherDetails, Timestamp transactionTime, boolean force) {
        if (force || statusRecord.changed()) {
            // update both contribution (incl its audit) and the status' own audit
            contributionAccess.update(transactionTime, null, null, null, null, I_ConceptAccess.ContributionChangeType.MODIFICATION, null);
            statusRecord.setInContribution(contributionAccess.getId()); // new contribution ID
            auditDetailsAccess.update(null, null, I_ConceptAccess.ContributionChangeType.MODIFICATION, null);
            statusRecord.setHasAudit(auditDetailsAccess.getId()); // new audit ID

            if (otherDetails != null) {
                statusRecord.setOtherDetails(otherDetails);
            }
            statusRecord.setSysTransaction(transactionTime);

            try {
                return statusRecord.update() > 0;
            } catch (RuntimeException e) {
                throw new InvalidApiParameterException("Couldn't marshall given EHR_STATUS / OTHER_DETAILS, content probably breaks RM rules");
            }
        }
        return false;   // if updated technically worked but jooq reports no update was necessary
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public Boolean update() {
        throw new InternalServerException("INTERNAL: this update signature is not valid");
    }

    @Override
    public Integer delete() {
        return statusRecord.delete();
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    @Override
    public void setStatusRecord(StatusRecord record) {
        this.statusRecord = record;
    }

    @Override
    public void setStatusRecord(StatusHistoryRecord input) {
        this.statusRecord = new StatusRecord(
                input.getId(),
                input.getEhrId(),
                input.getIsQueryable(),
                input.getIsModifiable(),
                input.getParty(),
                input.getOtherDetails(),
                null,
                null,
                input.getHasAudit(),
                input.getAttestationRef(),
                input.getInContribution(),
                input.getArchetypeNodeId(),
                input.getName()
        );
    }

    @Override
    public StatusRecord getStatusRecord() {
        return this.statusRecord;
    }

    @Override
    public void setAuditDetailsAccess(I_AuditDetailsAccess auditDetailsAccess) {
        this.auditDetailsAccess = auditDetailsAccess;
    }

    @Override
    public void setContributionAccess(I_ContributionAccess contributionAccess) {
        this.contributionAccess = contributionAccess;
    }

    @Override
    public I_AuditDetailsAccess getAuditDetailsAccess() {
        return this.auditDetailsAccess;
    }

    @Override
    public UUID getAuditDetailsId() {
        return statusRecord.getHasAudit();
    }

    @Override
    public void setContributionId(UUID contribution) {
        this.statusRecord.setInContribution(contribution);
    }

    @Override
    public UUID getContributionId() {
        return this.statusRecord.getInContribution();
    }

    @Override
    public void setAuditAndContributionAuditValues(UUID systemId, UUID committerId, String description) {
        if (systemId != null)
            this.auditDetailsAccess.setSystemId(systemId);

        if (committerId != null)
            this.auditDetailsAccess.setCommitter(committerId);

        if (description != null)
            this.auditDetailsAccess.setDescription(description);

        this.contributionAccess.setAuditDetailsValues(committerId, systemId, description);
    }

    /**
     * Helper method to convert result from query on history table to a record of the normal table.
     * @param statusHistoryRecord Given history record
     * @return Converted normal record
     */
    protected static StatusRecord historyRecToNormalRec(StatusHistoryRecord statusHistoryRecord) {
        return new StatusRecord(
                statusHistoryRecord.getId(),
                statusHistoryRecord.getEhrId(),
                statusHistoryRecord.getIsQueryable(),
                statusHistoryRecord.getIsModifiable(),
                statusHistoryRecord.getParty(),
                statusHistoryRecord.getOtherDetails(),
                statusHistoryRecord.getSysTransaction(),
                statusHistoryRecord.getSysPeriod(),
                statusHistoryRecord.getHasAudit(),
                statusHistoryRecord.getAttestationRef(),
                statusHistoryRecord.getInContribution(),
                statusHistoryRecord.getArchetypeNodeId(),
                statusHistoryRecord.getName()
        );
    }

    public static Integer getLatestVersionNumber(I_DomainAccess domainAccess, UUID statusId) {

        if (!hasPreviousVersionOfStatus(domainAccess, statusId))
            return 1;

        int versionCount = domainAccess.getContext().fetchCount(STATUS_HISTORY, STATUS_HISTORY.ID.eq(statusId));

        return versionCount + 1;
    }

    private static boolean hasPreviousVersionOfStatus(I_DomainAccess domainAccess, UUID ehrStatusId) {
        return domainAccess.getContext().fetchExists(STATUS_HISTORY, STATUS_HISTORY.ID.eq(ehrStatusId));
    }

    @Override
    @SuppressWarnings("rawtypes")   // `result` is raw so later iterating also gets the version number
    public int getEhrStatusVersionFromTimeStamp(Timestamp time) {
        UUID statusUid = this.statusRecord.getId();
        // retrieve current version from status tables
        I_StatusAccess retStatusAccess = I_StatusAccess.retrieveInstance(this.getDataAccess(), statusUid);

        // retrieve all other versions from status_history and sort by time
        Result result = getDataAccess().getContext().selectFrom(STATUS_HISTORY)
                .where(STATUS_HISTORY.ID.eq(statusUid))
                .orderBy(STATUS_HISTORY.SYS_TRANSACTION.desc())    // latest at top, i.e. [0]
                .fetch();

        // see 'what version was the top version at moment T?'
        // first: is time T after current version? then current version is result
        if (time.after(retStatusAccess.getStatusRecord().getSysTransaction()))
            return getLatestVersionNumber(getDataAccess(), statusUid);
        // second: if not, which one of the historical versions matches?
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i) instanceof StatusHistoryRecord) {
                // is time T after this version? then return its version number
                if (time.after(((StatusHistoryRecord) result.get(i)).getSysTransaction()))
                    return result.size() - i;   // reverses iterator because order was reversed above and always get non zero
            } else {
                throw new InternalServerException("Problem comparing timestamps of EHR_STATUS versions");
            }
        }

        throw new ObjectNotFoundException("EHR_STATUS", "Could not find EHR_STATUS version matching given timestamp");
    }

    @Override
    public Timestamp getInitialTimeOfVersionedEhrStatus() {
        Result<StatusHistoryRecord> result = getDataAccess().getContext().selectFrom(STATUS_HISTORY)
                .where(STATUS_HISTORY.EHR_ID.eq(statusRecord.getEhrId())) // ehrId from this instance
                .orderBy(STATUS_HISTORY.SYS_TRANSACTION.asc())  // oldest at top, i.e. [0]
                .fetch();

        if (!result.isEmpty()) {
            StatusHistoryRecord statusHistoryRecord = result.get(0); // get oldest
            return statusHistoryRecord.getSysTransaction();
        }

        // if haven't returned above use time from latest version (already available in this instance)
        return statusRecord.getSysTransaction();
    }
}
