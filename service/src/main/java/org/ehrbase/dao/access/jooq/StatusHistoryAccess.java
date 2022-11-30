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

import static org.ehrbase.jooq.pg.Tables.STATUS_HISTORY;

import java.sql.Timestamp;
import java.util.UUID;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_StatusHistoryAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.tables.records.StatusHistoryRecord;
import org.jooq.Condition;
import org.jooq.Result;

public class StatusHistoryAccess extends DataAccess implements I_StatusHistoryAccess {
    private StatusHistoryRecord statusHistoryRecord;

    private StatusHistoryAccess(I_DomainAccess domainAccess, StatusHistoryRecord statusHistoryRecord) {
        super(domainAccess);
        this.statusHistoryRecord = statusHistoryRecord;
    }

    public static StatusHistoryAccess retrieveByVersion(I_DomainAccess domainAccess, UUID statusId, int version) {
        Result<StatusHistoryRecord> historyRec = domainAccess
                .getContext()
                .selectFrom(STATUS_HISTORY)
                .where(STATUS_HISTORY.ID.eq(statusId))
                .orderBy(STATUS_HISTORY.SYS_TRANSACTION.asc())
                .fetch();

        if (historyRec.isEmpty()) return null;

        StatusHistoryRecord rec = historyRec.get(version - 1);
        return new StatusHistoryAccess(domainAccess, rec);
    }

    @Override
    public UUID getId() {
        return statusHistoryRecord.getId();
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    @Override
    public UUID getContributionId() {
        return statusHistoryRecord.getInContribution();
    }

    @Override
    public Timestamp getSysTransaction() {
        return statusHistoryRecord.getSysTransaction();
    }

    @Override
    public UUID commit(Timestamp transactionTime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UUID commit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean update(Timestamp transactionTime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean update() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean update(Boolean force) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer delete() {
        Condition condition = STATUS_HISTORY
                .ID
                .eq(statusHistoryRecord.getId())
                .and(STATUS_HISTORY.SYS_TRANSACTION.eq(statusHistoryRecord.getSysTransaction()));
        return getContext().delete(STATUS_HISTORY).where(condition).execute();
    }
}
