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

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_StatusAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.sql.Timestamp;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;
import static org.ehrbase.jooq.pg.Tables.STATUS;

/**
 * Created by Christian Chevalley on 4/20/2015.
 */
public class StatusAccess extends DataAccess implements I_StatusAccess {

    private static final Logger log = LogManager.getLogger(StatusAccess.class);

    private StatusRecord statusRecord;

    public StatusAccess(I_DomainAccess domainAccess) {
        super(domainAccess);

    }

    public static I_StatusAccess retrieveInstanceByNamedSubject(I_DomainAccess domainAccess, String partyName) {

        DSLContext context = domainAccess.getContext();

        Record record = context.select(STATUS.ID).from(STATUS)
                .where(STATUS.PARTY.eq
                        (context.select(PARTY_IDENTIFIED.ID)
                                .from(PARTY_IDENTIFIED)
                                .where(PARTY_IDENTIFIED.NAME.eq(partyName))
                        )
                )
                .fetchOne();

        if (record.size() == 0) {
            log.warn("Could not retrieveInstanceByNamedSubject status for party:" + partyName);
            return null;
        }

        StatusAccess statusAccess = new StatusAccess(domainAccess);
        statusAccess.statusRecord = (StatusRecord) record;

        return statusAccess;
    }

    public static I_StatusAccess retrieveInstance(I_DomainAccess domainAccess, UUID partyIdentified) {

        DSLContext context = domainAccess.getContext();

        Record record = context.select(STATUS.ID).from(STATUS)
                .where(STATUS.PARTY.eq
                        (context.select(PARTY_IDENTIFIED.ID)
                                .from(PARTY_IDENTIFIED)
                                .where(PARTY_IDENTIFIED.ID.eq(partyIdentified))
                        )
                )
                .fetchOne();

        if (record.size() == 0) {
            log.warn("Could not retrieveInstanceByNamedSubject Instance status for party:" + partyIdentified);
            return null;
        }
        StatusAccess statusAccess = new StatusAccess(domainAccess);
        statusAccess.statusRecord = (StatusRecord) record;

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
    public Boolean update(Timestamp transactionTime) {
        if (statusRecord.changed()) {
            statusRecord.changed(STATUS.SYS_PERIOD, false);
            statusRecord.setSysTransaction(transactionTime);
            return statusRecord.update() > 0;
        }

        return false;
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        return update(transactionTime);
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

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public Boolean update(Boolean force) {
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
}
