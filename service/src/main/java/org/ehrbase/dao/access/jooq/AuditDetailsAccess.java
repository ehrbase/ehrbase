/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.dao.access.jooq;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartyProxy;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.*;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.tables.records.AuditDetailsRecord;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.ehrbase.jooq.pg.tables.AuditDetails.AUDIT_DETAILS;

public class AuditDetailsAccess extends DataAccess implements I_AuditDetailsAccess {

    private AuditDetailsRecord auditDetailsRecord;

    public AuditDetailsAccess(I_DomainAccess dataAccess) {
        super(dataAccess.getContext(), null, null, dataAccess.getServerConfig());
        this.auditDetailsRecord = dataAccess.getContext().newRecord(AUDIT_DETAILS);
    }

    public AuditDetailsAccess(I_DomainAccess dataAccess, UUID systemId, UUID committer, I_ConceptAccess.ContributionChangeType changeType, String description) {
        super(dataAccess.getContext(), null, null, dataAccess.getServerConfig());
        this.auditDetailsRecord = dataAccess.getContext().newRecord(AUDIT_DETAILS);
        auditDetailsRecord.setSystemId(systemId);
        auditDetailsRecord.setCommitter(committer);
        setChangeType(I_ConceptAccess.fetchContributionChangeType(this, changeType));
        auditDetailsRecord.setDescription(description);
    }

    @Override
    public I_AuditDetailsAccess retrieveInstance(I_DomainAccess dataAccess, UUID auditId) {
        AuditDetailsAccess auditDetailsAccess = new AuditDetailsAccess(dataAccess);

        try {
            auditDetailsAccess.auditDetailsRecord = dataAccess.getContext().fetchOne(AUDIT_DETAILS, AUDIT_DETAILS.ID.eq(auditId));
        } catch (Exception e) {
            throw new InternalServerException("fetching audit_details failed", e);
        }

        if (auditDetailsAccess.auditDetailsRecord == null)  // FIXME can this even happen?
            return null;

        return auditDetailsAccess;
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    /**
     * @throws InternalServerException when DB problem on storing the auditDetails
     */
    @Override
    public UUID commit(Timestamp transactionTime) {
        auditDetailsRecord.setTimeCommitted(transactionTime);
        auditDetailsRecord.setTimeCommittedTzid(ZonedDateTime.now().getZone().getId()); // extracting only TZ, ignoring now() itself
        int result = auditDetailsRecord.insert();
        if (result == 1) {
            return auditDetailsRecord.getId();
        } else {
            throw new InternalServerException("Couldn't store auditDetails, DB problem");
        }
    }

    @Override
    public UUID commit() {
        return commit(Timestamp.valueOf(LocalDateTime.now()));
    }

    @Override
    public UUID commit(UUID systemId, UUID committerId, String description) {
        if (systemId == null || committerId == null)
            throw new IllegalArgumentException("arguments not optional");

        auditDetailsRecord.setSystemId(systemId);
        auditDetailsRecord.setCommitter(committerId);

        if (description != null) {
            auditDetailsRecord.setDescription(description);
        }
        auditDetailsRecord.setChangeType(ContributionChangeType.creation);

        return commit();
    }

    /**
     * @throws org.jooq.exception.DataAccessException when query executing went wrong
     * @throws org.jooq.exception.DataChangedException on DB inconsistency
     */
    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        boolean result = false;

        if (force || auditDetailsRecord.changed()) {
            auditDetailsRecord.setId(UUID.randomUUID()); // force to create new entry from old values
            result = auditDetailsRecord.insert() == 1;
        }

        return result;
    }

    @Override
    public Boolean update(Timestamp transactionTime) {
        return update(Timestamp.valueOf(LocalDateTime.now()), false);
    }

    @Override
    public Boolean update(Boolean force) {
        return update(Timestamp.valueOf(LocalDateTime.now()), force);
    }

    @Override
    public Boolean update() {
        return update(false);
    }

    @Override
    public Boolean update(UUID systemId, UUID committer, I_ConceptAccess.ContributionChangeType changeType, String description) {
        if (systemId != null)
            setSystemId(systemId);
        if (committer != null)
            setCommitter(committer);
        if (changeType != null)
            setChangeType(I_ConceptAccess.fetchContributionChangeType(this, changeType));
        if (description != null)
            setDescription(description);

        return update();
    }

    @Override
    public Integer delete() {
        return auditDetailsRecord.delete();
    }

    @Override
    public UUID getId() {
        return auditDetailsRecord.getId();
    }

    @Override
    public void setSystemId(UUID systemId) {
        auditDetailsRecord.setSystemId(systemId);
    }

    @Override
    public UUID getSystemId() {
        return auditDetailsRecord.getSystemId();
    }

    @Override
    public void setCommitter(UUID committer) {
        auditDetailsRecord.setCommitter(committer);
    }

    @Override
    public UUID getCommitter() {
        return auditDetailsRecord.getCommitter();
    }

    @Override
    public void setChangeType(UUID changeType) {
        String changeTypeString = I_ConceptAccess.fetchConceptLiteral(this, changeType);
        auditDetailsRecord.setChangeType(ContributionChangeType.valueOf(changeTypeString));
    }

    @Override
    public void setChangeType(I_ConceptAccess.ContributionChangeType changeType){
        auditDetailsRecord.setChangeType(ContributionChangeType.valueOf(changeType.name()));
    }

    @Override
    public ContributionChangeType getChangeType() {
        return auditDetailsRecord.getChangeType();
    }

    @Override
    public void setDescription(String description){
        auditDetailsRecord.setDescription(description);
    }

    @Override
    public String getDescription() {
        return auditDetailsRecord.getDescription();
    }

    @Override
    public Timestamp getTimeCommitted() {
        return auditDetailsRecord.getTimeCommitted();
    }

    @Override
    public String getTimeCommittedTzId() {
        return auditDetailsRecord.getTimeCommittedTzid();
    }

    @Override
    public void setRecord(AuditDetailsRecord record) {
        this.auditDetailsRecord = record;
    }

    @Override
    public AuditDetails getAsAuditDetails() {
        String systemId = getSystemId().toString();
        PartyProxy party = new PersistedPartyProxy(this).retrieve(getCommitter());
        DvDateTime time = new DvDateTime(getTimeCommitted().toLocalDateTime());
        DvCodedText changeType = new DvCodedText(getChangeType().getName(), new CodePhrase("openehr"));
        DvText description = new DvText(getDescription());
        return new AuditDetails(systemId, party, time, changeType, description);
    }
}
