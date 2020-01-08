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
import org.ehrbase.dao.access.interfaces.I_AuditDetailsAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_StatusAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;
import static org.ehrbase.jooq.pg.Tables.STATUS;

/**
 * Created by Christian Chevalley on 4/20/2015.
 */
public class StatusAccess extends DataAccess implements I_StatusAccess {

    private static final Logger log = LogManager.getLogger(StatusAccess.class);

    private StatusRecord statusRecord;
    private I_AuditDetailsAccess auditDetailsAccess;  // audit associated with this status

    public StatusAccess(I_DomainAccess domainAccess) {
        super(domainAccess);

        statusRecord = getContext().newRecord(STATUS);

        // associate status' own audit with this status access instance
        auditDetailsAccess = I_AuditDetailsAccess.getInstance(getDataAccess());
    }

    public static I_StatusAccess retrieveInstance(I_DomainAccess domainAccess, UUID statusId) {
        StatusRecord record = domainAccess.getContext().fetchOne(STATUS, STATUS.ID.eq(statusId));

        // FIXME VERSIONED_OBJECT_POC: externalize to private method
        if (record == null) {
            return null;
        }

        StatusAccess statusAccess = new StatusAccess(domainAccess);
        statusAccess.setStatusRecord(record);

        // retrieve corresponding audit
        I_AuditDetailsAccess auditAccess = new AuditDetailsAccess(domainAccess.getDataAccess()).retrieveInstance(domainAccess.getDataAccess(), statusAccess.getAuditDetailsId());
        statusAccess.setAuditDetailsAccess(auditAccess);

        return statusAccess;
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

        // FIXME VERSIONED_OBJECT_POC: delete when done
        /*Record record = context.select(STATUS.ID).from(STATUS)
                .where(STATUS.PARTY.eq
                        (context.select(PARTY_IDENTIFIED.ID)
                                .from(PARTY_IDENTIFIED)
                                .where(PARTY_IDENTIFIED.NAME.eq(partyName))
                        )
                )
                .fetchOne();*/

        if (record == null) {
            log.warn("Could not retrieveInstanceByNamedSubject status for party:" + partyName);
            return null;
        }

        StatusAccess statusAccess = new StatusAccess(domainAccess);
        statusAccess.setStatusRecord(record);

        // retrieve corresponding audit
        I_AuditDetailsAccess auditAccess = new AuditDetailsAccess(domainAccess.getDataAccess()).retrieveInstance(domainAccess.getDataAccess(), statusAccess.getAuditDetailsId());
        statusAccess.setAuditDetailsAccess(auditAccess);

        return statusAccess;
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

        // FIXME VERSIONED_OBJECT_POC: delete when done
        /*Record record = context.select(STATUS.ID).from(STATUS)
                .where(STATUS.PARTY.eq
                        (context.select(PARTY_IDENTIFIED.ID)
                                .from(PARTY_IDENTIFIED)
                                .where(PARTY_IDENTIFIED.ID.eq(partyIdentified))
                        )
                )
                .fetchOne();*/

        if (record == null) {
            log.warn("Could not retrieveInstanceByNamedSubject Instance status for party:" + partyIdentified);
            return null;
        }
        StatusAccess statusAccess = new StatusAccess(domainAccess);
        statusAccess.setStatusRecord(record);

        // retrieve corresponding audit
        I_AuditDetailsAccess auditAccess = new AuditDetailsAccess(domainAccess.getDataAccess()).retrieveInstance(domainAccess.getDataAccess(), statusAccess.getAuditDetailsId());
        statusAccess.setAuditDetailsAccess(auditAccess);

        return statusAccess;

    }

    public static I_StatusAccess retrieveInstanceByEhrId(I_DomainAccess domainAccess, UUID ehrId) {
        StatusRecord record = domainAccess.getContext().fetchOne(STATUS, STATUS.EHR_ID.eq(ehrId));

        if (record == null) {
            return null;
        }

        StatusAccess statusAccess = new StatusAccess(domainAccess);
        statusAccess.setStatusRecord(record);

        // retrieve corresponding audit
        I_AuditDetailsAccess auditAccess = new AuditDetailsAccess(domainAccess.getDataAccess()).retrieveInstance(domainAccess.getDataAccess(), statusAccess.getAuditDetailsId());
        statusAccess.setAuditDetailsAccess(auditAccess);

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
        getAuditDetailsAccess().setChangeType(I_ConceptAccess.fetchContributionChangeType(this, I_ConceptAccess.ContributionChangeType.CREATION));
        UUID auditId = getAuditDetailsAccess().commit();
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
        // FIXME VERSIONED_OBJECT_POC: delete when done!?
        /*if (statusRecord.changed()) {
            statusRecord.changed(STATUS.SYS_PERIOD, false);
            statusRecord.setSysTransaction(transactionTime);
            return statusRecord.update() > 0;
        }

        return false;*/

        return update(null, transactionTime, false);
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        // FIXME VERSIONED_OBJECT_POC: delete when done!?
        /*return update(transactionTime);

        if (force || statusRecord.changed()) {
            *//*statusRecord.changed(STATUS.SYS_PERIOD, false);
            statusRecord.setSysTransaction(transactionTime);
            return statusRecord.update() > 0;*//*
        }

        return false;*/

        return update(null, transactionTime, force);
    }

    @Override
    public Boolean update(Boolean force) {
        return update(null, Timestamp.valueOf(LocalDateTime.now()), force);
    }

    @Override   // root update()
    public Boolean update(ItemStructure otherDetails, Timestamp transactionTime, boolean force) {
        if (force || statusRecord.changed()) {
            // FIXME VERSIONED_OBJECT_POC: that's not right.. this creates a new audit in DB, where the three null attributes are really empty, not what they have been before the update
            auditDetailsAccess.update(null, null, I_ConceptAccess.ContributionChangeType.MODIFICATION, null);

            /*if (ehrId != null) {
                statusRecord.setEhrId(ehrId);
            }*/
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
    public StatusRecord getStatusRecord() {
        return this.statusRecord;
    }

    @Override
    public void setAuditDetailsAccess(I_AuditDetailsAccess auditDetailsAccess) {
        this.auditDetailsAccess = auditDetailsAccess;
    }

    @Override
    public I_AuditDetailsAccess getAuditDetailsAccess() {
        return this.auditDetailsAccess;
    }

    @Override
    public UUID getAuditDetailsId() {
        return statusRecord.getHasAudit();
    }
}
