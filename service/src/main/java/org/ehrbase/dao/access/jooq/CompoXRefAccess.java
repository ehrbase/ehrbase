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

import static org.ehrbase.jooq.pg.Tables.COMPO_XREF;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.dao.access.interfaces.I_CompoXrefAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.TransactionTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deals with composition links. For example to link an ACTION with an INSTRUCTION with  or an OBSERVATION
 * Created by christian on 9/12/2016.
 */
public class CompoXRefAccess extends DataAccess implements I_CompoXrefAccess {

    static Logger log = LoggerFactory.getLogger(CompoXRefAccess.class);

    public CompoXRefAccess(I_DomainAccess domainAccess) {
        super(domainAccess);
    }

    @Override
    public Map<UUID, Timestamp> getLinkList(UUID masterUid) {
        return getContext()
                .select(COMPO_XREF.CHILD_UUID, COMPO_XREF.SYS_TRANSACTION)
                .from(COMPO_XREF)
                .where(COMPO_XREF.MASTER_UUID.eq(masterUid))
                .fetch()
                .intoMap(COMPO_XREF.CHILD_UUID, COMPO_XREF.SYS_TRANSACTION);
    }

    @Override
    public UUID getLastLink(UUID masterUid) {
        return getContext()
                .select(COMPO_XREF.CHILD_UUID)
                .from(COMPO_XREF)
                .where(COMPO_XREF.MASTER_UUID.eq(masterUid))
                .orderBy(COMPO_XREF.SYS_TRANSACTION.desc())
                .fetchOne(COMPO_XREF.CHILD_UUID);
    }

    @Override
    public int setLink(UUID masterUid, UUID childUid) {
        Timestamp timestamp = TransactionTime.millis();
        return getContext()
                .insertInto(COMPO_XREF)
                .columns(COMPO_XREF.MASTER_UUID, COMPO_XREF.CHILD_UUID, COMPO_XREF.SYS_TRANSACTION)
                .values(masterUid, childUid, timestamp)
                .execute();
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }
}
