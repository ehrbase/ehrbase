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
package org.ehrbase.dao.access.jooq;

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;
import static org.ehrbase.jooq.pg.Tables.STATUS;
import static org.ehrbase.jooq.pg.Tables.STATUS_HISTORY;
import static org.jooq.impl.DSL.count;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.interfaces.I_AuditDetailsAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_StatusAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.jooq.pg.tables.records.StatusHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;
import org.ehrbase.service.RecordedDvCodedText;
import org.jooq.AggregateFunction;
import org.jooq.DSLContext;
import org.jooq.Param;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.DSL;

/**
 * Persistence operations on EHR status.
 *
 * @author Christian Chevalley
 * @author Jake Smolka
 * @author Luis Marco-Ruiz
 * @since 1.0.0
 */
public class StatusAccess extends DataAccess implements I_StatusAccess {

    private StatusRecord statusRecord;
    private I_ContributionAccess contributionAccess; // locally referenced contribution associated to this status
    private I_AuditDetailsAccess auditDetailsAccess; // audit associated with this status

    public StatusAccess(I_DomainAccess domainAccess, UUID ehrId, Short sysTenant) {
        super(domainAccess);

        statusRecord = getContext().newRecord(STATUS);
        statusRecord.setSysTenant(sysTenant);

        // associate a contribution with this composition
        contributionAccess = I_ContributionAccess.getInstance(this, ehrId, sysTenant);
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);

        // associate status' own audit with this status access instance
        auditDetailsAccess = I_AuditDetailsAccess.getInstance(getDataAccess(), sysTenant);
    }

    @Override
    public UUID commit(LocalDateTime timestamp, UUID committerId, UUID systemId, String description) {
        createAndSetContribution(committerId, systemId, description, ContributionChangeType.CREATION);
        return internalCommit(timestamp);
    }

    @Override
    public UUID commit(LocalDateTime timestamp, UUID contribution, UUID audit) {
        if (contribution == null) {
            throw new InternalServerException("Invalid null valued contribution.");
        }
        setContributionId(contribution);

        if (audit != null) {
            statusRecord.setHasAudit(audit);
        }

        return internalCommit(timestamp);
    }

    private UUID internalCommit(LocalDateTime transactionTime) {

        // generate if not externally set
        if (statusRecord.getHasAudit() == null) {
            auditDetailsAccess.setChangeType(
                    I_ConceptAccess.fetchContributionChangeType(this, I_ConceptAccess.ContributionChangeType.CREATION));
            if (auditDetailsAccess.getChangeType() == null
                    || auditDetailsAccess.getSystemId() == null
                    || auditDetailsAccess.getCommitter() == null) {
                throw new InternalServerException(
                        "Illegal to commit AuditDetailsAccess without setting mandatory fields.");
            }
            UUID auditId = auditDetailsAccess.commit();
            statusRecord.setHasAudit(auditId);
        }

        statusRecord.setSysTransaction(Timestamp.valueOf(transactionTime));

        if (statusRecord.store() == 0) {
            throw new InvalidApiParameterException("Input EHR couldn't be stored; Storing EHR_STATUS failed");
        }

        return statusRecord.getId();
    }

    @Override
    public boolean update(
            LocalDateTime timestamp,
            UUID committerId,
            UUID systemId,
            String description,
            ContributionChangeType changeType) {

        createAndSetContribution(committerId, systemId, description, ContributionChangeType.MODIFICATION);

        // new audit ID
        auditDetailsAccess.commit();
        statusRecord.setHasAudit(auditDetailsAccess.getId());

        return internalUpdate(timestamp);
    }

    @Override
    public boolean update(LocalDateTime timestamp, UUID contribution, UUID audit) {
        if (contribution == null) {
            throw new InternalServerException("Invalid null valued contribution.");
        }
        setContributionId(contribution);
        statusRecord.setHasAudit(audit);

        return internalUpdate(timestamp);
    }

    private Boolean internalUpdate(LocalDateTime transactionTime) {

        statusRecord.setSysTransaction(Timestamp.valueOf(transactionTime));

        try {
            return statusRecord.update() > 0;
        } catch (RuntimeException e) {
            throw new InvalidApiParameterException(
                    "Couldn't marshall given EHR_STATUS / OTHER_DETAILS, content probably breaks RM rules");
        }
    }

    @Override
    public int delete(LocalDateTime timestamp, UUID committerId, UUID systemId, String description) {

        createAndSetContribution(committerId, systemId, description, ContributionChangeType.DELETED);

        return internalDelete(timestamp, committerId, systemId, description);
    }

    @Override
    public int delete(LocalDateTime timestamp, UUID contribution, UUID audit) {
        if (contribution == null) {
            throw new InternalServerException("Invalid null valued contribution.");
        }
        setContributionId(contribution);

        // Retrieve audit metadata from given contribution
        var newContributionAccess = I_ContributionAccess.retrieveInstance(this.getDataAccess(), contribution);
        UUID systemId = newContributionAccess.getAuditsSystemId();
        UUID committerId = newContributionAccess.getAuditsCommitter();
        String description = newContributionAccess.getAuditsDescription();

        return internalDelete(timestamp, committerId, systemId, description);
    }

    private Integer internalDelete(LocalDateTime timestamp, UUID committerId, UUID systemId, String description) {
        Short sysTenant = statusRecord.getSysTenant();
        statusRecord.setSysTransaction(Timestamp.valueOf(timestamp));
        statusRecord.delete();

        // create new deletion audit
        var delAudit = I_AuditDetailsAccess.getInstance(
                this, systemId, committerId, I_ConceptAccess.ContributionChangeType.DELETED, description, sysTenant);
        UUID delAuditId = delAudit.commit();

        // create new, BUT already moved to _history, version documenting the deletion
        return createAndCommitNewDeletedVersionAsHistory(delAuditId, statusRecord.getInContribution(), sysTenant);
    }

    private int createAndCommitNewDeletedVersionAsHistory(UUID delAuditId, UUID contrib, Short sysTenant) {
        // a bit hacky: create new, BUT already moved to _history, version documenting the deletion
        // (Normal approach of first .update() then .delete() won't work, because postgres' transaction optimizer will
        // just skip the update if it will get deleted anyway.)
        // so copy values, but add deletion meta data
        StatusHistoryRecord newRecord = getDataAccess().getContext().newRecord(STATUS_HISTORY);
        newRecord.setId(statusRecord.getId());
        newRecord.setEhrId(statusRecord.getEhrId());
        newRecord.setInContribution(contrib);
        newRecord.setArchetypeNodeId(statusRecord.getArchetypeNodeId());
        newRecord.setAttestationRef(statusRecord.getAttestationRef());
        newRecord.setName(statusRecord.getName());
        newRecord.setSysTenant(sysTenant);
        newRecord.setIsModifiable(statusRecord.getIsModifiable());
        newRecord.setIsQueryable(statusRecord.getIsQueryable());
        newRecord.setOtherDetails(statusRecord.getOtherDetails());
        newRecord.setParty(statusRecord.getParty());
        newRecord.setHasAudit(delAuditId);

        getDataAccess().getContext().attach(newRecord);
        if (newRecord.insert() != 1) {
            throw new InternalServerException("DB inconsistency");
        } else {
            return 1;
        }
    }

    private void createAndSetContribution(
            UUID committerId, UUID systemId, String description, ContributionChangeType changeType) {
        contributionAccess.setAuditDetailsChangeType(I_ConceptAccess.fetchContributionChangeType(this, changeType));
        if (contributionAccess.getAuditsCommitter() == null || contributionAccess.getAuditsSystemId() == null) {
            if (committerId == null || systemId == null) {
                throw new InternalServerException(
                        "Illegal to commit the contribution's AuditDetailsAccess without setting mandatory fields.");
            } else {
                contributionAccess.setAuditDetailsCommitter(committerId);
                contributionAccess.setAuditDetailsSystemId(systemId);
                contributionAccess.setAuditDetailsDescription(description);
            }
        }
        UUID contributionId = this.contributionAccess.commit();
        setContributionId(contributionId);
    }

    public static I_StatusAccess retrieveInstance(I_DomainAccess domainAccess, UUID statusId) {
        StatusRecord record = domainAccess.getContext().fetchOne(STATUS, STATUS.ID.eq(statusId));

        if (record == null) {
            return null;
        }

        return createStatusAccessForRetrieval(domainAccess, record, null, record.getSysTenant());
    }

    public static I_StatusAccess retrieveInstanceByNamedSubject(I_DomainAccess domainAccess, String partyName) {

        DSLContext context = domainAccess.getContext();

        StatusRecord record = domainAccess
                .getContext()
                .fetchOne(
                        STATUS,
                        STATUS.PARTY.eq(context.select(PARTY_IDENTIFIED.ID)
                                .from(PARTY_IDENTIFIED)
                                .where(PARTY_IDENTIFIED.NAME.eq(partyName))));

        if (record == null) {
            return null;
        }

        return createStatusAccessForRetrieval(domainAccess, record, null, record.getSysTenant());
    }

    public static I_StatusAccess retrieveInstanceByParty(I_DomainAccess domainAccess, UUID partyIdentified) {

        DSLContext context = domainAccess.getContext();

        StatusRecord record = domainAccess
                .getContext()
                .fetchOne(
                        STATUS,
                        STATUS.PARTY.eq(context.select(PARTY_IDENTIFIED.ID)
                                .from(PARTY_IDENTIFIED)
                                .where(PARTY_IDENTIFIED.ID.eq(partyIdentified))));

        if (record == null) {
            return null;
        }

        return createStatusAccessForRetrieval(domainAccess, record, null, record.getSysTenant());
    }

    public static I_StatusAccess retrieveByVersion(I_DomainAccess domainAccess, UUID statusId, int version) {
        if (version == getLatestVersionNumber(domainAccess, statusId)) return retrieveInstance(domainAccess, statusId);

        Map<Integer, I_StatusAccess> allVersions = getVersionMapOfStatus(domainAccess, statusId);

        return allVersions.get(Integer.valueOf(version));
    }

    // fetch latest status
    public static I_StatusAccess retrieveInstanceByEhrId(I_DomainAccess domainAccess, UUID ehrId) {
        StatusRecord record = null;

        if (domainAccess.getContext().fetchExists(STATUS, STATUS.EHR_ID.eq(ehrId))) {
            record = domainAccess.getContext().fetchOne(STATUS, STATUS.EHR_ID.eq(ehrId));
        } else {
            if (!domainAccess.getContext().fetchExists(STATUS_HISTORY, STATUS_HISTORY.EHR_ID.eq(ehrId)))
            // no current one (premise from above) and no history --> inconsistency
            {
                throw new InternalServerException("DB inconsistency. No STATUS for given EHR ID: " + ehrId);
            } else {
                Result<StatusHistoryRecord> recordsRes = domainAccess
                        .getContext()
                        .selectFrom(STATUS_HISTORY)
                        .where(STATUS_HISTORY.EHR_ID.eq(ehrId))
                        .orderBy(STATUS_HISTORY.SYS_TRANSACTION.desc()) // latest at top, i.e. [0]
                        .fetch();
                // get latest
                if (recordsRes.get(0) != null) {
                    record = historyRecToNormalRec(domainAccess, recordsRes.get(0));
                }
            }
        }

        if (record == null) {
            return null;
        }

        return createStatusAccessForRetrieval(domainAccess, record, null, record.getSysTenant());
    }

    public static Map<ObjectVersionId, I_StatusAccess> retrieveInstanceByContribution(
            I_DomainAccess domainAccess, UUID contributionId, String node) {
        Set<UUID> statuses = new HashSet<>(); // Set, because of unique values
        // add all compositions having a link to given contribution
        domainAccess
                .getContext()
                .select(STATUS.ID)
                .from(STATUS)
                .where(STATUS.IN_CONTRIBUTION.eq(contributionId))
                .fetch()
                .forEach(rec -> statuses.add(rec.value1()));
        // and older versions or deleted ones, too
        domainAccess
                .getContext()
                .select(STATUS_HISTORY.ID)
                .from(STATUS_HISTORY)
                .where(STATUS_HISTORY.IN_CONTRIBUTION.eq(contributionId))
                .fetch()
                .forEach(rec -> statuses.add(rec.value1()));

        // get whole "version map" of each matching status and do fine-grain check for matching contribution
        // precondition: each UUID in `statuses` set is unique, so for each the "version map" is only created once below
        // (meta: can't do that as jooq query, because the specific version number isn't stored in DB)
        Map<ObjectVersionId, I_StatusAccess> resultMap = new HashMap<>();
        for (UUID statusId : statuses) {
            Map<Integer, I_StatusAccess> map = getVersionMapOfStatus(domainAccess, statusId);
            // fine-grained contribution ID check
            map.forEach((k, v) -> {
                if (v.getContributionId().equals(contributionId)) {
                    resultMap.put(new ObjectVersionId(statusId.toString(), node, k.toString()), v);
                }
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
            I_StatusAccess statusAccess =
                    createStatusAccessForRetrieval(domainAccess, record, null, record.getSysTenant());
            versionMap.put(versionCounter, statusAccess);

            versionCounter--;
        }

        // if composition was removed (i.e. from "COMPOSITION" table) *or* other versions are existing
        Result<StatusHistoryRecord> historyRecords = domainAccess
                .getContext()
                .selectFrom(STATUS_HISTORY)
                .where(STATUS_HISTORY.ID.eq(statusId))
                .orderBy(STATUS_HISTORY.SYS_TRANSACTION.desc())
                .fetch();

        for (StatusHistoryRecord historyRecord : historyRecords) {
            I_StatusAccess historyAccess =
                    createStatusAccessForRetrieval(domainAccess, null, historyRecord, historyRecord.getSysTenant());
            versionMap.put(versionCounter, historyAccess);
            versionCounter--;
        }

        if (versionCounter != 0) {
            throw new InternalServerException("Version Map generation failed");
        }

        return versionMap;
    }

    /**
     * Helper to create a new {@link StatusAccess} instance from a queried statusRecord (either as {@link
     * StatusRecord} or {@link StatusHistoryRecord}), to return to service layer.
     *
     * @param domainAccess  General access
     * @param statusRecord        Queried {@link StatusRecord} which contains ID of linked EHR, audit and
     *                      contribution. Give null if not used
     * @param historyRecord Option: Same as statusRecord but as history input type. Give null if not used
     * @return Resulting access object
     */
    private static I_StatusAccess createStatusAccessForRetrieval(
            I_DomainAccess domainAccess,
            StatusRecord statusRecord,
            StatusHistoryRecord historyRecord,
            Short sysTenant) {
        StatusAccess statusAccess;
        if (statusRecord != null) {
            statusAccess = new StatusAccess(domainAccess, statusRecord.getEhrId(), sysTenant);
            statusAccess.setStatusRecord(statusRecord);
        } else if (historyRecord != null) {
            statusAccess = new StatusAccess(domainAccess, historyRecord.getEhrId(), sysTenant);
            statusAccess.setStatusRecord(historyRecord);
        } else {
            throw new InternalServerException("Error creating version map of EHR_STATUS");
        }

        // retrieve corresponding audit
        I_AuditDetailsAccess auditAccess = new AuditDetailsAccess(domainAccess.getDataAccess(), sysTenant)
                .retrieveInstance(domainAccess.getDataAccess(), statusAccess.getAuditDetailsId());
        statusAccess.setAuditDetailsAccess(auditAccess);

        // retrieve corresponding contribution
        I_ContributionAccess retContributionAccess =
                I_ContributionAccess.retrieveInstance(domainAccess, statusAccess.getContributionId());
        statusAccess.setContributionAccess(retContributionAccess);

        return statusAccess;
    }

    @Override
    public UUID getId() {
        return statusRecord.getId();
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
        statusRecord = StatusAccess.historyRecToNormalRec(getDataAccess(), input);
        statusRecord.setSysPeriod(null);
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
    public void setAuditAndContributionAuditValues(
            UUID systemId, UUID committerId, String description, ContributionChangeType changeType) {
        if (committerId == null || systemId == null || changeType == null) {
            throw new IllegalArgumentException("arguments not optional");
        }
        this.auditDetailsAccess.setCommitter(committerId);
        this.auditDetailsAccess.setSystemId(systemId);
        this.auditDetailsAccess.setChangeType(I_ConceptAccess.fetchContributionChangeType(this, changeType));

        if (description != null) {
            this.auditDetailsAccess.setDescription(description);
        }

        this.contributionAccess.setAuditDetailsValues(committerId, systemId, description, changeType);
    }

    /**
     * Helper method to convert result from query on history table to a record of the normal table.
     *
     * @param statusHistoryRecord Given history record
     * @return Converted normal record
     */
    protected static StatusRecord historyRecToNormalRec(
            I_DomainAccess domainAccess, StatusHistoryRecord statusHistoryRecord) {
        StatusRecord statusRecord = domainAccess.getContext().newRecord(STATUS);
        statusRecord.setId(statusHistoryRecord.getId());
        statusRecord.setEhrId(statusHistoryRecord.getEhrId());
        statusRecord.setIsQueryable(statusHistoryRecord.getIsQueryable());
        statusRecord.setIsModifiable(statusHistoryRecord.getIsModifiable());
        statusRecord.setParty(statusHistoryRecord.getParty());
        statusRecord.setOtherDetails(statusHistoryRecord.getOtherDetails());
        statusRecord.setSysTransaction(statusHistoryRecord.getSysTransaction());
        statusRecord.setSysPeriod(statusHistoryRecord.getSysPeriod());
        statusRecord.setHasAudit(statusHistoryRecord.getHasAudit());
        statusRecord.setAttestationRef(statusHistoryRecord.getAttestationRef());
        statusRecord.setInContribution(statusHistoryRecord.getInContribution());
        statusRecord.setArchetypeNodeId(statusHistoryRecord.getArchetypeNodeId());
        statusRecord.setName(statusHistoryRecord.getName());
        statusRecord.setSysTenant(statusHistoryRecord.getSysTenant());
        return statusRecord;
    }

    public static Integer getLatestVersionNumber(I_DomainAccess domainAccess, UUID statusId) {

        DSLContext ctx = domainAccess.getContext();
        Param<UUID> uuidParam = DSL.param("id", statusId);
        Table<Record1<Integer>> unionAll = ctx.select(count(STATUS.ID))
                .from(STATUS)
                .where(STATUS.ID.eq(uuidParam))
                .unionAll(ctx.select(count(STATUS_HISTORY.ID))
                        .from(STATUS_HISTORY)
                        .where(STATUS_HISTORY.ID.eq(uuidParam)))
                .asTable("version_counts");

        AggregateFunction<BigDecimal> sum = DSL.sum(unionAll.field(0, Integer.class));

        int version = ctx.select(sum).from(unionAll).fetchOne(sum).intValue();

        return version;
    }

    public static boolean exists(I_DomainAccess domainAccess, UUID ehrStatusId) {
        return domainAccess.getContext().fetchExists(STATUS, STATUS.ID.eq(ehrStatusId));
    }

    @Override
    @SuppressWarnings("rawtypes") // `result` is raw so later iterating also gets the version number
    public int getEhrStatusVersionFromTimeStamp(Timestamp time) {
        UUID statusUid = this.statusRecord.getId();
        // retrieve current version from status tables
        I_StatusAccess retStatusAccess = I_StatusAccess.retrieveInstance(this.getDataAccess(), statusUid);

        // retrieve all other versions from status_history and sort by time
        Result result = getDataAccess()
                .getContext()
                .selectFrom(STATUS_HISTORY)
                .where(STATUS_HISTORY.ID.eq(statusUid))
                .orderBy(STATUS_HISTORY.SYS_TRANSACTION.desc()) // latest at top, i.e. [0]
                .fetch();

        // see 'what version was the top version at moment T?'
        // first: is time T after current version? then current version is result
        if (time.after(retStatusAccess.getStatusRecord().getSysTransaction())) {
            return getLatestVersionNumber(getDataAccess(), statusUid);
        }
        // second: if not, which one of the historical versions matches?
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i) instanceof StatusHistoryRecord) {
                // is time T after this version? then return its version number
                if (time.after(((StatusHistoryRecord) result.get(i)).getSysTransaction())) {
                    return result.size()
                            - i; // reverses iterator because order was reversed above and always get non zero
                }
            } else {
                throw new InternalServerException("Problem comparing timestamps of EHR_STATUS versions");
            }
        }

        throw new ObjectNotFoundException("EHR_STATUS", "Could not find EHR_STATUS version matching given timestamp");
    }

    @Override
    public Timestamp getInitialTimeOfVersionedEhrStatus() {
        Result<StatusHistoryRecord> result = getDataAccess()
                .getContext()
                .selectFrom(STATUS_HISTORY)
                .where(STATUS_HISTORY.EHR_ID.eq(statusRecord.getEhrId())) // ehrId from this instance
                .orderBy(STATUS_HISTORY.SYS_TRANSACTION.asc()) // oldest at top, i.e. [0]
                .fetch();

        if (!result.isEmpty()) {
            StatusHistoryRecord statusHistoryRecord = result.get(0); // get oldest
            return statusHistoryRecord.getSysTransaction();
        }

        // if haven't returned above use time from latest version (already available in this instance)
        return statusRecord.getSysTransaction();
    }

    @Override
    public EhrStatus getStatus() {
        EhrStatus status = new EhrStatus();

        status.setModifiable(getStatusRecord().getIsModifiable());
        status.setQueryable(getStatusRecord().getIsQueryable());
        // set otherDetails if available
        if (getStatusRecord().getOtherDetails() != null) {
            status.setOtherDetails(getStatusRecord().getOtherDetails());
        }

        // Locatable attribute
        status.setArchetypeNodeId(getStatusRecord().getArchetypeNodeId());
        Object name = new RecordedDvCodedText().fromDB(getStatusRecord(), STATUS.NAME);
        status.setName(name instanceof DvText ? (DvText) name : (DvCodedText) name);

        UUID statusId = getStatusRecord().getId();
        status.setUid(new HierObjectId(statusId.toString() + "::"
                + getServerConfig().getNodename() + "::" + I_StatusAccess.getLatestVersionNumber(this, statusId)));

        PartySelf partySelf = (PartySelf)
                new PersistedPartyProxy(this).retrieve(getStatusRecord().getParty());
        status.setSubject(partySelf);

        return status;
    }

    @Override
    public void setOtherDetails(ItemStructure otherDetails) {
        if (otherDetails != null) {
            statusRecord.setOtherDetails(otherDetails);
        }
    }

    @Override
    public ItemStructure getOtherDetails() {
        return this.statusRecord.getOtherDetails();
    }

    @Override
    public void setEhrId(UUID ehrId) {
        this.statusRecord.setEhrId(ehrId);
    }

    @Override
    public UUID getEhrId() {
        return this.statusRecord.getEhrId();
    }

    @Override
    public Timestamp getSysTransaction() {
        return this.statusRecord.getSysTransaction();
    }
}
