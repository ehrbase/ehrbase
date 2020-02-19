/*
 * Modifications copyright (C) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).

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

import org.ehrbase.dao.access.interfaces.*;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.tables.records.CompositionHistoryRecord;
import org.jooq.Result;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.COMPOSITION_HISTORY;

/**
 * Stripped down DAO to perform some `*_history` table related actions. Composition access handling in general is
 * done through the {@link CompositionAccess} class.
 */
public class CompositionHistoryAccess extends DataAccess implements I_CompositionHistoryAccess {

    private CompositionHistoryRecord record;

    public CompositionHistoryAccess(I_DomainAccess domainAccess) {
        super(domainAccess);
        this.record = domainAccess.getContext().newRecord(COMPOSITION_HISTORY);
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    @Override
    public UUID commit(Timestamp transactionTime) {
        record.setSysTransaction(transactionTime);
        return commit();
    }

    @Override
    public UUID commit() {
        if (this.record.getSysTransaction() == null) {
            this.record.setSysTransaction(Timestamp.valueOf(LocalDateTime.now()));
            this.record.setSysPeriod(new AbstractMap.SimpleEntry<>(OffsetDateTime.now(), null));
        }
        if (record.insert() == 1)
            return record.getId();
        else
            return null;
    }

    @Override
    public Boolean update(Timestamp transactionTime) {
        return null;    // TODO
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        return null;    // TODO
    }

    @Override
    public Boolean update() {

        // manually constructing an update query, because _history tables aren't of type UpdatableRecord, because table has no PK
        // two conditions: same ID and timestamp
        int num = this.getContext().update(COMPOSITION_HISTORY)
                .set(this.record)
                .where(COMPOSITION_HISTORY.ID.eq(this.record.getId())
                        .and(COMPOSITION_HISTORY.SYS_TRANSACTION.eq(this.record.getSysTransaction())))
                .execute();

        return num >= 1;
    }

    @Override
    public Boolean update(Boolean force) {
        return null;    // TODO
    }

    @Override
    public Integer delete() {
        return null;    // TODO
    }

    public static I_CompositionHistoryAccess retrieveLatest(I_DomainAccess domainAccess, UUID compositionId) {
        // retrieve all history records for given ID, ordered by time with latest at top
        Result<CompositionHistoryRecord> historyRecordsRes = domainAccess.getContext()
                .selectFrom(COMPOSITION_HISTORY)
                .where(COMPOSITION_HISTORY.ID.eq(compositionId))
                .orderBy(COMPOSITION_HISTORY.SYS_TRANSACTION.desc())    // latest at top, i.e. [0]
                .fetch();
        if (historyRecordsRes.isEmpty())
            return null;
        CompositionHistoryRecord rec = historyRecordsRes.get(0);

        I_CompositionHistoryAccess historyAccess = new CompositionHistoryAccess(domainAccess);
        historyAccess.setRecord(rec);
        return historyAccess;
    }

    @Override
    public void setRecord(CompositionHistoryRecord record) {
        this.record = record;
    }

    @Override
    public CompositionHistoryRecord getRecord() {
        return this.record;
    }

    @Override
    public void setInContribution(UUID contribution) {
        this.record.setInContribution(contribution);
    }

    @Override
    public void setHasAudit(UUID audit) {
        this.record.setHasAudit(audit);
    }
}
